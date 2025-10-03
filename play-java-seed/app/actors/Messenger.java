package actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import services.YouTubeService;

/**
 * SuperActor / Messenger
 * The Messenger actor is responsible for handling search queries related to YouTube videos.
 * It communicates with the YouTubeService to fetch video details based on search queries.
 * The results are cached and periodically updated, and the actor broadcasts the results to connected clients.
 * @author Janmitsinh Panjrolia
 */
public class Messenger extends AbstractActor {

    private static final Logger log = LoggerFactory.getLogger(Messenger.class);

    private static final long FETCH_INTERVAL = 5; // Fetch interval in seconds
    private static final Map<String, List<ObjectNode>> queryResults = Collections.synchronizedMap(new HashMap<>());
    private static final Set<ActorRef> clients = Collections.synchronizedSet(new HashSet<>());
    private static ScheduledExecutorService scheduler; // Scheduler for polling

    private final ActorRef out;
    private final YouTubeService youTubeService;

    // Map to store the last timestamp for each query
    private static final Map<String, Long> queryTimestamps = Collections.synchronizedMap(new HashMap<>());

    /**
     * Factory method to create a Props instance for the Messenger actor.
     *
     * @param out           The ActorRef representing the output client.
     * @param youTubeService The YouTube service used to fetch video details.
     * @return A Props instance for creating a Messenger actor.
     */
    public static Props props(ActorRef out, YouTubeService youTubeService) {
        return Props.create(Messenger.class, () -> new Messenger(out, youTubeService));
    }

    /**
     * Constructor for the Messenger actor.
     *
     * @param out           The ActorRef representing the output client.
     * @param youTubeService The YouTube service used to fetch video details.
     */
    public Messenger(ActorRef out, YouTubeService youTubeService) {
        this.out = out;
        this.youTubeService = youTubeService;
    }

    /**
     * Called when the actor is started. Adds the client to the active client list.
     * If no clients are active, the global polling for YouTube search queries is started.
     */
    @Override
    public void preStart() {
        clients.add(out); // Add client to the active list
        if (scheduler == null) {
            startGlobalPolling();
        }
    }

    /**
     * Called when the actor is stopped. Removes the client from the active client list.
     * If no clients are active, stops the global polling.
     */
    @Override
    public void postStop() {
        clients.remove(out); // Remove client when actor stops
        if (clients.isEmpty() && scheduler != null) {
            scheduler.shutdownNow(); // Stop scheduler if no clients are active
            scheduler = null;
        }
    }

    /**
     * Defines the behavior of the actor when it receives messages.
     * It listens for incoming JSON search queries and processes them.
     *
     * @return The behavior of the actor when receiving messages.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, this::handleSearchQuery)
                .build();
    }

    /**
     * Handles the incoming search query.
     * If the query has been recently fetched (within the last 10 minutes), cached results are sent.
     * Otherwise, the results are fetched, cached, and sent to the client.
     *
     * @param jsonNode The JSON object containing the search query.
     */
    private void handleSearchQuery(JsonNode jsonNode) {
        if (jsonNode.has("query")) {
            String query = jsonNode.get("query").asText();
            log.info("New search query received: {}", query);

            // Check if the query has been recently fetched and avoid redundant fetching
            long currentTime = System.currentTimeMillis();
            if (queryTimestamps.containsKey(query) &&
                    (currentTime - queryTimestamps.get(query)) < TimeUnit.MINUTES.toMillis(10)) {
                // If the query was fetched in the last 10 minutes, use cached data
                log.info("Returning cached results for query: {}", query);
                sendCachedResults(query);
            } else {
                // Otherwise, fetch and cache the results
                queryTimestamps.put(query, currentTime);
                queryResults.putIfAbsent(query, new ArrayList<>());
                fetchQueryResults(query)
                        .thenAccept(results -> updateQueryResults(query, results))
                        .exceptionally(ex -> {
                            log.error("Error fetching results for query '{}': {}", query, ex.getMessage());
                            return null;
                        });
            }
        }
    }

    /**
     * Sends the cached results for a given query to the clients.
     * If no cached results are available, a message indicating that is sent.
     *
     * @param query The search query for which cached results are being sent.
     */
    private void sendCachedResults(String query) {
        List<ObjectNode> cachedResults = queryResults.get(query);
        if (cachedResults != null) {
            // Send the cached results immediately, including the first 10 results
            broadcastResults(query, cachedResults);
        } else {
            // If no cached results, notify that no results are available yet
            ObjectNode noResultsMessage = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
            noResultsMessage.put("query", query);
            noResultsMessage.put("results", "No cached results available yet.");
            clients.forEach(client -> client.tell(noResultsMessage, self()));
        }
    }

    /**
     * Starts the global polling to fetch and update results for all queries every specified interval.
     */
    private void startGlobalPolling() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::fetchAndUpdateAllQueries, 0, FETCH_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Fetches and updates the results for all stored queries at regular intervals.
     */
    private void fetchAndUpdateAllQueries() {
        queryResults.keySet().forEach(query -> fetchQueryResults(query)
                .thenAccept(results -> updateQueryResults(query, results))
                .exceptionally(ex -> {
                    log.error("Error fetching results for query '{}': {}", query, ex.getMessage());
                    return null;
                }));
    }

    /**
     * Fetches the video details for a given query using the YouTube service.
     *
     * @param query The search query to fetch video details for.
     * @return A CompletableFuture containing the fetched video details.
     */
    private CompletableFuture<List<ObjectNode>> fetchQueryResults(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Fetching results for query: {}", query);
                return youTubeService.fetchVideoDetails(query).stream()
                        .map(result -> {
                            ObjectNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                            jsonNode.put("id", result.getId().getVideoId());
                            jsonNode.put("title", result.getSnippet().getTitle());
                            jsonNode.put("description", result.getSnippet().getDescription());
                            jsonNode.put("channelTitle", result.getSnippet().getChannelTitle());
                            jsonNode.put("channelId", result.getSnippet().getChannelId());
                            jsonNode.put("thumbnail", result.getSnippet().getThumbnails().getDefault().getUrl());
                            jsonNode.put("publishedAt", result.getSnippet().getPublishedAt().toString());
                            return jsonNode;
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException("Error fetching video details", e);
            }
        });
    }

    /**
     * Updates the cached results for a given query with new videos.
     * The results are sorted by the publication date and only the top 10 videos are kept.
     *
     * @param query    The search query for which the results are being updated.
     * @param newVideos The new video details to be added to the cached results.
     */
    private void updateQueryResults(String query, List<ObjectNode> newVideos) {
        List<ObjectNode> currentVideos = queryResults.get(query);

        synchronized (currentVideos) {
            newVideos.forEach(video -> {
                if (currentVideos.stream().noneMatch(v -> v.get("id").asText().equals(video.get("id").asText()))) {
                    currentVideos.add(video);
                }
            });

            currentVideos.sort(Comparator.comparing(v -> v.get("publishedAt").asText(), Comparator.reverseOrder()));
            if (currentVideos.size() > 10) {
                currentVideos.subList(10, currentVideos.size()).clear();
            }
        }
        broadcastResults(query, currentVideos);
    }

    /**
     * Broadcasts the current video results for a given query to all connected clients.
     *
     * @param query        The search query for which the results are being broadcasted.
     * @param currentVideos The current list of video results to be broadcasted.
     */
    private void broadcastResults(String query, List<ObjectNode> currentVideos) {
        ObjectNode response = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        response.put("query", query);
        response.set("results", new com.fasterxml.jackson.databind.ObjectMapper().createArrayNode().addAll(currentVideos));

        synchronized (clients) {
            clients.forEach(client -> client.tell(response, self()));
        }
    }
}

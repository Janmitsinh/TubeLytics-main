package actors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.naming.directory.SearchResult;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;

import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * The Word actor processes word statistics from YouTube video titles and descriptions.
 * It fetches data using the YouTube API and calculates word frequencies.
 * @author Janmitsinh Panjrolia
 */
public class Word extends AbstractActor {

    private final YouTube youtube;
    private final String apiKey;

    /**
     * Constructor for the Word actor.
     *
     * @param youtube The YouTube API client.
     * @param apiKey  The YouTube API key.
     */
    public Word(YouTube youtube, String apiKey) {
        this.youtube = youtube;
        this.apiKey = apiKey;
    }

    /**
     * Factory method to create Props for the Word actor.
     *
     * @param youtube The YouTube API client.
     * @param apiKey  The YouTube API key.
     * @return Props for creating the Word actor.
     */
    public static Props props(YouTube youtube, String apiKey) {
        return Props.create(Word.class, () -> new Word(youtube, apiKey));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Key.class, key -> {
                    try {
                        List<Map.Entry<String, Long>> stats = getWordStats(key.query);
                        sender().tell(stats, self());
                    } catch (IOException e) {
                        sender().tell(new akka.actor.Status.Failure(e), self());
                    }
                })
                .build();
    }

    /**
     * Processes word statistics for a given query.
     *
     * @param query The search query.
     * @return A list of word frequencies.
     * @throws IOException If an error occurs while accessing the YouTube API.
     */
    private List<Map.Entry<String, Long>> getWordStats(String query) throws IOException {
        YouTube.Search.List search = youtube.search().list("id,snippet");
        search.setQ(query);
        search.setType("video");
        search.setMaxResults(50L);
        search.setKey(apiKey);

        SearchListResponse searchResponse = search.execute();
        List<SearchResult> searchResults = searchResponse.getItems();

        List<String> videoIds = searchResults.stream()
                .map(result -> result.getId().getVideoId())
                .collect(Collectors.toList());

        YouTube.Videos.List videosList = youtube.videos().list("snippet,contentDetails");
        videosList.setId(String.join(",", videoIds));
        videosList.setKey(apiKey);

        List<Video> videoDetails = videosList.execute().getItems();

        Map<String, Long> wordFrequency = videoDetails.stream()
                .flatMap(video -> {
                    String title = video.getSnippet().getTitle();
                    String description = video.getSnippet().getDescription();
                    String combinedText = title + " " + (description != null ? description : "");
                    return Arrays.stream(combinedText.toLowerCase().split("\\W+"));
                })
                .filter(word -> !word.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toList());
    }

    /**
     * Message class to encapsulate the search query.
     */
    public static class Key {
        public final String query;

        public Key(String query) {
            this.query = query;
        }
    }
}

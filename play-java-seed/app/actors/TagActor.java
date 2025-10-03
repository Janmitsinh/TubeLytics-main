package actors;

import java.io.IOException;
import java.util.List;

import javax.naming.directory.SearchResult;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;

import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * The TagActor class is responsible for interacting with the YouTube API to fetch video details and video tags
 * based on a provided tag or video ID. It handles messages related to retrieving videos by tag and fetching
 * details and tags of specific videos. It communicates with the sender actor by returning search results, video
 * details, or tags.
 * @author Janmitsinh Panjrolia
 */
public class TagActor extends AbstractActor {

    private final YouTube youtube;
    private final String apiKey;

    /**
     * Constructor for the TagActor.
     *
     * @param youtube The YouTube API client used for making requests to the YouTube service.
     * @param apiKey  The API key used to authenticate requests to the YouTube API.
     */
    public TagActor(YouTube youtube, String apiKey) {
        this.youtube = youtube;
        this.apiKey = apiKey;
    }

    /**
     * Factory method to create a Props instance for the TagActor.
     *
     * @param youtube The YouTube API client.
     * @param apiKey  The API key used to authenticate requests to the YouTube API.
     * @return A Props instance to create a new TagActor.
     */
    public static Props props(YouTube youtube, String apiKey) {
        return Props.create(TagActor.class, () -> new TagActor(youtube, apiKey));
    }

    // Define message classes

    /**
     * Message to request a list of videos matching a given tag.
     */
    public static class GetVideosByTag {
        public final String tag;

        /**
         * Constructor for GetVideosByTag message.
         *
         * @param tag The tag to search for on YouTube.
         */
        public GetVideosByTag(String tag) {
            this.tag = tag;
        }
    }

    /**
     * Message to request detailed information for a specific video identified by its video ID.
     */
    public static class GetVideoDetails {
        public final String videoId;

        /**
         * Constructor for GetVideoDetails message.
         *
         * @param videoId The ID of the video to retrieve details for.
         */
        public GetVideoDetails(String videoId) {
            this.videoId = videoId;
        }
    }

    /**
     * Message to request the tags associated with a specific video.
     */
    public static class GetVideoTags {
        public final String videoId;

        /**
         * Constructor for GetVideoTags message.
         *
         * @param videoId The ID of the video to retrieve tags for.
         */
        public GetVideoTags(String videoId) {
            this.videoId = videoId;
        }
    }

    /**
     * Defines the behavior of the actor when it receives messages.
     * It listens for messages requesting video details, videos by tag, and video tags.
     *
     * @return The behavior of the actor when receiving messages.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetVideosByTag.class, this::handleGetVideosByTag)
                .match(GetVideoDetails.class, message -> {
                    try {
                        sender().tell(fetchVideoDetails(message.videoId), self());
                    } catch (IOException e) {
                        sender().tell(new akka.actor.Status.Failure(e), self());
                    }
                })
                .match(GetVideoTags.class, this::handleGetVideoTags)
                .build();
    }

    /**
     * Handles the GetVideosByTag message. It makes a request to the YouTube API to search for videos by the given tag.
     * The results are returned to the sender actor.
     *
     * @param message The message containing the tag to search for.
     */
    private void handleGetVideosByTag(GetVideosByTag message) {
        try {
            YouTube.Search.List search = youtube.search().list("id,snippet");
            search.setKey(apiKey);
            search.setQ(message.tag);
            search.setType("video");
            search.setMaxResults(10L);

            List<SearchResult> results = search.execute().getItems();
            sender().tell(results, self());
        } catch (IOException e) {
            sender().tell(new akka.actor.Status.Failure(e), self());
        }
    }

    /**
     * Fetches the details for a specific video by its ID from the YouTube API.
     *
     * @param videoId The ID of the video to fetch details for.
     * @return The detailed information about the video.
     * @throws IOException If an error occurs while fetching video details.
     */
    private Video fetchVideoDetails(String videoId) throws IOException {
        YouTube.Videos.List videoRequest = youtube.videos().list("snippet");
        videoRequest.setKey(apiKey);
        videoRequest.setId(videoId);

        List<Video> videos = videoRequest.execute().getItems();
        if (!videos.isEmpty()) {
            return videos.get(0);
        } else {
            throw new IOException("No video found");
        }
    }

    /**
     * Handles the GetVideoTags message. It retrieves the tags associated with a video identified by its video ID.
     * The tags are returned to the sender actor.
     *
     * @param message The message containing the video ID for which to retrieve tags.
     */
    private void handleGetVideoTags(GetVideoTags message) {
        try {
            Video videoDetails = fetchVideoDetails(message.videoId);
            if (videoDetails != null && videoDetails.getSnippet().getTags() != null) {
                sender().tell(videoDetails.getSnippet().getTags(), self());
            } else {
                sender().tell(List.of(), self());
            }
        } catch (Exception e) {
            sender().tell(new akka.actor.Status.Failure(e), self());
        }
    }
}

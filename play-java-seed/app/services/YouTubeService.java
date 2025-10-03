package services;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.naming.directory.SearchResult;

import com.google.api.services.youtube.YouTube;

/**
 * Service class for interacting with the YouTube Data API to fetch video details based on a search query.
 * Provides methods to search for videos by query and return a list of search results.
 * @author Janmitsinh Panjrolia
 */
public class YouTubeService {

    private final YouTube youTube;

    /**
     * Constructs a YouTubeService instance with the provided YouTube client.
     *
     * @param youTube The YouTube client used to interact with the YouTube API.
     */
    @Inject
    public YouTubeService(YouTube youTube) {
        this.youTube = youTube;
    }

    /**
     * Fetches the details of videos that match the given search query.
     * This method retrieves the latest 10 video results from YouTube based on the query.
     * @param query The search query to search for videos.
     * @return A list of {@link SearchResult} objects representing the matching video details.
     *         Returns an empty list if an error occurs during the search process.
     * @throws java.io.IOException If an error occurs during the API call.
     */
    public List<SearchResult> fetchVideoDetails(String query) {
        try {
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setQ(query);
            search.setType("video");
            search.setMaxResults(10L);
            search.setOrder("date");
            search.setKey("AIzaSyBE1vlaVKOrkty9xU-fglNBHteMfX9kavw");
            return search.execute().getItems();
        } catch (IOException e) {
            e.printStackTrace();
            return List.of(); // Return an empty list if an exception occurs
        }
    }
}

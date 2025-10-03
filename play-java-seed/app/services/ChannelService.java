package services;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for interacting with the YouTube Data API to fetch channel information and videos.
 * Provides methods to get channel details and a list of videos from a specific channel.
 * @author Janmitsinh Panjrolia
 */
public class ChannelService {
    private final YouTube youtube;
    private final String apiKey;

    /**
     * Constructs a ChannelService instance with the given YouTube client and API key.
     *
     * @param youtube The YouTube client used to interact with the YouTube API.
     * @param apiKey  The API key used to authenticate requests to the YouTube API.
     */
    public ChannelService(YouTube youtube, String apiKey) {
        this.youtube = youtube;
        this.apiKey = apiKey;
    }

    /**
     * Retrieves the channel information for the specified channel ID, including snippet and statistics data.
     *
     * @param channelId The ID of the YouTube channel to retrieve information for.
     * @return A {@link Channel} object containing the channel's details.
     * @throws IOException If there is an error while communicating with the YouTube API or if the channel is not found.
     */
    public Channel getChannelInfo(String channelId) throws IOException {
        return youtube.channels()
                .list("snippet,statistics")
                .setId(channelId)
                .setKey(apiKey)
                .execute()
                .getItems()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IOException("Channel not found"));
    }

    /**
     * Retrieves a list of the latest videos from the specified channel.
     *
     * @param channelId The ID of the YouTube channel to fetch videos from.
     * @return A list of {@link SearchResult} objects representing the latest videos from the specified channel.
     * @throws IOException If there is an error while communicating with the YouTube API.
     */
    public List<SearchResult> getChannelVideos(String channelId) throws IOException {
        return youtube.search()
                .list("id,snippet")
                .setChannelId(channelId)
                .setType("video")
                .setMaxResults(10L)
                .setOrder("date")
                .setKey(apiKey)
                .execute()
                .getItems()
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }
}

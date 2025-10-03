import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import services.ChannelService;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChannelServiceTest {

    private YouTube youtubeMock;
    private ChannelService channelService;
    private String apiKey = "mock-api-key";

    @Before
    public void setUp() {
        youtubeMock = Mockito.mock(YouTube.class);
        channelService = new ChannelService(youtubeMock, apiKey);
    }

    @Test
    public void testGetChannelInfo() throws IOException {
        // Mock the YouTube API response
        YouTube.Channels channelsMock = mock(YouTube.Channels.class);
        YouTube.Channels.List channelsListMock = mock(YouTube.Channels.List.class);
        ChannelListResponse channelListResponseMock = mock(ChannelListResponse.class);
        Channel channelMock = mock(Channel.class);

        when(youtubeMock.channels()).thenReturn(channelsMock);
        when(channelsMock.list("snippet,statistics")).thenReturn(channelsListMock);
        when(channelsListMock.setId("test-channel-id")).thenReturn(channelsListMock);
        when(channelsListMock.setKey(apiKey)).thenReturn(channelsListMock);
        when(channelsListMock.execute()).thenReturn(channelListResponseMock);
        when(channelListResponseMock.getItems()).thenReturn(Arrays.asList(channelMock));

        // Test the method
        Channel result = channelService.getChannelInfo("test-channel-id");

        // Assert that the result is not null and is the mock channel
        assertNotNull(result);
        assertEquals(channelMock, result);

        // Verify interactions with YouTube API
        verify(channelsListMock).setId("test-channel-id");
        verify(channelsListMock).setKey(apiKey);
    }

    @Test
    public void testGetChannelVideos() throws IOException {
        // Mock the YouTube API response for video search
        YouTube.Search searchMock = mock(YouTube.Search.class);
        YouTube.Search.List searchListMock = mock(YouTube.Search.List.class);
        SearchListResponse searchListResponseMock = mock(SearchListResponse.class);
        SearchResult video1Mock = mock(SearchResult.class);
        SearchResult video2Mock = mock(SearchResult.class);

        when(youtubeMock.search()).thenReturn(searchMock);
        when(searchMock.list("id,snippet")).thenReturn(searchListMock);
        when(searchListMock.setChannelId("test-channel-id")).thenReturn(searchListMock);
        when(searchListMock.setType("video")).thenReturn(searchListMock);
        when(searchListMock.setMaxResults(10L)).thenReturn(searchListMock);
        when(searchListMock.setOrder("date")).thenReturn(searchListMock);
        when(searchListMock.setKey(apiKey)).thenReturn(searchListMock);
        when(searchListMock.execute()).thenReturn(searchListResponseMock);
        when(searchListResponseMock.getItems()).thenReturn(Arrays.asList(video1Mock, video2Mock));

        // Test the method
        List<SearchResult> result = channelService.getChannelVideos("test-channel-id");

        // Assert the result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(video1Mock, result.get(0));
        assertEquals(video2Mock, result.get(1));

        // Verify interactions with YouTube API
        verify(searchListMock).setChannelId("test-channel-id");
        verify(searchListMock).setKey(apiKey);
    }
}

import actors.ChannelActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.api.services.youtube.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.ChannelService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import java.util.stream.Collectors;

public class ChannelActorTest {

    private ActorSystem system;

    @Before
    public void setup() {
        system = ActorSystem.create();
    }

    @After
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    private Channel createMockChannel(String title, long subscriberCount) {
        ChannelSnippet channelSnippet = new ChannelSnippet();
        channelSnippet.setTitle(title);
        ChannelStatistics channelStatistics = new ChannelStatistics();
        channelStatistics.setSubscriberCount(BigInteger.valueOf(subscriberCount));

        Channel channel = new Channel();
        channel.setSnippet(channelSnippet);
        channel.setStatistics(channelStatistics);
        return channel;
    }

    private List<SearchResult> createMockSearchResults(String... videoTitles) {
        return Arrays.stream(videoTitles)
                .map(title -> {
                    SearchResult video = new SearchResult();
                    video.setSnippet(new SearchResultSnippet().setTitle(title));
                    return video;
                })
                .collect(Collectors.toList());
    }

    @Test
    public void testChannelActor() throws Exception {
        new TestKit(system) {{
            // Mock ChannelService
            ChannelService channelService = mock(ChannelService.class);

            // Create mock data
            Channel mockChannel = createMockChannel("Test Channel", 1000);
            List<SearchResult> searchResults = createMockSearchResults("Video One", "Video Two");

            // Set up mock behavior for ChannelService
            when(channelService.getChannelInfo(anyString())).thenReturn(mockChannel);
            when(channelService.getChannelVideos(anyString())).thenReturn(searchResults);

            // Test the ChannelActor
            Props props = ChannelActor.props(channelService);
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new ChannelActor.ChannelRequest("test-channel-id"), probe.getRef());

            ChannelActor.ChannelData response = probe.expectMsgClass(ChannelActor.ChannelData.class);

            // Validate the results
            assertEquals("Test Channel", response.channel.getSnippet().getTitle());
            assertEquals(1000L, response.channel.getStatistics().getSubscriberCount().longValue());
            assertEquals(2, response.videos.size());
            assertEquals("Video One", response.videos.get(0).getSnippet().getTitle());
            assertEquals("Video Two", response.videos.get(1).getSnippet().getTitle());
        }};
    }

    @Test
    public void testChannelDataWithValidChannelAndEmptyVideoList() {
        // Given a channel and an empty list of videos
        ChannelSnippet channelSnippet = new ChannelSnippet();
        channelSnippet.setTitle("Test Channel");
        ChannelStatistics channelStatistics = new ChannelStatistics();
        channelStatistics.setSubscriberCount(new BigInteger("1000"));

        Channel mockChannel = new Channel();
        mockChannel.setSnippet(channelSnippet);
        mockChannel.setStatistics(channelStatistics);

        List<SearchResult> searchResults = Arrays.asList(); // Empty list of videos

        // Create ChannelData with valid channel and empty video list
        ChannelActor.ChannelData channelData = new ChannelActor.ChannelData(mockChannel, searchResults);

        // When we access the channel and videos in ChannelData
        assertEquals(mockChannel, channelData.channel); // Channel should be the mock channel
        assertEquals(0, channelData.videos.size()); // Empty list of videos
    }


    @Test
    public void testChannelDataWithNullVideoInList() {
        // Given a channel and a list of videos with a null video
        ChannelSnippet channelSnippet = new ChannelSnippet();
        channelSnippet.setTitle("Test Channel");
        ChannelStatistics channelStatistics = new ChannelStatistics();
        channelStatistics.setSubscriberCount(new BigInteger("1000"));

        Channel mockChannel = new Channel();
        mockChannel.setSnippet(channelSnippet);
        mockChannel.setStatistics(channelStatistics);

        List<SearchResult> searchResults = Arrays.asList((SearchResult) null); // Null video in list

        // Create ChannelData with null video in the list
        ChannelActor.ChannelData channelData = new ChannelActor.ChannelData(mockChannel, searchResults);

        // When we access the channel and videos in ChannelData
        assertEquals(mockChannel, channelData.channel); // Channel should be the mock channel
        assertEquals(1, channelData.videos.size()); // One video (which is null)
        assertNull(channelData.videos.get(0)); // The video should be null
    }

    @Test
    public void testChannelDataWithNullChannelAndNullVideos() {
        // Given a null channel and a null list of videos
        ChannelActor.ChannelData channelData = new ChannelActor.ChannelData(null, null);

        // When we access the channel and videos in ChannelData
        assertNull(channelData.channel); // Channel should be null
        assertNull(channelData.videos);  // Videos list should be null
    }

    @Test
    public void testChannelRequestWithNullChannelId() {
        // Given a null channelId
        String channelId = null;

        // When we create a ChannelRequest with null ID
        ChannelActor.ChannelRequest request = new ChannelActor.ChannelRequest(channelId);

        // Then the channelId should be null
        assertNull(request.channelId);
    }

    @Test
    public void testChannelRequest() {
        // Given a channelId
        String channelId = "test-channel-id";

        // When we create a ChannelRequest
        ChannelActor.ChannelRequest request = new ChannelActor.ChannelRequest(channelId);

        // Then the channelId should be correctly set
        assertEquals(channelId, request.channelId);

        // Ensure the ChannelRequest object is not null
        assertNotNull(request);
    }

    @Test
    public void testChannelRequestWithEmptyChannelId() {
        // Given an empty channelId
        String channelId = "";

        // When we create a ChannelRequest with empty ID
        ChannelActor.ChannelRequest request = new ChannelActor.ChannelRequest(channelId);

        // Then the channelId should be empty
        assertEquals(channelId, request.channelId);
    }

    @Test
    public void testChannelDataWithEmptyVideoList() {
        // Given a channel and an empty list of videos
        ChannelSnippet channelSnippet = new ChannelSnippet();
        channelSnippet.setTitle("Test Channel");
        ChannelStatistics channelStatistics = new ChannelStatistics();
        channelStatistics.setSubscriberCount(new BigInteger("1000"));

        Channel mockChannel = new Channel();
        mockChannel.setSnippet(channelSnippet);
        mockChannel.setStatistics(channelStatistics);

        List<SearchResult> searchResults = Arrays.asList(); // Empty list

        // Create ChannelData with empty video list
        ChannelActor.ChannelData channelData = new ChannelActor.ChannelData(mockChannel, searchResults);

        // When we access the channel and videos in ChannelData
        assertEquals(mockChannel, channelData.channel);
        assertEquals(0, channelData.videos.size()); // Empty list of videos
    }

    @Test
    public void testChannelDataWithNullChannel() {
        // Given a null channel and a list of videos
        List<SearchResult> searchResults = Arrays.asList(new SearchResult());

        // Create ChannelData with null channel
        ChannelActor.ChannelData channelData = new ChannelActor.ChannelData(null, searchResults);

        // When we access the channel and videos in ChannelData
        assertNull(channelData.channel); // Channel should be null
        assertEquals(1, channelData.videos.size()); // Assuming there is one video
    }
}

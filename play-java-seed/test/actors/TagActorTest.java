import actors.TagActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import com.google.api.services.youtube.model.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TagActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testGetVideosByTagWithException() throws Exception {
        new TestKit(system) {{
            // Mock YouTube client
            YouTube youtube = mock(YouTube.class);
            YouTube.Search search = mock(YouTube.Search.class);
            YouTube.Search.List searchList = mock(YouTube.Search.List.class);

            when(youtube.search()).thenReturn(search);
            when(search.list(anyString())).thenReturn(searchList);

            // Make the mock throw an IOException when executing the search
            when(searchList.execute()).thenThrow(new IOException("API Error"));

            // Create and test TagActor
            Props props = TagActor.props(youtube, "mock-api-key");
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new TagActor.GetVideosByTag("test-tag"), probe.getRef());

            // Expect a failure message
            akka.actor.Status.Failure failure = probe.expectMsgClass(akka.actor.Status.Failure.class);

            // Verify that the failure contains the correct exception
            assertTrue(failure.cause() instanceof IOException);
            assertTrue(failure.cause().getMessage().contains("API Error"));
        }};
    }

    @Test
    public void testGetVideoDetailsWithException() throws Exception {
        new TestKit(system) {{
            // Mock YouTube client
            YouTube youtube = mock(YouTube.class);
            YouTube.Videos videos = mock(YouTube.Videos.class);
            YouTube.Videos.List videosList = mock(YouTube.Videos.List.class);

            when(youtube.videos()).thenReturn(videos);
            when(videos.list(anyString())).thenReturn(videosList);

            // Make the mock throw an IOException when executing the video request
            when(videosList.execute()).thenThrow(new IOException("Video Not Found"));

            // Create and test TagActor
            Props props = TagActor.props(youtube, "mock-api-key");
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new TagActor.GetVideoDetails("video1"), probe.getRef());

            // Expect a failure message
            akka.actor.Status.Failure failure = probe.expectMsgClass(akka.actor.Status.Failure.class);

            // Verify that the failure contains the correct exception
            assertTrue(failure.cause() instanceof IOException);
            assertTrue(failure.cause().getMessage().contains("Video Not Found"));
        }};
    }

    @Test
    public void testGetVideoTagsWithException() throws Exception {
        new TestKit(system) {{
            // Mock YouTube client
            YouTube youtube = mock(YouTube.class);
            YouTube.Videos videos = mock(YouTube.Videos.class);
            YouTube.Videos.List videosList = mock(YouTube.Videos.List.class);

            when(youtube.videos()).thenReturn(videos);
            when(videos.list(anyString())).thenReturn(videosList);

            // Make the mock throw an IOException when executing the video request for tags
            when(videosList.execute()).thenThrow(new IOException("Video Tags Not Found"));

            // Create and test TagActor
            Props props = TagActor.props(youtube, "mock-api-key");
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new TagActor.GetVideoTags("video1"), probe.getRef());

            // Expect a failure message
            akka.actor.Status.Failure failure = probe.expectMsgClass(akka.actor.Status.Failure.class);

            // Verify that the failure contains the correct exception
            assertTrue(failure.cause() instanceof IOException);
            assertTrue(failure.cause().getMessage().contains("Video Tags Not Found"));
        }};
    }

    @Test
    public void testGetVideosByTagSuccess() throws Exception {
        new TestKit(system) {{
            // Mock YouTube client
            YouTube youtube = mock(YouTube.class);
            YouTube.Search search = mock(YouTube.Search.class);
            YouTube.Search.List searchList = mock(YouTube.Search.List.class);

            when(youtube.search()).thenReturn(search);
            when(search.list(anyString())).thenReturn(searchList);

            // Create the correct ResourceId and set it in SearchResult
            ResourceId resourceId = new ResourceId();
            resourceId.setVideoId("video123");

            // Create SearchResult and set the ID
            SearchResult searchResult = new SearchResult();
            searchResult.setId(resourceId); // Set the ResourceId here
            searchResult.setSnippet(new SearchResultSnippet().setTitle("Test Video"));

            // Mock the response from the YouTube API
            when(searchList.execute()).thenReturn(new SearchListResponse().setItems(Arrays.asList(searchResult)));

            // Create and test TagActor
            Props props = TagActor.props(youtube, "mock-api-key");
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new TagActor.GetVideosByTag("test-tag"), probe.getRef());

            // Expect the result to be returned
            List<SearchResult> results = probe.expectMsgClass(List.class);
            assertTrue(results.size() > 0);
            assertTrue(results.get(0).getId().getVideoId().equals("video123"));
        }};
    }
}

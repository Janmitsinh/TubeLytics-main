import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import services.YouTubeService;
import actors.Messenger;
import akka.actor.Status.Failure;
import akka.actor.ActorRef;
import com.google.api.services.youtube.model.SearchResult;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MessengerTest {

    private static ActorSystem system;

    @Mock
    private YouTubeService mockYouTubeService;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    // Mock search results for testing
    private List<SearchResult> mockResults() {
        SearchResult result1 = mock(SearchResult.class);
        SearchResult result2 = mock(SearchResult.class);

        // Mock the snippet for each result
        when(result1.getSnippet().getTitle()).thenReturn("Video 1");
        when(result2.getSnippet().getTitle()).thenReturn("Video 2");

        return Arrays.asList(result1, result2);
    }

    private JsonNode mockQueryMessage(String query) {
        ObjectNode jsonNode = mock(ObjectNode.class);
        when(jsonNode.get("query")).thenReturn(new TextNode(query));  // Wrap the String in a TextNode
        return jsonNode;
    }

    @Test
    public void testPreStartAddsClient() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Create actor with mock service
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef messengerActor = system.actorOf(props);

            // Assert no interaction with YouTubeService, test should not send any messages yet
            expectNoMsg();
            verifyNoInteractions(mockYouTubeService);
        }};
    }

    @Test
    public void testPostStopRemovesClient() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Create actor
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef actorRef = system.actorOf(props);

            // Send a message to ensure actor is running
            actorRef.tell(new Object(), getRef());

            // Stop actor
            system.stop(actorRef);

            // Assert no further messages are being sent
            expectNoMsg();
        }};
    }

    @Test
    public void testHandleValidQueryMessage() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Mock YouTube service to return results
            when(mockYouTubeService.searchVideos("test query"))
                    .thenReturn(CompletableFuture.completedFuture(mockResults()));

            // Create actor
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef messengerActor = system.actorOf(props);

            // Send a valid query message
            JsonNode queryMessage = mockQueryMessage("test query");
            messengerActor.tell(queryMessage, getRef());

            // Expect a response containing video titles
            String expectedResponse = "Video 1, Video 2";
            String actualResponse = expectMsgClass(String.class);
            assertEquals(expectedResponse, actualResponse);

            verify(mockYouTubeService, times(1)).searchVideos("test query");
        }};
    }

    @Test
    public void testHandleInvalidQueryMessage() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Create actor
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef messengerActor = system.actorOf(props);

            // Send an invalid query message (null query)
            messengerActor.tell(mockQueryMessage(null), getRef());

            // Expect a failure response
            Failure failure = expectMsgClass(Failure.class);
            assertNotNull(failure.cause());
        }};
    }

    @Test
    public void testHandleEmptyQueryMessage() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Create actor
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef messengerActor = system.actorOf(props);

            // Send an empty query message
            JsonNode emptyQueryMessage = mockQueryMessage("");
            messengerActor.tell(emptyQueryMessage, getRef());

            // Expect an appropriate error message
            String actualResponse = expectMsgClass(String.class);
            assertEquals("Query cannot be empty.", actualResponse);

            verifyNoInteractions(mockYouTubeService);
        }};
    }

    @Test
    public void testYouTubeServiceErrorHandling() {
        new TestKit(system) {{
            MockitoAnnotations.openMocks(this);
            mockYouTubeService = mock(YouTubeService.class);

            // Mock YouTube service to throw an exception
            when(mockYouTubeService.searchVideos("error query"))
                    .thenThrow(new RuntimeException("YouTube service error"));

            // Create actor
            Props props = Messenger.props(getRef(), mockYouTubeService);
            ActorRef messengerActor = system.actorOf(props);

            // Send a query that causes an error
            JsonNode errorQueryMessage = mockQueryMessage("error query");
            messengerActor.tell(errorQueryMessage, getRef());

            // Expect a failure message
            Failure failure = expectMsgClass(Failure.class);
            assertNotNull(failure.cause());
            assertEquals("YouTube service error", failure.cause().getMessage());

            verify(mockYouTubeService, times(1)).searchVideos("error query");
        }};
    }
}

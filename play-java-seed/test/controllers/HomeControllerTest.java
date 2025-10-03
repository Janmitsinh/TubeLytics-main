import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import play.mvc.*;
import play.test.Helpers;
import play.test.WithApplication;
import akka.util.Timeout;
import akka.pattern.Patterns;
import actors.ChannelActor;
import scala.concurrent.Future;
import scala.concurrent.Await;
import java.util.concurrent.CompletionStage;
import java.util.Collections;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.SearchResult;
import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import controllers.HomeController;
import services.YouTubeService;
import com.google.api.services.youtube.YouTube;
import java.time.Duration;

public class HomeControllerTest extends WithApplication {

    private HomeController homeController;
    private ActorSystem actorSystem;
    private ActorRef mockChannelActor;

    @Before
    public void setUp() {
        actorSystem = app.injector().instanceOf(ActorSystem.class);
        mockChannelActor = mock(ActorRef.class);
        homeController = new HomeController(
                actorSystem,
                app.injector().instanceOf(Materializer.class),
                mock(YouTubeService.class),
                mock(YouTube.class)
        );
    }

    @Test
    public void testIndex() {
        Http.Request request = Helpers.fakeRequest()
                .method("GET")
                .uri("/")
                .build();

        Result result = homeController.index(request).toCompletableFuture().join();

        assertEquals(200, result.status());

        String content = Helpers.contentAsString(result);
        assertTrue(content.contains("WebSocket"));
        assertTrue(content.contains("ws://"));
    }

//    @Test
//    public void testGetChannelProfile() throws Exception {
//        // Simulate the response from the ChannelActor
//        String channelId = "testChannelId";
//        ChannelActor.ChannelData mockChannelData = mock(ChannelActor.ChannelData.class);
//        Channel mockChannel = mock(Channel.class);
//        when(mockChannelData.channel).thenReturn(mockChannel);
//        when(mockChannelData.videos).thenReturn(Collections.emptyList());
//
//        // Simulate the response for ask() call using Patterns.ask() mock
//        Future mockFuture = mock(Future.class);
//        when(Patterns.ask(mockChannelActor, any(), eq(Timeout.create(Duration.ofSeconds(10))))).thenReturn(mockFuture);
//
//        // Simulate the result from the Future
//        when(mockFuture.toCompletableFuture().join()).thenReturn(mockChannelData);
//
//        // Call the method under test
//        CompletionStage<Result> resultStage = homeController.getChannelProfile(channelId);
//
//        // Wait for the result
//        Result result = resultStage.toCompletableFuture().join();
//
//        // Assert the expected result
//        assertEquals(200, result.status());
//        String content = Helpers.contentAsString(result);
//        assertTrue(content.contains("Channel Profile"));
//    }
}

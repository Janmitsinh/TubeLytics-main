import actors.Word;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.VideoListResponse;

import static org.mockito.Mockito.*;

public class WordTest {

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
    public void testWordStats() throws Exception {
        new TestKit(system) {{
            // Mock YouTube client
            YouTube youtube = mock(YouTube.class);
            YouTube.Search search = mock(YouTube.Search.class);
            YouTube.Search.List searchList = mock(YouTube.Search.List.class);
            YouTube.Videos videos = mock(YouTube.Videos.class);
            YouTube.Videos.List videosList = mock(YouTube.Videos.List.class);

            when(youtube.search()).thenReturn(search);
            when(search.list(anyString())).thenReturn(searchList);
            when(youtube.videos()).thenReturn(videos);
            when(videos.list(anyString())).thenReturn(videosList);

            // Mock search results
            SearchResult searchResult1 = new SearchResult();
            SearchResult searchResult2 = new SearchResult();

            searchResult1.setId(new ResourceId().setVideoId("video1"));
            searchResult2.setId(new ResourceId().setVideoId("video2"));

            List<SearchResult> searchResults = Arrays.asList(searchResult1, searchResult2);
            SearchListResponse searchListResponse = mock(SearchListResponse.class);

            when(searchList.execute()).thenReturn(searchListResponse);
            when(searchListResponse.getItems()).thenReturn(searchResults);

            // Mock video details
            Video video1 = new Video();
            VideoSnippet snippet1 = new VideoSnippet();
            snippet1.setTitle("Title One");
            snippet1.setDescription("Description One");
            video1.setSnippet(snippet1);

            Video video2 = new Video();
            VideoSnippet snippet2 = new VideoSnippet();
            snippet2.setTitle("Title Two");
            snippet2.setDescription("Description Two");
            video2.setSnippet(snippet2);

            List<Video> videoDetails = Arrays.asList(video1, video2);
            VideoListResponse videoListResponse = mock(VideoListResponse.class);

            when(videosList.execute()).thenReturn(videoListResponse);
            when(videoListResponse.getItems()).thenReturn(videoDetails);

            // Create and test Word actor
            Props props = Word.props(youtube, "mock-api-key");
            TestKit probe = new TestKit(system);
            system.actorOf(props).tell(new Word.Key("test-query"), probe.getRef());

            List<Map.Entry<String, Long>> expectedStats = Arrays.asList(
                            new AbstractMap.SimpleEntry<>("title", 2L),
                            new AbstractMap.SimpleEntry<>("one", 2L),
                            new AbstractMap.SimpleEntry<>("two", 2L),
                            new AbstractMap.SimpleEntry<>("description", 2L)
                    ).stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .collect(Collectors.toList());

            List<Map.Entry<String, Long>> actualStats = probe.expectMsgClass(List.class);
            assertEquals(expectedStats, actualStats);
        }};
    }

}

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.YouTubeService;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class YouTubeServiceTest {

    private YouTubeService youTubeService;
    private YouTube mockYouTube;
    private YouTube.Search mockSearch;
    private YouTube.Search.List mockSearchList;

    @Before
    public void setup() throws IOException {
        // Create mocks for dependencies
        mockYouTube = mock(YouTube.class);
        mockSearch = mock(YouTube.Search.class);
        mockSearchList = mock(YouTube.Search.List.class);

        // Mock YouTube search object to return a mocked search list
        when(mockYouTube.search()).thenReturn(mockSearch);
        when(mockSearch.list("id,snippet")).thenReturn(mockSearchList);

        youTubeService = new YouTubeService(mockYouTube);
    }

    @Test
    public void testFetchVideoDetails_Success() throws IOException {
        // Mock the response
        SearchListResponse mockResponse = mock(SearchListResponse.class);
        SearchResult mockResult = mock(SearchResult.class);
        ResourceId mockResourceId = mock(ResourceId.class);
        SearchResultSnippet mockSnippet = mock(SearchResultSnippet.class);

        // Set up mock details for the video
        when(mockResourceId.getVideoId()).thenReturn("video123");
        when(mockSnippet.getTitle()).thenReturn("Test Video Title");
        when(mockSnippet.getDescription()).thenReturn("Test Video Description");
        when(mockSnippet.getChannelTitle()).thenReturn("Test Channel");
        when(mockSnippet.getChannelId()).thenReturn("channel123");

        // Set the mock response to return the list of mock results
        when(mockResponse.getItems()).thenReturn(List.of(mockResult));

        // Set the response to return the mock response when execute is called
        when(mockSearchList.execute()).thenReturn(mockResponse);

        // Mock the result's ID and snippet
        when(mockResult.getId()).thenReturn(mockResourceId);
        when(mockResult.getSnippet()).thenReturn(mockSnippet);

        // Test the method
        List<SearchResult> results = youTubeService.fetchVideoDetails("Akka");

        // Verify the results
        assertEquals(1, results.size());
        assertEquals("video123", results.get(0).getId().getVideoId());
        assertEquals("Test Video Title", results.get(0).getSnippet().getTitle());
        assertEquals("Test Video Description", results.get(0).getSnippet().getDescription());
        assertEquals("Test Channel", results.get(0).getSnippet().getChannelTitle());
        assertEquals("channel123", results.get(0).getSnippet().getChannelId());

        // Verify the API call was made without checking for the API key
        verify(mockSearchList).setQ("Akka");
        verify(mockSearchList).setType("video");
        verify(mockSearchList).setMaxResults(10L);
        verify(mockSearchList).setOrder("date");
    }

    private List<SearchResult> handleIOException(IOException e) {
        // Log the exception (or print the stack trace)
        e.printStackTrace();

        // Return an empty list when an exception occurs
        return List.of();
    }

    @Test
    public void testFetchVideoDetails_Failure() throws IOException {
        // Simulate an API exception
        when(mockSearchList.execute()).thenThrow(new IOException("API failure"));

        // Test the method
        List<SearchResult> results = youTubeService.fetchVideoDetails("Akka");

        // Verify the results (empty list due to failure)
        assertEquals(0, results.size());
    }

    @Test
    public void testFetchVideoDetails_EmptyResults() throws IOException {
        // Simulate an empty list of items from the API response
        SearchListResponse mockResponse = mock(SearchListResponse.class);
        when(mockResponse.getItems()).thenReturn(List.of()); // Return an empty list
        when(mockSearchList.execute()).thenReturn(mockResponse);

        // Test the method
        List<SearchResult> results = youTubeService.fetchVideoDetails("No Results");

        // Verify the results (empty list)
        assertEquals(0, results.size());
    }

    @Test
    public void testFetchVideoDetails_EmptySearchQuery() throws IOException {
        // Simulate an empty search query
        SearchListResponse mockResponse = mock(SearchListResponse.class);
        SearchResult mockResult = mock(SearchResult.class);
        when(mockSearchList.execute()).thenReturn(mockResponse);

        // Test the method with an empty query
        List<SearchResult> results = youTubeService.fetchVideoDetails("");

        // Verify the results (empty list)
        assertEquals(0, results.size());
    }

}

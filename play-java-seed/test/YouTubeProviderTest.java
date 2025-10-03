import com.google.api.services.youtube.YouTube;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class YouTubeProviderTest {

    private YouTubeProvider youtubeProvider;

    @Before
    public void setUp() {
        // Create an instance of the YouTubeProvider for testing
        youtubeProvider = new YouTubeProvider();
    }

    @Test
    public void testYouTubeProvider_Success() {
        // Call the get() method of the provider to get the YouTube instance
        YouTube youtubeClient = youtubeProvider.get();

        // Assert that the YouTube client is not null
        assertNotNull("YouTube client should not be null", youtubeClient);
    }

//    @Test
//    public void testYouTubeProvider_Exception() {
//        // Mock static methods using Mockito.mockStatic()
//        try (MockedStatic<GoogleNetHttpTransport> transportMock = mockStatic(GoogleNetHttpTransport.class);
//             MockedStatic<JacksonFactory> jsonFactoryMock = mockStatic(JacksonFactory.class)) {
//
//            // Simulate an exception in the GoogleNetHttpTransport.newTrustedTransport() method
//            transportMock.when(GoogleNetHttpTransport::newTrustedTransport).thenThrow(new RuntimeException("Transport error"));
//
//            // Simulate the JacksonFactory.getDefaultInstance() method to work normally
//            jsonFactoryMock.when(JacksonFactory::getDefaultInstance).thenReturn(mock(JsonFactory.class));
//
//            // Call the provider's get() method, expecting it to throw a RuntimeException
//            try {
//                youtubeProvider.get();
//                fail("Expected RuntimeException to be thrown");
//            } catch (RuntimeException e) {
//                // Assert that the exception message matches the expected message
//                assertEquals("Failed to initialize YouTube client", e.getMessage());
//                // Assert that the cause of the exception is a RuntimeException with the expected message
//                assertTrue(e.getCause() instanceof RuntimeException);
//                assertEquals("Transport error", e.getCause().getMessage());
//            }
//        }
//    }
}

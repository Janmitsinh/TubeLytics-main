import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.api.services.youtube.YouTube;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModuleTest {

    private Injector injector;

    @Before
    public void setUp() {
        // Create the injector with the Module that binds the YouTube API client
        injector = Guice.createInjector(new Module());
    }

    @Test
    public void testYouTubeInjection() {
        // Get the injected YouTube client from Guice
        YouTube youtubeClient = injector.getInstance(YouTube.class);

        // Assert that the YouTube client is not null
        assertNotNull("YouTube client should not be null", youtubeClient);
    }
}

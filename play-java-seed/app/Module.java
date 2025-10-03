import java.util.ServiceLoader.Provider;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.inject.AbstractModule;

/**
 * Guice module that configures bindings for the YouTube API client.
 * This module binds the YouTube client to a provider that creates and configures
 * an instance of the YouTube API client.
 * @author Janmitsinh Panjrolia
 */
public class Module extends AbstractModule {

    /**
     * Configures the bindings for the application, including the YouTube API client.
     * Binds the lass to a provider that creates a YouTube client instance.
     */
    @Override
    protected void configure() {
        // Bind the YouTube API client
        bind(YouTube.class).toProvider(YouTubeProvider.class);
    }
}

/**
 * Provider for creating and configuring a YouTube API client instance.
 * This class handles the initialization of the YouTube client with necessary
 * configurations, including the application name and transport mechanism.
 */
class YouTubeProvider implements Provider<YouTube> {

    /**
     * Provides a new instance of the YouTube API client.
     * Initializes the client with the default JSON factory, trusted transport,
     * and application name.
     *
     * @return A configured {@link YouTube} client instance.
     * @throws RuntimeException if the YouTube client initialization fails.
     */
    @Override
    public YouTube get() {
        try {
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            return new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory,
                    null
            ).setApplicationName("play-java-seed").build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize YouTube client", e);
        }
    }
}

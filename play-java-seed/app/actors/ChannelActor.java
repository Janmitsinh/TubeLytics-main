package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.SearchResult;
import services.ChannelService;

import java.io.IOException;
import java.util.List;

/**
 * ChannelActor is an Akka actor responsible for handling requests related to YouTube channel information.
 * It interacts with the ChannelService to fetch channel details and videos based on a given channel ID.
 * The actor will respond with the channel's information and a list of videos or an error in case of failure.
 * @author Janmitsinh Panjrolia
 */
public class ChannelActor extends AbstractActor {

    private final ChannelService channelService;

    /**
     * Constructor for the ChannelActor.
     *
     * @param channelService The service used to fetch channel information and videos.
     */
    public ChannelActor(ChannelService channelService) {
        this.channelService = channelService;
    }

    /**
     * Factory method to create a Props instance for the ChannelActor.
     *
     * @param channelService The service used to fetch channel information and videos.
     * @return A Props instance for creating a ChannelActor.
     */
    public static Props props(ChannelService channelService) {
        return Props.create(ChannelActor.class, () -> new ChannelActor(channelService));
    }

    /**
     * Defines the behavior of the ChannelActor when receiving messages.
     * It listens for  messages, fetches the channel's information and videos,
     * and sends a {@link ChannelData} response to the sender.
     * In case of an error, a failure message is sent back.
     *
     * @return The behavior of the actor when receiving messages.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ChannelRequest.class, request -> {
                    try {
                        Channel channel = channelService.getChannelInfo(request.channelId);
                        List<SearchResult> videos = channelService.getChannelVideos(request.channelId);
                        sender().tell(new ChannelData(channel, videos), self());
                    } catch (IOException e) {
                        sender().tell(new akka.actor.Status.Failure(e), self());
                    }
                })
                .build();
    }

    /**
     * Represents a request to fetch information about a specific YouTube channel.
     */
    public static class ChannelRequest {
        public final String channelId;

        /**
         * Constructor for ChannelRequest.
         *
         * @param channelId The ID of the YouTube channel for which information is requested.
         */
        public ChannelRequest(String channelId) {
            this.channelId = channelId;
        }
    }

    /**
     * Represents the data returned by the ChannelActor containing channel information and a list of videos.
     */
    public static class ChannelData {
        public final Channel channel;
        public final List<SearchResult> videos;

        /**
         * Constructor for ChannelData.
         *
         * @param channel The YouTube channel information.
         * @param videos  The list of videos associated with the channel.
         */
        public ChannelData(Channel channel, List<SearchResult> videos) {
            this.channel = channel;
            this.videos = videos;
        }
    }
}

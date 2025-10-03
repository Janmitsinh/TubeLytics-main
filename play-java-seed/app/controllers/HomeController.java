package controllers;

import play.mvc.*;
import play.libs.streams.ActorFlow;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import services.YouTubeService;
import actors.Messenger;
import java.util.*;
import models.*;
import akka.actor.ActorRef;
import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import scala.compat.java8.FutureConverters;
import akka.pattern.Patterns;
import actors.Word;
import actors.ChannelActor;
import actors.ReadabilityActor; // Import ReadabilityActor
import actors.TagActor;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import views.html.tags;
import views.html.videoTags;
import akka.util.Timeout;
import services.ChannelService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HomeController extends Controller {

    private final ActorSystem actorSystem;
    private final Materializer materializer;
    private final YouTubeService youTubeService;
    private final YouTube youtube;
    private final String apiKey;
    private final Timeout timeout;
    private final ActorRef tagActor;
    private final ActorRef channelActor;

    // Create a cache that expires items after 10 minutes
    private final Cache<String, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    @Inject
    public HomeController(ActorSystem actorSystem, Materializer materializer, YouTubeService youTubeService, YouTube youtube) {
        this.actorSystem = actorSystem;
        this.materializer = materializer;
        this.youTubeService = youTubeService;
        this.youtube = youtube;
        this.apiKey = "AIzaSyBE1vlaVKOrkty9xU-fglNBHteMfX9kavw";
        ChannelService channelService = new ChannelService(youtube, apiKey);
        this.timeout = Timeout.create(java.time.Duration.ofSeconds(100000));
        this.tagActor = actorSystem.actorOf(TagActor.props(youtube, apiKey), "tagActor");
        this.channelActor = actorSystem.actorOf(ChannelActor.props(channelService), "channelActor");
    }

    public CompletionStage<Result> index(Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            String url = routes.HomeController.socket().webSocketURL(request);
            return ok(views.html.index.render(url));
        });
    }

    public Result getCacheData(String key) {
        String cachedData = cache.getIfPresent(key);
        if (cachedData != null) {
            return ok(cachedData);
        } else {
            return notFound("No data found in cache");
        }
    }

    public Result setCacheData(String key, String value) {
        cache.put(key, value);
        return ok("Cache updated");
    }

    public WebSocket socket() {
        return WebSocket.Json.accept(request -> ActorFlow.actorRef(
                out -> Messenger.props(out, youTubeService), actorSystem, materializer
        ));
    }

    public CompletionStage<Result> getWordStats(String query) {
        return Patterns.ask(
                actorSystem.actorOf(Word.props(youtube, apiKey)),
                new Word.Key(query),
                Duration.ofSeconds(10)
        ).thenApplyAsync(response -> {
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map.Entry<String, Long>> stats = (List<Map.Entry<String, Long>>) response;
                return ok(views.html.word_stats.render(stats));
            } else {
                return internalServerError("Unexpected response from Word actor.");
            }
        });
    }

    public CompletionStage<Result> getChannelProfile(String channelId) {
        return Patterns.ask(
                channelActor,
                new ChannelActor.ChannelRequest(channelId),
                Duration.ofSeconds(10)
        ).thenApplyAsync(response -> {
            if (response instanceof ChannelActor.ChannelData) {
                ChannelActor.ChannelData channelData = (ChannelActor.ChannelData) response;
                return ok(views.html.channel_profile.render(channelData.channel, channelData.videos));
            } else {
                return internalServerError("Error fetching channel profile");
            }
        });
    }

    public ActorRef getChannelActor() {
        return channelActor;
    }

    public CompletionStage<Result> videosByTag(String tag) {
        CompletionStage<Object> future = FutureConverters.toJava(
                Patterns.ask(tagActor, new TagActor.GetVideosByTag(tag), timeout)
        );

        return future.thenApply(results -> {
            @SuppressWarnings("unchecked")
            List<SearchResult> searchResults = (List<SearchResult>) results;
            return ok(tags.render(tag, searchResults));
        });
    }

    public CompletionStage<Result> videoTags(String videoId) {
        CompletionStage<Object> videoFuture = FutureConverters.toJava(
                Patterns.ask(tagActor, new TagActor.GetVideoDetails(videoId), timeout)
        );

        return videoFuture.thenCompose(videoDetails -> {
            CompletionStage<Object> tagsFuture = FutureConverters.toJava(
                    Patterns.ask(tagActor, new TagActor.GetVideoTags(videoId), timeout)
            );

            return tagsFuture.thenApply(tags -> {
                @SuppressWarnings("unchecked")
                List<String> tagList = (List<String>) tags;
                return ok(videoTags.render((Video) videoDetails, tagList));
            });
        });
    }

    // New Method for Description Readability
    public CompletionStage<Result> calculateReadability(Http.Request request) {
        JsonNode json = request.body().asJson();
        if (json == null || !json.has("descriptions")) {
            return CompletableFuture.completedFuture(badRequest("Invalid request, 'descriptions' field is missing."));
        }

        List<String> descriptions = new ArrayList<>();
        json.get("descriptions").forEach(node -> descriptions.add(node.asText()));

        // Limit to maximum 50 descriptions
List<String> limitedDescriptions = descriptions.subList(0, Math.min(descriptions.size(), 50));

// Create the ReadabilityActor
ActorRef readabilityActor = actorSystem.actorOf(ReadabilityActor.props(), "readabilityActor");

// Process each description asynchronously
List<CompletionStage<Object>> readabilityFutures = new ArrayList<>();

for (String description : limitedDescriptions) {
    readabilityFutures.add(Patterns.ask(
            readabilityActor,
            new ReadabilityActor.ComputeReadability(description),
            Duration.ofSeconds(10) // Corrected syntax
    ));
}

// Combine all results into a single response
return CompletableFuture.allOf(readabilityFutures.toArray(new CompletableFuture[0]))
        .thenApply(v -> readabilityFutures.stream()
                .map(CompletionStage::toCompletableFuture)
                .map(CompletableFuture::join)
                .map(result -> (ReadabilityActor.ReadabilityResult) result)
                .collect(Collectors.toList()))
        .thenApply(readabilityResults -> {
            ArrayNode resultArray = Json.newArray();
            readabilityResults.forEach(result -> {
                ObjectNode resultJson = Json.newObject(); // Ensure ObjectNode is created properly
                resultJson.put("fleschEase", result.fleschEase);
                resultJson.put("fleschGrade", result.fleschGrade);
                resultArray.add(resultJson);
            });
            return ok(resultArray);
        });

}

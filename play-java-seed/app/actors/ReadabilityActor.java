package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;

// Define the ReadabilityActor class
public class ReadabilityActor extends AbstractActor {

    // Props factory method for creating instances of ReadabilityActor
    public static Props props() {
        return Props.create(ReadabilityActor.class);
    }

    // Define the message classes
    public static class ComputeReadability {
        public final String description;

        public ComputeReadability(String description) {
            this.description = description;
        }
    }

    public static class ReadabilityResult {
        public final double fleschEase;
        public final double fleschGrade;

        public ReadabilityResult(double fleschEase, double fleschGrade) {
            this.fleschEase = fleschEase;
            this.fleschGrade = fleschGrade;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComputeReadability.class, this::handleComputeReadability)
                .build();
    }

    private void handleComputeReadability(ComputeReadability message) {
        double fleschEase = calculateFleschEase(message.description);
        double fleschGrade = calculateFleschGrade(message.description);
        sender().tell(new ReadabilityResult(fleschEase, fleschGrade), self());
    }

    private double calculateFleschEase(String text) {
        return 206.835 - 1.015 * wordsPerSentence(text) - 84.6 * syllablesPerWord(text);
    }

    private double calculateFleschGrade(String text) {
        return 0.39 * wordsPerSentence(text) + 11.8 * syllablesPerWord(text) - 15.59;
    }

    private double wordsPerSentence(String text) {
        String[] sentences = text.split("[.!?]");
        int wordCount = text.split("\\s+").length;
        return sentences.length == 0 ? 0 : (double) wordCount / sentences.length;
    }

    private double syllablesPerWord(String text) {
        String[] words = text.split("\\s+");
        int totalSyllables = 0;
        for (String word : words) {
            totalSyllables += countSyllables(word);
        }
        return words.length == 0 ? 0 : (double) totalSyllables / words.length;
    }

    private int countSyllables(String word) {
        word = word.toLowerCase().replaceAll("[^a-z]", "");
        int count = 0;
        boolean lastWasVowel = false;
        for (char c : word.toCharArray()) {
            if ("aeiouy".indexOf(c) >= 0) {
                if (!lastWasVowel) count++;
                lastWasVowel = true;
            } else {
                lastWasVowel = false;
            }
        }
        if (word.endsWith("e")) count--;
        return Math.max(count, 1);
    }
}

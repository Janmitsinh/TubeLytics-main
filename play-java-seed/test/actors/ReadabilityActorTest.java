package actors;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import actors.ReadabilityActor.*;

import static org.junit.Assert.assertEquals;

public class ReadabilityActorTest {

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
    public void testReadabilityComputation() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "This is a simple test sentence. It should calculate readability scores.";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835 - 1.015 * (10.0 / 2) - 84.6 * (15.0 / 10);
            double expectedFleschGrade = 0.39 * (10.0 / 2) + 11.8 * (15.0 / 10) - 15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testEmptyText() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835;
            double expectedFleschGrade = -15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testSingleWord() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "Word.";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835 - 1.015 * 1.0 - 84.6 * 1.0;
            double expectedFleschGrade = 0.39 * 1.0 + 11.8 * 1.0 - 15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testSpecialCharactersOnly() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "!@#$%^&*()";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835;
            double expectedFleschGrade = -15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testNumbersAndText() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "The cost of this item is 42.99 dollars.";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835 - 1.015 * (7.0 / 1) - 84.6 * (9.0 / 7);
            double expectedFleschGrade = 0.39 * (7.0 / 1) + 11.8 * (9.0 / 7) - 15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testLongText() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "This is a long paragraph that contains multiple sentences. " +
                    "Each sentence is designed to test the accuracy of the readability computation. " +
                    "The paragraph consists of short and long sentences to mimic natural writing styles. " +
                    "By using such a paragraph, we can ensure the calculations are robust.";

            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 60.0; // Placeholder, compute manually
            double expectedFleschGrade = 8.0; // Placeholder, compute manually

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testEdgeCaseNoVowels() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "Shhh psst hmm grr.";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835 - 1.015 * (4.0 / 1) - 84.6 * (4.0 / 4);
            double expectedFleschGrade = 0.39 * (4.0 / 1) + 11.8 * (4.0 / 4) - 15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }

    @Test
    public void testPunctuationHeavyText() {
        new TestKit(system) {{
            final var readabilityActor = system.actorOf(ReadabilityActor.props());

            String testDescription = "Wait! What? Really?! Oh...";
            readabilityActor.tell(new ComputeReadability(testDescription), getRef());

            ReadabilityResult result = expectMsgClass(ReadabilityResult.class);

            double expectedFleschEase = 206.835 - 1.015 * (4.0 / 4) - 84.6 * (4.0 / 4);
            double expectedFleschGrade = 0.39 * (4.0 / 4) + 11.8 * (4.0 / 4) - 15.59;

            assertEquals(expectedFleschEase, result.fleschEase, 0.01);
            assertEquals(expectedFleschGrade, result.fleschGrade, 0.01);
        }};
    }
}

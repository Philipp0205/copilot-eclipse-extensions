package copilot.eclipse.extensions.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SessionInfoTest {

  private static final Instant CREATED = Instant.parse("2026-05-01T09:00:00Z");
  private static final Instant LAST_MESSAGE = Instant.parse("2026-05-01T11:30:00Z");

  @Test
  void prefersTheLastMessageTimeForActivity() {
    SessionInfo session = new SessionInfo("id", "Title", CREATED, LAST_MESSAGE);

    assertEquals(LAST_MESSAGE, session.lastActivity());
  }

  @Test
  void usesCreationTimeWhenNothingHasBeenSentYet() {
    SessionInfo session = new SessionInfo("id", "Title", CREATED, null);

    assertEquals(CREATED, session.lastActivity());
  }

  @Test
  void hasNoActivityWithoutAnyTimestamps() {
    SessionInfo session = new SessionInfo("id", "Title", null, null);

    assertEquals(Instant.EPOCH, session.lastActivity());
  }

  @Test
  void treatsAMissingTitleAsUntitled() {
    assertTrue(new SessionInfo("id", null, CREATED, null).isUntitled());
    assertTrue(new SessionInfo("id", "   ", CREATED, null).isUntitled());
  }

  @Test
  void treatsTheIdEchoedAsTitleAsUntitled() {
    // Copilot reports the conversation id as the title until it generates a real one.
    assertTrue(new SessionInfo("conv-1", "conv-1", CREATED, null).isUntitled());
  }

  @Test
  void keepsARealTitle() {
    assertFalse(new SessionInfo("conv-1", "Fix the parser", CREATED, null).isUntitled());
  }

  @Test
  void comparesByIdOnly() {
    SessionInfo first = new SessionInfo("id", "One", CREATED, null);
    SessionInfo second = new SessionInfo("id", "Two", LAST_MESSAGE, LAST_MESSAGE);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }
}

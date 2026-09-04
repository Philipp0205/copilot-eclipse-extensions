package copilot.eclipse.extensions.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class SessionListTest {

  private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

  @Test
  void sortsMostRecentlyActiveFirst() {
    SessionInfo oldest = session("a", "Oldest", NOW.minus(Duration.ofDays(3)));
    SessionInfo newest = session("b", "Newest", NOW.minus(Duration.ofMinutes(2)));
    SessionInfo middle = session("c", "Middle", NOW.minus(Duration.ofHours(5)));

    List<SessionInfo> sorted = SessionList.sortedByMostRecent(Arrays.asList(oldest, newest, middle));

    assertEquals(List.of("b", "c", "a"), ids(sorted));
  }

  @Test
  void fallsBackToCreationDateAndSortsUndatedSessionsLast() {
    SessionInfo created = new SessionInfo("a", "Created only", NOW.minus(Duration.ofMinutes(30)), null);
    SessionInfo undated = new SessionInfo("b", "No dates", null, null);
    SessionInfo messaged = session("c", "Messaged", NOW.minus(Duration.ofMinutes(5)));

    List<SessionInfo> sorted = SessionList.sortedByMostRecent(Arrays.asList(undated, created, messaged));

    assertEquals(List.of("c", "a", "b"), ids(sorted));
  }

  @Test
  void dropsSessionsWithoutAnId() {
    SessionInfo valid = session("a", "Valid", NOW);
    SessionInfo blank = session("  ", "Blank id", NOW);

    List<SessionInfo> sorted = SessionList.sortedByMostRecent(Arrays.asList(valid, blank, null));

    assertEquals(List.of("a"), ids(sorted));
  }

  @Test
  void cyclesForwardAndWrapsAround() {
    List<SessionInfo> sessions = threeSessions();

    assertEquals("b", SessionList.relativeTo(sessions, "a", 1).getId());
    assertEquals("c", SessionList.relativeTo(sessions, "b", 1).getId());
    assertEquals("a", SessionList.relativeTo(sessions, "c", 1).getId());
  }

  @Test
  void cyclesBackwardAndWrapsAround() {
    List<SessionInfo> sessions = threeSessions();

    assertEquals("a", SessionList.relativeTo(sessions, "b", -1).getId());
    assertEquals("c", SessionList.relativeTo(sessions, "a", -1).getId());
  }

  @Test
  void startsAtAnEndWhenNoSessionIsActive() {
    List<SessionInfo> sessions = threeSessions();

    assertEquals("a", SessionList.relativeTo(sessions, null, 1).getId());
    assertEquals("c", SessionList.relativeTo(sessions, null, -1).getId());
    assertEquals("a", SessionList.relativeTo(sessions, "unknown", 1).getId());
  }

  @Test
  void hasNothingToCycleToWhenListIsEmpty() {
    assertNull(SessionList.relativeTo(Collections.emptyList(), "a", 1));
  }

  @Test
  void describesRecentActivityRelativeToNow() {
    assertEquals("just now", SessionList.relativeTime(NOW.minus(Duration.ofSeconds(20)), NOW));
    assertEquals("5 min ago", SessionList.relativeTime(NOW.minus(Duration.ofMinutes(5)), NOW));
    assertEquals("3 h ago", SessionList.relativeTime(NOW.minus(Duration.ofHours(3)), NOW));
    assertEquals("2 d ago", SessionList.relativeTime(NOW.minus(Duration.ofDays(2)), NOW));
    assertEquals("", SessionList.relativeTime(null, NOW));
    assertEquals("", SessionList.relativeTime(Instant.EPOCH, NOW));
  }

  @Test
  void describesOlderActivityWithAnAbsoluteTimestamp() {
    String label = SessionList.relativeTime(NOW.minus(Duration.ofDays(30)), NOW);

    assertTrue(label.startsWith("2026-04-01"), "expected an absolute date but got " + label);
  }

  @Test
  void treatsClockSkewAsJustNow() {
    assertEquals("just now", SessionList.relativeTime(NOW.plus(Duration.ofMinutes(5)), NOW));
  }

  private static List<SessionInfo> threeSessions() {
    return List.of(
        session("a", "First", NOW.minus(Duration.ofMinutes(1))),
        session("b", "Second", NOW.minus(Duration.ofMinutes(2))),
        session("c", "Third", NOW.minus(Duration.ofMinutes(3))));
  }

  private static SessionInfo session(String id, String title, Instant lastMessage) {
    return new SessionInfo(id, title, lastMessage, lastMessage);
  }

  private static List<String> ids(List<SessionInfo> sessions) {
    return sessions.stream().map(SessionInfo::getId).toList();
  }
}

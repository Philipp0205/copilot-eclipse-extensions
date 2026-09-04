package copilot.eclipse.extensions.sessions;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Ordering and formatting of the session list, kept free of workbench and Copilot runtime state so
 * it can be tested directly.
 */
final class SessionList {

  private static final DateTimeFormatter ABSOLUTE_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  /** Most recently active session first, with the session id as a stable tie-break. */
  static final Comparator<SessionInfo> BY_MOST_RECENT = (left, right) -> {
    int byRecency = right.lastActivity().compareTo(left.lastActivity());
    return byRecency != 0 ? byRecency : left.getId().compareTo(right.getId());
  };

  private SessionList() {
  }

  static List<SessionInfo> sortedByMostRecent(Collection<SessionInfo> sessions) {
    List<SessionInfo> sorted = new ArrayList<>(sessions);
    sorted.removeIf(session -> session == null || isBlank(session.getId()));
    sorted.sort(BY_MOST_RECENT);
    return sorted;
  }

  static int indexOf(List<SessionInfo> sessions, String sessionId) {
    if (sessionId == null) {
      return -1;
    }
    for (int i = 0; i < sessions.size(); i++) {
      if (sessionId.equals(sessions.get(i).getId())) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the session {@code offset} positions from the one identified by {@code activeId},
   * wrapping around the ends. When {@code activeId} is not in the list, stepping forward starts at
   * the most recent session and stepping backward at the oldest.
   */
  static SessionInfo relativeTo(List<SessionInfo> sessions, String activeId, int offset) {
    if (sessions.isEmpty()) {
      return null;
    }
    int current = indexOf(sessions, activeId);
    if (current < 0) {
      return sessions.get(offset >= 0 ? 0 : sessions.size() - 1);
    }
    int size = sessions.size();
    int target = ((current + offset) % size + size) % size;
    return sessions.get(target);
  }

  /** Human readable age, falling back to an absolute timestamp for anything over a week old. */
  static String relativeTime(Instant activity, Instant now) {
    if (activity == null || Instant.EPOCH.equals(activity)) {
      return "";
    }
    Duration age = Duration.between(activity, now);
    if (age.isNegative() || age.toMinutes() < 1) {
      return "just now";
    }
    if (age.toHours() < 1) {
      return age.toMinutes() + " min ago";
    }
    if (age.toDays() < 1) {
      return age.toHours() + " h ago";
    }
    if (age.toDays() < 7) {
      return age.toDays() + " d ago";
    }
    return ABSOLUTE_DATE.format(activity.atZone(ZoneId.systemDefault()));
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}

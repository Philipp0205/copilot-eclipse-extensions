package copilot.eclipse.extensions.sessions;

import java.time.Instant;
import java.util.Objects;

/**
 * A saved Copilot chat session.
 *
 * <p>
 * This mirrors the parts of Copilot's persisted conversation summary that the session UI needs, so
 * the rest of this plugin does not have to handle Copilot's internal persistence types.
 * </p>
 */
public final class SessionInfo {

  private final String id;
  private final String rawTitle;
  private final Instant createdAt;
  private final Instant lastMessageAt;

  public SessionInfo(String id, String rawTitle, Instant createdAt, Instant lastMessageAt) {
    this.id = id;
    this.rawTitle = rawTitle;
    this.createdAt = createdAt;
    this.lastMessageAt = lastMessageAt;
  }

  public String getId() {
    return id;
  }

  public String getRawTitle() {
    return rawTitle;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastMessageAt() {
    return lastMessageAt;
  }

  /** When this session was last used, falling back to its creation time. */
  public Instant lastActivity() {
    if (lastMessageAt != null) {
      return lastMessageAt;
    }
    return createdAt != null ? createdAt : Instant.EPOCH;
  }

  /**
   * Whether Copilot has not titled this session yet. Copilot reports the conversation id as the
   * title until a title has been generated, so that counts as untitled.
   */
  public boolean isUntitled() {
    if (rawTitle == null || rawTitle.trim().isEmpty()) {
      return true;
    }
    return rawTitle.trim().equals(id);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof SessionInfo && Objects.equals(id, ((SessionInfo) other).id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "SessionInfo[id=" + id + ", title=" + rawTitle + "]";
  }
}

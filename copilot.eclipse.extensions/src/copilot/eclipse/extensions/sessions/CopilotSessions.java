package copilot.eclipse.extensions.sessions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

import copilot.eclipse.extensions.CopilotExtensionsPlugin;

/**
 * Session management on top of the single Copilot chat view.
 *
 * <p>
 * Copilot for Eclipse persists every conversation but only surfaces them through the chat history
 * panel, and its chat services are bound to one chat view instance at a time. So a "session" here is
 * a persisted Copilot conversation, and switching sessions swaps the conversation shown in that one
 * chat view instead of opening a second one.
 * </p>
 *
 * <p>
 * This class is the only place that touches Copilot's own types; everything else works with
 * {@link SessionInfo}.
 * </p>
 */
public final class CopilotSessions {

  public static final String CHAT_VIEW_ID = "com.microsoft.copilot.eclipse.ui.chat.ChatView";

  private static final String TOPIC_CONVERSATION_SELECTED =
      "com/microsoft/copilot/eclipse/CHAT/HISTORY_CONVERSATION_SELECTED";
  private static final String TOPIC_CONVERSATION_TITLE_UPDATED =
      "com/microsoft/copilot/eclipse/CHAT/CONVERSATION_TITLE_UPDATED";
  private static final String TOPIC_NEW_CONVERSATION =
      "com/microsoft/copilot/eclipse/CHAT/NEW_CONVERSATION";
  private static final String TOPIC_HIDE_CHAT_HISTORY =
      "com/microsoft/copilot/eclipse/CHAT/HIDE_CHAT_HISTORY";

  private static final String[] SESSION_CHANGE_TOPICS = {
      TOPIC_CONVERSATION_SELECTED,
      TOPIC_CONVERSATION_TITLE_UPDATED,
      TOPIC_NEW_CONVERSATION,
      "com/microsoft/copilot/eclipse/CHAT/ON_SEND",
      "com/microsoft/copilot/eclipse/CHAT/MESSAGE_SEND",
      // Fires once Copilot has finished starting up and knows the sign-in state, which is when
      // sessions become readable at all.
      "com/microsoft/copilot/eclipse/AUTH/STATUS_CHANGED",
  };

  private CopilotSessions() {
  }

  /** Removes a listener registered through {@link #onSessionsChanged(Runnable)}. */
  public interface Subscription {
    void dispose();
  }

  /**
   * Returns a message explaining why sessions cannot be used right now, or {@code null} when they
   * are available.
   */
  public static String unavailableReason() {
    if (CopilotUi.getPlugin() == null) {
      return "GitHub Copilot for Eclipse is not active in this workbench.";
    }
    if (chatServiceManager() == null || persistenceManager() == null) {
      return "GitHub Copilot is still starting up. Try again in a moment.";
    }
    AuthStatusManager auth = authStatusManager();
    if (auth != null && !auth.isSignedIn()) {
      return "Sign in to GitHub Copilot to use chat sessions.";
    }
    return null;
  }

  public static boolean isAvailable() {
    return unavailableReason() == null;
  }

  /**
   * All persisted Copilot conversations, most recently active first. Empty when Copilot is not ready
   * or nothing has been persisted yet.
   */
  public static List<SessionInfo> listSessions() {
    ConversationPersistenceManager persistence = persistenceManager();
    if (persistence == null) {
      return List.of();
    }
    try {
      List<SessionInfo> sessions = new ArrayList<>();
      for (ConversationXmlData conversation : persistence.listConversations()) {
        if (conversation != null) {
          sessions.add(toSessionInfo(conversation));
        }
      }
      return SessionList.sortedByMostRecent(sessions);
    } catch (RuntimeException | LinkageError e) {
      CopilotExtensionsPlugin.logError("Failed to list Copilot chat sessions", e);
      return List.of();
    }
  }

  /**
   * The conversation currently shown in the chat view, or {@code null} when the view is closed or
   * showing an unsaved conversation.
   */
  public static String activeSessionId() {
    ChatView chatView = findChatView();
    if (chatView == null) {
      return null;
    }
    String conversationId = chatView.getConversationId();
    return isBlank(conversationId) ? null : conversationId;
  }

  /**
   * Shows {@code session} in the chat view, opening the view if needed.
   *
   * @return whether the switch was requested
   */
  public static boolean switchTo(SessionInfo session) {
    if (session == null || isBlank(session.getId()) || !isAvailable()) {
      return false;
    }
    ChatView chatView = openChatView();
    if (chatView == null) {
      return false;
    }
    if (session.getId().equals(activeSessionId())) {
      chatView.setFocus();
      return true;
    }
    IEventBroker broker = eventBroker();
    if (broker == null) {
      return false;
    }
    // The chat view loads the conversation, restores its turns and hides the history panel itself.
    broker.post(TOPIC_CONVERSATION_SELECTED, toConversationData(session));
    return true;
  }

  /**
   * Starts an empty session in the chat view. Prompts first when the current session has unresolved
   * file changes.
   */
  public static boolean createNewSession() {
    if (!isAvailable()) {
      return false;
    }
    if (!ConversationUtils.confirmEndChat()) {
      return false;
    }
    ChatView chatView = openChatView();
    if (chatView == null) {
      return false;
    }
    chatView.onNewConversation();
    IEventBroker broker = eventBroker();
    if (broker != null) {
      broker.post(TOPIC_NEW_CONVERSATION, null);
      broker.post(TOPIC_HIDE_CHAT_HISTORY, null);
      broker.post(TOPIC_CONVERSATION_TITLE_UPDATED, Messages.chat_topBanner_defaultChatTitle);
    }
    return true;
  }

  /** Renames a session and updates the chat view title when that session is the active one. */
  public static CompletableFuture<Void> rename(SessionInfo session, String newTitle) {
    ConversationPersistenceManager persistence = persistenceManager();
    if (persistence == null || session == null || isBlank(newTitle)) {
      return CompletableFuture.completedFuture(null);
    }
    String sessionId = session.getId();
    return persistence.updateConversationTitle(sessionId, newTitle).thenRun(() -> {
      IEventBroker broker = eventBroker();
      if (broker != null && sessionId.equals(activeSessionId())) {
        broker.post(TOPIC_CONVERSATION_TITLE_UPDATED, newTitle);
      }
    });
  }

  /** Deletes a session from Copilot's conversation store. */
  public static CompletableFuture<Void> delete(SessionInfo session) {
    ConversationPersistenceManager persistence = persistenceManager();
    if (persistence == null || session == null || isBlank(session.getId())) {
      return CompletableFuture.completedFuture(null);
    }
    return persistence.removeConversationById(session.getId());
  }

  /**
   * Returns the session {@code offset} positions away from the active one, wrapping around. Used by
   * the next/previous session commands.
   */
  public static SessionInfo relativeSession(int offset) {
    return SessionList.relativeTo(listSessions(), activeSessionId(), offset);
  }

  /** Display title for a session, using Copilot's placeholder for untitled ones. */
  public static String title(SessionInfo session) {
    if (session == null || session.isUntitled()) {
      return Messages.chat_topBanner_chatHistoryItem_untitledConversation_placeholder;
    }
    return session.getRawTitle().trim();
  }

  /** Human readable age of a session's most recent message. */
  public static String lastActivityLabel(SessionInfo session) {
    if (session == null) {
      return "";
    }
    return SessionList.relativeTime(session.lastActivity(), Instant.now());
  }

  /**
   * Notifies {@code listener} when Copilot activity may have changed the session list. The listener
   * may be called on any thread.
   */
  public static Subscription onSessionsChanged(Runnable listener) {
    IEventBroker broker = eventBroker();
    if (broker == null) {
      return () -> {
      };
    }
    EventHandler handler = event -> listener.run();
    for (String topic : SESSION_CHANGE_TOPICS) {
      broker.subscribe(topic, handler);
    }
    return () -> broker.unsubscribe(handler);
  }

  private static SessionInfo toSessionInfo(ConversationXmlData conversation) {
    return new SessionInfo(conversation.getConversationId(), conversation.getTitle(),
        conversation.getCreationDate(), conversation.getLastMessageDate());
  }

  /**
   * The chat view only reads the id and title off the conversation it is asked to show, so a summary
   * rebuilt from a {@link SessionInfo} is enough to drive the switch.
   */
  private static ConversationXmlData toConversationData(SessionInfo session) {
    return new ConversationXmlData(session.getId(), session.getRawTitle(), session.getCreatedAt(),
        session.getLastMessageAt());
  }

  private static ChatView openChatView() {
    IWorkbenchPage page = activePage();
    if (page == null) {
      return null;
    }
    try {
      IViewPart part = page.showView(CHAT_VIEW_ID);
      return part instanceof ChatView ? (ChatView) part : null;
    } catch (PartInitException e) {
      CopilotExtensionsPlugin.logError("Failed to open the Copilot chat view", e);
      return null;
    }
  }

  private static ChatView findChatView() {
    IWorkbenchPage page = activePage();
    if (page == null) {
      return null;
    }
    IViewPart part = page.findView(CHAT_VIEW_ID);
    return part instanceof ChatView ? (ChatView) part : null;
  }

  private static IWorkbenchPage activePage() {
    if (!PlatformUI.isWorkbenchRunning()) {
      return null;
    }
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    return window == null ? null : window.getActivePage();
  }

  private static IEventBroker eventBroker() {
    if (!PlatformUI.isWorkbenchRunning()) {
      return null;
    }
    IWorkbench workbench = PlatformUI.getWorkbench();
    return workbench.getService(IEventBroker.class);
  }

  private static ChatServiceManager chatServiceManager() {
    CopilotUi copilotUi = CopilotUi.getPlugin();
    return copilotUi == null ? null : copilotUi.getChatServiceManager();
  }

  private static ConversationPersistenceManager persistenceManager() {
    ChatServiceManager manager = chatServiceManager();
    return manager == null ? null : manager.getPersistenceManager();
  }

  private static AuthStatusManager authStatusManager() {
    ChatServiceManager manager = chatServiceManager();
    return manager == null ? null : manager.getAuthStatusManager();
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}

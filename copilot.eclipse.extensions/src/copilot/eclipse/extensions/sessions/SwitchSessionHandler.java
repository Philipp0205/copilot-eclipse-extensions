package copilot.eclipse.extensions.sessions;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Opens a filtered list of saved sessions and shows the picked one in the chat view.
 */
public class SwitchSessionHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);
    String unavailableReason = CopilotSessions.unavailableReason();
    if (unavailableReason != null) {
      MessageDialog.openInformation(shell, "Copilot Sessions", unavailableReason);
      return null;
    }
    List<SessionInfo> sessions = CopilotSessions.listSessions();
    if (sessions.isEmpty()) {
      MessageDialog.openInformation(shell, "Copilot Sessions",
          "No saved sessions yet. Send a message in Copilot chat to start one.");
      return null;
    }

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider() {
      @Override
      public String getText(Object element) {
        return CopilotSessions.title((SessionInfo) element);
      }
    });
    dialog.setTitle("Switch Copilot Session");
    dialog.setMessage("Select a session (? = any character, * = any string):");
    dialog.setElements(sessions.toArray());
    dialog.setMultipleSelection(false);
    dialog.setInitialSelections(currentSession(sessions));
    if (dialog.open() != ElementListSelectionDialog.OK) {
      return null;
    }
    Object result = dialog.getFirstResult();
    if (result instanceof SessionInfo) {
      CopilotSessions.switchTo((SessionInfo) result);
    }
    return null;
  }

  private static Object[] currentSession(List<SessionInfo> sessions) {
    String activeSessionId = CopilotSessions.activeSessionId();
    if (activeSessionId == null) {
      return new Object[0];
    }
    return sessions.stream()
        .filter(session -> activeSessionId.equals(session.getId()))
        .toArray();
  }
}

package copilot.eclipse.extensions.sessions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Starts an empty Copilot chat session.
 */
public class NewSessionHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String unavailableReason = CopilotSessions.unavailableReason();
    if (unavailableReason != null) {
      MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Copilot Sessions",
          unavailableReason);
      return null;
    }
    CopilotSessions.createNewSession();
    return null;
  }
}

package copilot.eclipse.extensions.sessions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Moves the chat view to the session next to the current one, wrapping around at the ends.
 */
public abstract class CycleSessionHandler extends AbstractHandler {

  /** Steps forward through the session list, from most to least recently active. */
  public static class Next extends CycleSessionHandler {
    public Next() {
      super(1);
    }
  }

  /** Steps backward through the session list. */
  public static class Previous extends CycleSessionHandler {
    public Previous() {
      super(-1);
    }
  }

  private final int offset;

  CycleSessionHandler(int offset) {
    this.offset = offset;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String unavailableReason = CopilotSessions.unavailableReason();
    if (unavailableReason != null) {
      MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Copilot Sessions",
          unavailableReason);
      return null;
    }
    SessionInfo target = CopilotSessions.relativeSession(offset);
    if (target != null) {
      CopilotSessions.switchTo(target);
    }
    return null;
  }
}

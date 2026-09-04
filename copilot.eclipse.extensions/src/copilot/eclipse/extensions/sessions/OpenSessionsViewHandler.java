package copilot.eclipse.extensions.sessions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import copilot.eclipse.extensions.CopilotExtensionsPlugin;

/**
 * Shows the Copilot Sessions view.
 */
public class OpenSessionsViewHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
    if (window == null) {
      return null;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return null;
    }
    try {
      page.showView(SessionsView.VIEW_ID);
    } catch (PartInitException e) {
      CopilotExtensionsPlugin.logError("Failed to open the Copilot Sessions view", e);
    }
    return null;
  }
}

package copilot.eclipse.extensions.sessions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * Lists the persisted Copilot chat sessions and switches the chat view between them.
 */
public class SessionsView extends ViewPart {

  public static final String VIEW_ID = "copilot.eclipse.extensions.views.SessionsView";

  private TableViewer viewer;
  private Label statusLabel;
  private Display display;
  private CopilotSessions.Subscription subscription;

  /** Resolved once per refresh so a repaint does not have to look up the chat view per row. */
  private String activeSessionId;

  private Action switchAction;
  private Action newSessionAction;
  private Action renameAction;
  private Action deleteAction;
  private Action refreshAction;

  @Override
  public void createPartControl(Composite parent) {
    display = parent.getDisplay();
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 2;
    container.setLayout(layout);

    statusLabel = new Label(container, SWT.WRAP);
    statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    createViewer(container);
    createActions();
    fillActionBars();
    createContextMenu();

    subscription = CopilotSessions.onSessionsChanged(this::refreshAsync);
    refresh();
  }

  private void createViewer(Composite container) {
    viewer = new TableViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    Table table = viewer.getTable();
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    table.setHeaderVisible(true);
    table.setLinesVisible(false);

    TableColumn sessionColumn = new TableColumn(table, SWT.LEFT);
    sessionColumn.setText("Session");
    TableColumn activityColumn = new TableColumn(table, SWT.LEFT);
    activityColumn.setText("Last activity");

    TableLayout tableLayout = new TableLayout();
    tableLayout.addColumnData(new ColumnWeightData(70, 180, true));
    tableLayout.addColumnData(new ColumnWeightData(30, 110, true));
    table.setLayout(tableLayout);

    viewer.setContentProvider(ArrayContentProvider.getInstance());
    viewer.setLabelProvider(new SessionLabelProvider());
    viewer.addDoubleClickListener(event -> switchToSelection());
    viewer.addSelectionChangedListener(event -> updateActionState());
    getSite().setSelectionProvider(viewer);
  }

  private void createActions() {
    ISharedImages images = PlatformUI.getWorkbench().getSharedImages();

    switchAction = new Action("Switch to Session") {
      @Override
      public void run() {
        switchToSelection();
      }
    };
    switchAction.setToolTipText("Show the selected session in the Copilot chat view");

    newSessionAction = new Action("New Session") {
      @Override
      public void run() {
        if (!reportIfUnavailable() && CopilotSessions.createNewSession()) {
          refreshAsync();
        }
      }
    };
    newSessionAction.setToolTipText("Start an empty Copilot chat session");
    newSessionAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));

    renameAction = new Action("Rename...") {
      @Override
      public void run() {
        renameSelection();
      }
    };

    deleteAction = new Action("Delete") {
      @Override
      public void run() {
        deleteSelection();
      }
    };
    deleteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
    deleteAction.setDisabledImageDescriptor(
        images.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED));

    refreshAction = new Action("Refresh") {
      @Override
      public void run() {
        refresh();
      }
    };
    refreshAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
  }

  private void fillActionBars() {
    IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
    toolBar.add(newSessionAction);
    toolBar.add(deleteAction);
    toolBar.add(new Separator());
    toolBar.add(refreshAction);

    IMenuManager viewMenu = getViewSite().getActionBars().getMenuManager();
    viewMenu.add(newSessionAction);
    viewMenu.add(switchAction);
    viewMenu.add(renameAction);
    viewMenu.add(deleteAction);
    viewMenu.add(new Separator());
    viewMenu.add(refreshAction);
  }

  private void createContextMenu() {
    MenuManager menuManager = new MenuManager();
    menuManager.add(switchAction);
    menuManager.add(newSessionAction);
    menuManager.add(new Separator());
    menuManager.add(renameAction);
    menuManager.add(deleteAction);
    menuManager.add(new Separator());
    menuManager.add(refreshAction);
    Menu menu = menuManager.createContextMenu(viewer.getTable());
    viewer.getTable().setMenu(menu);
  }

  /** Reloads the session list from Copilot's conversation store. */
  public void refresh() {
    if (isDisposed()) {
      return;
    }
    activeSessionId = CopilotSessions.activeSessionId();
    String unavailableReason = CopilotSessions.unavailableReason();
    List<SessionInfo> sessions =
        unavailableReason == null ? CopilotSessions.listSessions() : List.<SessionInfo>of();
    statusLabel.setText(unavailableReason == null ? describe(sessions.size()) : unavailableReason);
    viewer.setInput(sessions.toArray(new SessionInfo[0]));
    updateActionState();
    statusLabel.getParent().layout();
  }

  /** Refreshes on the UI thread; safe to call from Copilot's event delivery threads. */
  private void refreshAsync() {
    if (display == null || display.isDisposed()) {
      return;
    }
    display.asyncExec(() -> {
      if (!isDisposed()) {
        refresh();
      }
    });
  }

  private boolean isDisposed() {
    return viewer == null || viewer.getTable().isDisposed();
  }

  private static String describe(int sessionCount) {
    if (sessionCount == 0) {
      return "No saved sessions yet. Send a message in Copilot chat to start one.";
    }
    return sessionCount == 1 ? "1 session" : sessionCount + " sessions";
  }

  private void switchToSelection() {
    SessionInfo session = selectedSession();
    if (session == null || reportIfUnavailable()) {
      return;
    }
    CopilotSessions.switchTo(session);
    refreshAsync();
  }

  private void renameSelection() {
    SessionInfo session = selectedSession();
    if (session == null || reportIfUnavailable()) {
      return;
    }
    IInputValidator validator = value ->
        value == null || value.trim().isEmpty() ? "Enter a session name." : null;
    String currentName = session.isUntitled() ? "" : CopilotSessions.title(session);
    InputDialog dialog = new InputDialog(getSite().getShell(), "Rename Session",
        "Session name:", currentName, validator);
    if (dialog.open() != InputDialog.OK) {
      return;
    }
    CopilotSessions.rename(session, dialog.getValue().trim())
        .whenComplete((ignored, error) -> refreshAsync());
  }

  private void deleteSelection() {
    SessionInfo session = selectedSession();
    if (session == null || reportIfUnavailable()) {
      return;
    }
    boolean confirmed = MessageDialog.openConfirm(getSite().getShell(), "Delete Session",
        "Delete the session \"" + CopilotSessions.title(session) + "\"? This cannot be undone.");
    if (!confirmed) {
      return;
    }
    boolean wasActive = session.getId().equals(CopilotSessions.activeSessionId());
    CopilotSessions.delete(session).whenComplete((ignored, error) -> {
      if (wasActive && display != null && !display.isDisposed()) {
        // The chat view is still showing the conversation that no longer exists.
        display.asyncExec(() -> {
          if (!isDisposed()) {
            CopilotSessions.createNewSession();
          }
        });
      }
      refreshAsync();
    });
  }

  private boolean reportIfUnavailable() {
    String unavailableReason = CopilotSessions.unavailableReason();
    if (unavailableReason == null) {
      return false;
    }
    statusLabel.setText(unavailableReason);
    MessageDialog.openInformation(getSite().getShell(), "Copilot Sessions", unavailableReason);
    return true;
  }

  private SessionInfo selectedSession() {
    IStructuredSelection selection = viewer.getStructuredSelection();
    Object first = selection.getFirstElement();
    return first instanceof SessionInfo ? (SessionInfo) first : null;
  }

  private void updateActionState() {
    boolean hasSelection = selectedSession() != null;
    switchAction.setEnabled(hasSelection);
    renameAction.setEnabled(hasSelection);
    deleteAction.setEnabled(hasSelection);
  }

  @Override
  public void setFocus() {
    if (viewer != null && !viewer.getTable().isDisposed()) {
      viewer.getTable().setFocus();
    }
  }

  @Override
  public void dispose() {
    if (subscription != null) {
      subscription.dispose();
      subscription = null;
    }
    super.dispose();
  }

  private final class SessionLabelProvider extends LabelProvider
      implements ITableLabelProvider, ITableFontProvider {

    @Override
    public String getColumnText(Object element, int columnIndex) {
      if (!(element instanceof SessionInfo)) {
        return "";
      }
      SessionInfo session = (SessionInfo) element;
      if (columnIndex == 0) {
        String title = CopilotSessions.title(session);
        return isActive(session) ? title + "  (current)" : title;
      }
      return CopilotSessions.lastActivityLabel(session);
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @Override
    public Font getFont(Object element, int columnIndex) {
      if (element instanceof SessionInfo && isActive((SessionInfo) element)) {
        return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
      }
      return null;
    }

    private boolean isActive(SessionInfo session) {
      return session.getId() != null && session.getId().equals(activeSessionId);
    }
  }
}

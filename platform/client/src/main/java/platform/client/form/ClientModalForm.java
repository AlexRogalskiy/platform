package platform.client.form;

import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ClientModalForm extends JDialog {

    protected ClientFormController currentForm;
    protected final RemoteFormInterface remoteForm;
    private final boolean newSession;
    private boolean activatedFirstTime = true;

    public ClientModalForm(Component owner, final RemoteFormInterface remoteForm, boolean newSession) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность
        this.remoteForm = remoteForm;
        this.newSession = newSession;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        createListeners();

        updateCurrentForm();
    }

    protected void createListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                if (activatedFirstTime && currentForm != null) {
                    activatedFirstTime = false;
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm.getComponent());
                }
            }
        });
    }

    void updateCurrentForm() throws IOException, ClassNotFoundException {
        if (currentForm != null) {
            remove(currentForm.getComponent());
        }

        currentForm = createFormController();

        add(currentForm.getComponent(), BorderLayout.CENTER);
        setTitle(currentForm.getCaption());

        validate();
    }

    protected ClientFormController createFormController() throws IOException, ClassNotFoundException {
        return new ClientFormController(remoteForm, null) {
            @Override
            public boolean isDialogMode() {
                return true;
            }

            @Override
            public boolean isReadOnlyMode() {
                return super.isReadOnlyMode();
            }

            @Override
            public void okPressed() {
                if (okPressed(newSession)) {
                    hideDialog();
                }
            }

            @Override
            boolean nullPressed() {
                hideDialog();
                return true;
            }

            @Override
            boolean closePressed() {
                hideDialog();
                return true;
            }
        };
    }

    private void hideDialog() {
        setVisible(false);
    }

    public void showDialog() {
        setDefaultSize();
        setLocationRelativeTo(null);
        setVisible(true);
        dispose();
        closed();
    }

    public void closed() {
        if (currentForm != null) {
            currentForm.closed();
            currentForm = null;
        }
    }

    public Dimension calculatePreferredSize(boolean undecorated) {
        Dimension preferredSize = currentForm.calculatePreferredSize();

        // так как у нас есть только preferredSize самого contentPane, а нам нужен у JDialog
        // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
        if (undecorated) {
            preferredSize.width += 10;
            preferredSize.height += 40;
        } else {
            preferredSize.width += 20;
            preferredSize.height += 80;
        }

        preferredSize.height += 15; // под отборы

        return preferredSize;
    }

    public void setDefaultSize() {
        setSize(SwingUtils.clipToScreen(calculatePreferredSize(isUndecorated())));
    }
}

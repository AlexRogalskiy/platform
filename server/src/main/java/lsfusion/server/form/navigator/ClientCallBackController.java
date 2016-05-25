package lsfusion.server.form.navigator;

import lsfusion.interop.remote.*;
import lsfusion.server.ServerLoggers;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.List;

public class ClientCallBackController extends RemoteObject implements ClientCallBackInterface, Unreferenced {
    public interface UsageTracker {
        void used();
    }

    private final List<LifecycleMessage> messages = new ArrayList<>();
    private final UsageTracker usageTracker;
    private Boolean deniedRestart = null;

    public ClientCallBackController(int port, String caption, UsageTracker usageTracker) throws RemoteException {
        super(port, true);
        this.caption = caption;
        this.usageTracker = usageTracker;
    }

    public synchronized void disconnect() {
        addMessage(new ClientCallbackMessage(CallbackMessage.DISCONNECTED));
    }

    public synchronized void notifyServerRestart() {
        deniedRestart = false;
        addMessage(new ClientCallbackMessage(CallbackMessage.SERVER_RESTARTING));
    }

    public synchronized void shutdownClient(boolean restart) {
        addMessage(new ClientCallbackMessage(restart ? CallbackMessage.CLIENT_RESTART : CallbackMessage.CLIENT_SHUTDOWN));
    }

    public synchronized void notifyServerRestartCanceled() {
        deniedRestart = null;
    }

    public synchronized void denyRestart() {
        deniedRestart = true;
    }

    public synchronized boolean isRestartAllowed() {
        //если не спрашивали, либо если отказался
        return deniedRestart != null && !deniedRestart;
    }

    public synchronized void pushMessage(Integer idNotification) {
        addMessage(new PushMessage(idNotification));
    }

    public synchronized void addMessage(LifecycleMessage message) {
        messages.add(message);
    }

    public synchronized List<LifecycleMessage> pullMessages() {
        if (usageTracker != null) {
            usageTracker.used();
        }
        ArrayList result = new ArrayList(messages);
        messages.clear();
        return result.isEmpty() ? null : result;
    }

    private String caption;
    public void unreferenced() {
        ServerLoggers.remoteLifeLog("CALLBACK UNREFERENCED : " + caption);
    }
}

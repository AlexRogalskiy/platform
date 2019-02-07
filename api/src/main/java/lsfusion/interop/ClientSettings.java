package lsfusion.interop;

import java.io.Serializable;

public class ClientSettings implements Serializable {
    public LocalePreferences localePreferences;
    public Integer fontSize;
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean useRequestTimeout;
    public boolean configurationAccessAllowed;

    public ClientSettings(LocalePreferences localePreferences, Integer fontSize, boolean busyDialog, long busyDialogTimeout, boolean useRequestTimeout,
                          boolean configurationAccessAllowed) {
        this.localePreferences = localePreferences;
        this.fontSize = fontSize;
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
        this.configurationAccessAllowed = configurationAccessAllowed;
    }
}
package fr.arthurbr02.deploymanager.util;

public final class AuditConstants {

    private AuditConstants() {}

    // Entity names
    public static final String ENTITY_HOST = "Host";
    public static final String ENTITY_USER = "User";
    public static final String ENTITY_TERMINAL = "Terminal";
    public static final String ENTITY_APP_CONFIG = "AppConfig";
    public static final String ENTITY_USER_HOST_PERMISSION = "UserHostPermission";

    // Actions
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_TERMINAL_CONNECT = "TERMINAL_CONNECT";
    public static final String ACTION_TERMINAL_DISCONNECT = "TERMINAL_DISCONNECT";
    public static final String ACTION_TERMINAL_COMMAND = "TERMINAL_COMMAND";
    public static final String ACTION_TERMINAL_COMMAND_BLOCKED = "TERMINAL_COMMAND_BLOCKED";

    // MFA
    public static final String ACTION_MFA_CODE_SENT = "MFA_CODE_SENT";
    public static final String ACTION_MFA_VERIFIED = "MFA_VERIFIED";
    public static final String ACTION_MFA_FAILED = "MFA_FAILED";
    public static final String ACTION_MFA_BLOCKED = "MFA_BLOCKED";
    public static final String ACTION_MFA_DEVICE_TRUSTED = "MFA_DEVICE_TRUSTED";
    public static final String ACTION_MFA_DEVICE_REVOKED = "MFA_DEVICE_REVOKED";
}

package com.zelix.klassmaster.config;

public abstract class WizardAppSettings extends PersistedStringList {
    // Preserve the existing serialized identity while recovering non-persistent member names.
    private static final long serialVersionUID = -451450663869808852L;

    public transient int applicationType;
    /** mainClassName; the field name is retained for serialized settings compatibility. */
    public String f;
    /** mainClassApplication; the field name is retained for serialized settings compatibility. */
    public boolean c;
    /** mainClass; the field name is retained for serialized settings compatibility. */
    public boolean d = false;

    public boolean isMainClassApplication() {
        return this.c;
    }

    public String getMainClassName() {
        return this.f;
    }

    public int getApplicationType() {
        return this.applicationType;
    }

    public void setMainClass(boolean mainClass, String mainClassName, boolean mainClassApplication) {
        this.d = mainClass;
        if (this.d) {
            this.f = mainClassName;
            this.c = mainClassApplication;
        } else {
            this.f = null;
        }
    }

    public void setApplicationType(int applicationTypeArgument) {
        this.applicationType = applicationTypeArgument;
    }

    public boolean hasMainClass() {
        return this.d;
    }
}

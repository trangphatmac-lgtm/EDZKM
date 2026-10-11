package com.zelix.klassmaster.config;

import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.util.SettingsObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.ObjectInputStream;

public class ExclusionWizardSettings extends WizardAppSettings implements Serializable {
    /** packageSelected; the field name is retained for serialized settings compatibility. */
    public boolean g = false;
    /** selectedPackageName; the field name is retained for serialized settings compatibility. */
    public String h = null;

    public String getSelectedPackageName() {
        return this.h;
    }

    public boolean isPackageSelected() {
        return this.g;
    }

    public static ExclusionWizardSettings loadFromFile(final String parent) {
        final File file = new File(parent, "ZKM_EX.ser");
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new SettingsObjectInputStream(new FileInputStream(file));
                final ExclusionWizardSettings exclusionWizardSettings = (ExclusionWizardSettings) objectInputStream.readObject();
                if (exclusionWizardSettings.l == null) {
                    exclusionWizardSettings.l = new LinkedHashMap();
                    exclusionWizardSettings.saved = true;
                } else {
                    exclusionWizardSettings.saved = true;
                }
                final ExclusionWizardSettings exclusionWizardSettings2 = exclusionWizardSettings;
                try {
                    objectInputStream.close();
                } catch (final IOException ioException) {
                }
                return exclusionWizardSettings2;
            } catch (final IOException ioExceptionValue) {
            } catch (final ClassNotFoundException classNotFoundException) {
            } catch (final ClassCastException classCastException) {
            } catch (final AssertionFailedException assertionFailedException) {
                throw assertionFailedException;
            } catch (final Throwable throwable) {
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (final IOException ioExceptionValue2) {
                    }
                }
            }
        }
        return new ExclusionWizardSettings();
    }

    public boolean isExactPackageName(String string) {
        return string != null && string.length() > 0 && string.indexOf("*") == -1 && string.indexOf("^") == -1 && string.charAt(string.length() - 1) == '.';
    }

    public void setPackageSelection(boolean packageSelected, String selectedPackageName) {
        this.g = packageSelected;
        this.h = selectedPackageName;
    }

    @Override
    public void setEntries(List list1) {
        super.l.clear();
        int ba = 0;
        String selectedPackageName = null;

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            String selectedPackageNameValue = (String) list1.get(elementIndex);
            super.l.put(selectedPackageNameValue, selectedPackageNameValue);
            if (this.isExactPackageName(selectedPackageNameValue)) {
                if (++ba == 1) {
                    selectedPackageName = selectedPackageNameValue;
                }
            }
        }

        boolean packageSelected;
        if (super.f != null) {
            if (super.f.length() > 0) {
                if (super.d) {
                    if (!super.l.containsKey(super.f)) {
                        super.d = false;
                        packageSelected = this.g;
                    } else {
                        packageSelected = this.g;
                    }
                } else if (super.l.containsKey(super.f)) {
                    super.d = true;
                    packageSelected = this.g;
                } else {
                    packageSelected = this.g;
                }
            } else {
                packageSelected = this.g;
            }
        } else {
            packageSelected = this.g;
        }

        if (packageSelected) {
            if (!super.l.containsKey(this.h)) {
                this.g = false;
                packageSelected = this.g;
            } else {
                packageSelected = this.g;
            }
        } else if (super.l.containsKey(this.h)) {
            this.g = true;
            packageSelected = this.g;
        } else {
            packageSelected = this.g;
        }

        if (!packageSelected && ba > 0) {
            this.g = true;
            this.h = selectedPackageName;
        }
    }
}

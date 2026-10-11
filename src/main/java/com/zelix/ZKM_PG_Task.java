package com.zelix;

import com.zelix.klassmaster.ant.ProGuardTaskPath;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.engine.ZkmApiBase;
import com.zelix.klassmaster.util.ObservableHolder;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class ZKM_PG_Task extends Task {
    public File configurationFile;
    public StringBuilder proGuardOptions = new StringBuilder();

    public void addText(String string) {
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
        }

        this.proGuardOptions.append(this.getProject().replaceProperties(string));
    }

    public void setRenamesourcefileattribute(String renamesourcefileattributeArgument) {
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
        }

        this.proGuardOptions.append("-renamesourcefileattribute");
        if (renamesourcefileattributeArgument != null) {
            this.proGuardOptions.append(' ');
            this.proGuardOptions.append('"');
            this.proGuardOptions.append(renamesourcefileattributeArgument);
            this.proGuardOptions.append('"');
        }
    }

    public void setFlattenpackagehierarchy(String flattenpackagehierarchyArgument) {
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
        }

        this.proGuardOptions.append("-flattenpackagehierarchy");
        if (flattenpackagehierarchyArgument != null) {
            this.proGuardOptions.append(' ');
            this.proGuardOptions.append('"');
            this.proGuardOptions.append(flattenpackagehierarchyArgument);
            this.proGuardOptions.append('"');
        }
    }

    public void setMergeinterfacesaggressively(boolean mergeinterfacesaggressivelyArgument) {
        if (mergeinterfacesaggressivelyArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-mergeinterfacesaggressively");
        }
    }

    public void setOptimizationpasses(int optimizationpassesArgument) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-optimizationpasses");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append(optimizationpassesArgument);
    }

    public void setConfiguration(File file1) {
        this.configurationFile = file1;
    }

    public void setPackageobfuscationdictionary(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-packageobfuscationdictionary");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setVerbose(boolean verboseArgument) {
        if (verboseArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-verbose");
        }
    }

    public void setMicroedition(boolean microeditionArgument) {
        if (!microeditionArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-microedition");
        }
    }

    public void addConfiguredOutjar(ProGuardTaskPath proGuardTaskPath) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-outjars");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append(proGuardTaskPath.getFileName());
    }

    public void setPrintseeds(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-printseeds");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setNote(boolean note) {
        if (!note) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontnote");
        }
    }

    public void setWarn(boolean warn) {
        if (!warn) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontwarn");
        }
    }

    public void setDump(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-dump");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setPreverify(boolean preverify) {
        if (!preverify) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontpreverify");
        }
    }

    public void setAllowaccessmodification(boolean allowaccessmodificationArgument) {
        if (allowaccessmodificationArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-allowaccessmodification");
        }
    }

    public void setSkipnonpubliclibraryclassmembers(boolean skipnonpubliclibraryclassmembers) {
        if (!skipnonpubliclibraryclassmembers) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontskipnonpubliclibraryclasses");
        }
    }

    public void setDefaultpackage(String defaultpackage) {
        this.setRepackageclasses(defaultpackage);
    }

    public void setObfuscate(boolean obfuscate) {
        if (!obfuscate) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontobfuscate");
        }
    }

    public void setObfuscationdictionary(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-obfuscationdictionary");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setUsemixedcaseclassnames(boolean usemixedcaseclassnames) {
        if (!usemixedcaseclassnames) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontusemixedcaseclassnames");
        }
    }

    public void setPrintconfiguration(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-printconfiguration");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setForceprocessing(boolean forceprocessingArgument) {
        if (forceprocessingArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-forceprocessing");
        }
    }

    public void setPrintmapping(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-printmapping");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setTarget(String targetArgument) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-target");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append(targetArgument);
    }

    public void setOptimize(boolean optimize) {
        if (!optimize) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontoptimize");
        }
    }

    public void addConfiguredInjar(ProGuardTaskPath proGuardTaskPath) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-injars");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append(proGuardTaskPath.getFileName());
    }

    public void setSkipnonpubliclibraryclasses(boolean skipnonpubliclibraryclassesArgument) {
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
        }

        if (skipnonpubliclibraryclassesArgument) {
            this.proGuardOptions.append("-skipnonpubliclibraryclasses");
        } else {
            this.proGuardOptions.append("-dontskipnonpubliclibraryclasses");
        }
    }

    public void setShrink(boolean shrink) {
        if (!shrink) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-dontshrink");
        }
    }

    public void setApplymapping(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-applymapping");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setIgnorewarnings(boolean ignorewarningsArgument) {
        if (ignorewarningsArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-ignorewarnings");
        }
    }

    public void execute() {
        if (this.configurationFile == null && this.proGuardOptions.length() == 0) {
            throw new BuildException("Missing or empty ProGuard configuration");
        }

        try {
            String absolutePathValue2;
            Project project;
            if (this.proGuardOptions.length() > 0) {
                String text;
                if (this.configurationFile == null) {
                    text = this.proGuardOptions.toString();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.proGuardOptions.toString());
                    stringBuilder.append(ZkmFileUtils.LINE_SEPARATOR);
                    stringBuilder.append(ZkmFileUtils.readFileAsString(this.configurationFile));
                    text = stringBuilder.toString();
                }

                ObservableHolder observableHolder = new ObservableHolder();
                String absolutePath = ZkmFileUtils.writeStringToTempFile(text, observableHolder).getAbsolutePath();
                if (!observableHolder.isValueNull()) {
                    throw new BuildException((String) observableHolder.getValue());
                }

                absolutePathValue2 = absolutePath;
                project = this.getProject();
            } else {
                String absolutePathValue = this.configurationFile.getAbsolutePath();
                absolutePathValue2 = absolutePathValue;
                project = this.getProject();
            }

            ZkmApiBase.run(absolutePathValue2, project.getProperties());
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BuildException(exception.toString());
        }
    }

    public void setUseuniqueclassmembernames(boolean useuniqueclassmembernamesArgument) {
        if (useuniqueclassmembernamesArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-useuniqueclassmembernames");
        }
    }

    public void setKeepparameternames(boolean keepparameternamesArgument) {
        if (keepparameternamesArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-keepparameternames");
        }
    }

    public void setPrintusage(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-printusage");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setClassobfuscationdictionary(File file1) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-classobfuscationdictionary");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append('"');
        this.proGuardOptions.append(file1.getAbsolutePath());
        this.proGuardOptions.append('"');
    }

    public void setRepackageclasses(String repackageclassesArgument) {
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
        }

        this.proGuardOptions.append("-repackageclasses");
        if (repackageclassesArgument != null) {
            this.proGuardOptions.append(' ');
            this.proGuardOptions.append('"');
            this.proGuardOptions.append(repackageclassesArgument);
            this.proGuardOptions.append('"');
        }
    }

    public void addConfiguredLibraryjar(ProGuardTaskPath proGuardTaskPath) {
        StringBuilder stringBuilder;
        if (this.proGuardOptions.length() > 0) {
            this.proGuardOptions.append(' ');
            stringBuilder = this.proGuardOptions;
        } else {
            stringBuilder = this.proGuardOptions;
        }

        stringBuilder.append("-libraryjars");
        this.proGuardOptions.append(' ');
        this.proGuardOptions.append(proGuardTaskPath.getFileName());
    }

    public void setAndroid(boolean androidArgument) {
        if (!androidArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-android");
        }
    }

    public void setOverloadaggressively(boolean overloadaggressivelyArgument) {
        if (overloadaggressivelyArgument) {
            if (this.proGuardOptions.length() > 0) {
                this.proGuardOptions.append(' ');
            }

            this.proGuardOptions.append("-overloadaggressively");
        }
    }
}

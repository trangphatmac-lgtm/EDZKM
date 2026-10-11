package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.xml.ResourcePathTranslator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

public class ManifestEntrySection extends AbstractManifestProcessor {
    public String getSectionName() {
        return this.getUnwrappedHeaderValue("Name");
    }

    @Override
    public void writeSection(
            PrintWriter printWriter,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            Object object,
            String string,
            boolean bl,
            MessageReporter messageReporter1
    ) throws IOException {
        ResourcePathTranslator resourcePathTranslator1 = (ResourcePathTranslator) object;
        String markerString = ResourcePathTranslator.getMarkerString();
        Enumeration enumeration = super.headers.keys();
        String markerStringValue = markerString;

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (key.equals("Name")) {
                String sectionName = this.getSectionName();
                if (sectionName != null && sectionName.length() <= 0) {
                    printWriter.println(this.wrapManifestLine("Name: "));
                } else {
                    printWriter.println(
                            this.wrapManifestLine("Name: " + AbstractManifestProcessor.translateResourcePath(sectionName, enumerableMap1, resourcePathTranslator1))
                    );
                }
            } else if (key.equals("Java-Bean")) {
                String rawHeaderValue = this.getRawHeaderValue("Java-Bean");
                printWriter.println("Java-Bean: " + rawHeaderValue);
            } else if (key.equals("Sealed")) {
                String rawHeaderValueValue = this.getRawHeaderValue("Sealed");
                printWriter.println("Sealed: " + rawHeaderValueValue);
            } else if (!key.equals("Magic") && key.indexOf("Digest") == -1) {
                BooleanFlag booleanFlag = new BooleanFlag();
                String string4 = this.translateClassListAttribute(key, enumerableMap, enumerableMap1, booleanFlag, string, bl, messageReporter1);
                if (!booleanFlag.getValue() || HiddenOptionFlags.KEEP_UNCHANGED_MANIFEST_ENTRIES) {
                    printWriter.println(string4);
                }
            }

            if (markerStringValue == null) {
                break;
            }
        }

        printWriter.println();
    }

    public void collectEntryPointClasses() {
    }

    @Override
    public String normalizeHeaderName(String string) {
        if (string.equalsIgnoreCase("Name")) {
            return "Name";
        } else if (string.equalsIgnoreCase("Java-Bean")) {
            return "Java-Bean";
        } else {
            return string.equalsIgnoreCase("Sealed") ? "Sealed" : string;
        }
    }

    public ManifestEntrySection(BufferedReader bufferedReader) throws ZkmException, IOException {
        super(bufferedReader);
    }
}

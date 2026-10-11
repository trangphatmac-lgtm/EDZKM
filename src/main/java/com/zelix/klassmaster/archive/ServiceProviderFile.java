package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.OrderedIndexedMap;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ServiceProviderFile {
    public static final int SERVICES_PREFIX_LENGTH = "META-INF/services/".length();
    public OrderedIndexedMap lines = new OrderedIndexedMap();
    public ArrayList providerClassNames = new ArrayList();
    public boolean compressed;
    public String serviceName;

    public void mergeFrom(ServiceProviderFile serviceProviderFile1) {
        if (!this.compressed && !serviceProviderFile1.compressed) {
            this.compressed = false;
        } else {
            this.compressed = true;
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        Iterator iterator = this.lines.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            String value = (String) entry.getValue();
            if (value != null) {
                hashSet.add(value);
            }
        }

        iterator = serviceProviderFile1.lines.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            String key = (String) entry1.getKey();
            String valueValue = (String) entry1.getValue();
            if (valueValue != null) {
                if (hashSet.add(valueValue)) {
                    this.lines.put(key, valueValue);
                }
            } else if (!this.lines.containsKey(key)) {
                this.lines.put(key, null);
            }
        }
    }

    
    
    public void writeTranslated(ZipOutputStream zipOutputStream1, Long long1, EnumerableMap enumerableMap, boolean compressedArgument) throws IOException {
        ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, this.getTranslatedEntryName(enumerableMap), compressedArgument);
        PrintWriter printWriter = null;
        boolean bl1 = false ;

        try {
            bl1 = true;
            printWriter = new PrintWriter(new OutputStreamWriter(zipEntryWriter.getOutputStream(), "UTF-8"));
            Iterator iterator = this.lines.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (value != null) {
                    String keyValue = value.replace('.', '/');
                    String string3 = (String) ZkmUtils.mapOrSelf(keyValue, enumerableMap);
                    if (!keyValue.equals(string3)) {
                        key = ZkmStringUtils.replaceAll(key, value, string3.replace('/', '.'));
                    }
                }

                printWriter.println(key);
            }

            printWriter.flush();
            bl1 = false;
        } finally {
            if (bl1) {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        }

        printWriter.close();
        zipEntryWriter.writeEntry(long1);
    }

    public ServiceProviderFile(ZipFile zipFile1, ZipEntry zipEntry1) throws ZkmException, IOException {
        if (zipEntry1.getMethod() == 8) {
            this.compressed = true;
        } else {
            this.compressed = false;
        }

        this.serviceName = zipEntry1.getName().substring(SERVICES_PREFIX_LENGTH).trim();
        InputStream inputStream1 = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream1 = zipFile1.getInputStream(zipEntry1);
            bufferedReader = ZkmFileUtils.createBomAwareReader(inputStream1, "UTF-8", (ObservableHolder) null);

            String key;
            while ((key = bufferedReader.readLine()) != null) {
                String second = key;
                int ba = key.indexOf("#");
                if (ba != -1) {
                    if (ba > 0) {
                        second = key.substring(0, ba - 1);
                    } else {
                        second = "";
                    }
                }

                second = second.trim();
                if (second.length() > 0) {
                    this.providerClassNames.add(second);
                    this.lines.put(key, second);
                } else {
                    this.lines.put(key, null);
                }
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException iOException1) {
                }
            } else if (inputStream1 != null) {
                try {
                    inputStream1.close();
                } catch (IOException iOException) {
                }
            }
        }
    }

    public String getTranslatedEntryName(EnumerableMap enumerableMap) {
        String string = ((String) ZkmUtils.mapOrSelf(this.serviceName.replace('.', '/'), enumerableMap)).replace('/', '.');
        return "META-INF/services/" + string;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public void collectReferencedClasses(String string, Map map1) {
        String value = "'META-INF/services/" + this.serviceName + "' in '" + string + "'";
        ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(ZkmUtils.dotsToSlashes(this.serviceName));
        if (programClass1 != null) {
            map1.put(programClass1, value);
        }

        int elementIndex = 0;
        int bb = 0;

        for (ArrayList arrayList = this.providerClassNames; bb < arrayList.size(); arrayList = this.providerClassNames) {
            ProgramClass programClass2 = ClassHierarchyNode.findProgramClass(ZkmUtils.dotsToSlashes((String) this.providerClassNames.get(elementIndex)));
            if (programClass2 != null) {
                map1.put(programClass2, value);
            }

            bb = ++elementIndex;
        }
    }
}

package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.archive.ZipEntryWriter;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.MutableInt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.OutputStream;

public class XmlZipEntryRewriter {
    public XmlZipEntryRewriter(final File file, final MutableInt mutableInt, final MutableInt mutableInt2, final MutableInt mutableInt3, final String s, final File file2, final XmlContentHandler xmlContentHandler, final boolean b) throws ZkmException, IOException {
        final File parentFile = file2.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(file);
            outputStream = new FileOutputStream(file2);
            new XmlResourceProcessor(inputStream, outputStream, xmlContentHandler, s, mutableInt.getValue(), mutableInt2.getValue(), mutableInt3.getValue(), file.getAbsolutePath(), b);
            try {
                ((FileInputStream) inputStream).close();
            } catch (final IOException ioException) {
            }
            try {
                ((FileOutputStream) outputStream).close();
            } catch (final IOException ioExceptionValue) {
            }
        } finally {
            try {
                if (inputStream != null) {
                    ((FileInputStream) inputStream).close();
                }
            } catch (final IOException ioExceptionValue2) {
            }
            try {
                if (outputStream != null) {
                    ((FileOutputStream) outputStream).close();
                }
            } catch (final IOException ioExceptionValue3) {
            }
        }
    }

    public XmlZipEntryRewriter(
            ZipFile zipFile1,
            ZipEntry zipEntry1,
            Long long1,
            MutableInt mutableInt,
            MutableInt mutableInt1,
            MutableInt mutableInt2,
            String string,
            ZipOutputStream zipOutputStream1,
            ResourcePathTranslator resourcePathTranslator1,
            XmlContentHandler xmlContentHandler,
            boolean compressed,
            boolean bl1
    ) throws ZkmException, IOException {
        InputStream inputStream1 = null;

        try {
            inputStream1 = zipFile1.getInputStream(zipEntry1);
            String entryName = resourcePathTranslator1.translateResourcePath(zipEntry1.getName());
            ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, entryName, compressed);
            new XmlResourceProcessor(
                    inputStream1,
                    zipEntryWriter.getOutputStream(),
                    xmlContentHandler,
                    string,
                    mutableInt.getValue(),
                    mutableInt1.getValue(),
                    mutableInt2.getValue(),
                    ZkmFileUtils.getEntryPath(zipFile1, zipEntry1),
                    bl1
            );
            zipEntryWriter.writeEntry(long1);
        } finally {
            try {
                if (inputStream1 != null) {
                    inputStream1.close();
                }
            } catch (IOException iOException) {
            }
        }
    }

    public XmlZipEntryRewriter(final File file, final MutableInt mutableInt, final MutableInt mutableInt2, final MutableInt mutableInt3, final String s, final ZipOutputStream zipOutputStream, final XmlContentHandler xmlContentHandler, final boolean compressed, final boolean b2) throws ZkmException, IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            final ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, file.getName(), compressed);
            new XmlResourceProcessor(inputStream, zipEntryWriter.getOutputStream(), xmlContentHandler, s, mutableInt.getValue(), mutableInt2.getValue(), mutableInt3.getValue(), file.getName(), b2);
            zipEntryWriter.writeEntry();
            try {
                ((FileInputStream) inputStream).close();
            } catch (final IOException ioException) {
            }
        } finally {
            try {
                if (inputStream != null) {
                    ((FileInputStream) inputStream).close();
                }
            } catch (final IOException ioExceptionValue) {
            }
        }
    }
}

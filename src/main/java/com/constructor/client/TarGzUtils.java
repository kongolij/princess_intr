package com.constructor.client;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.*;

public class TarGzUtils {

    public static void createTarGzArchive(List<File> files, File outputTarGzFile) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(outputTarGzFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GZIPOutputStream gos = new GZIPOutputStream(bos);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gos)
        ) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            for (File file : files) {
                if (!file.exists()) {
                    throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
                }

                TarArchiveEntry entry = new TarArchiveEntry(file, file.getName());
                entry.setSize(file.length());
                taos.putArchiveEntry(entry);

                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = bis.read(buffer)) != -1) {
                        taos.write(buffer, 0, read);
                    }
                }

                taos.closeArchiveEntry();
            }

            taos.finish(); // Finalizes the TAR layer
            taos.flush();
            gos.finish();  // Finalizes the GZIP layer
            gos.flush();
            bos.flush();   // Ensures all buffered data is pushed to disk
        }
    }
}

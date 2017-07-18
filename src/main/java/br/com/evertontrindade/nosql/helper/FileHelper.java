package br.com.evertontrindade.nosql.helper;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by everton on 04/09/16.
 */
@Component
public class FileHelper {

    public String getContent(String path) throws IOException {
        Logger.getLogger(FileHelper.class).info("File to read: " + path);

        String result = IOUtils.toString(Files.newInputStream(Paths.get(path)), "UTF-8");
        Logger.getLogger(FileHelper.class).info("file read locally... ");

        return result;
    }

    public void writeContent(String path, String content) throws IOException {

        deleteFile(path);

        File file = new File(path);
        Logger.getLogger(FileHelper.class).info("Create new file...");
        file.createNewFile();

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        Logger.getLogger(FileHelper.class).info("Write content...");
        out.write(content.getBytes());
        Logger.getLogger(FileHelper.class).info("Close file...");
        out.close();
    }

    public void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            Logger.getLogger(FileHelper.class).info("Deleting file...");
            file.delete();
        }
    }
}

package org.lolobored.bankstatements.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

public class FileUtility {

    public static String getDownloadedFilename(File downloads, int waitTime) throws IOException, InterruptedException {
        /**
         * List resulting files in the download directory
         */
        Collection<File> files = null;
        long initialTime= System.currentTimeMillis();
        while (files==null || files.size()==0){
            files = org.apache.commons.io.FileUtils.listFiles(downloads, new String[]{"csv"}, false);
            long elapsedMilliSeconds = System.currentTimeMillis() - initialTime;
            if (waitTime*1000-elapsedMilliSeconds<0){
                throw new IOException("No csv file was downloaded in "+waitTime+" sec");
            }
            Thread.sleep(500);
        }

        if (files.size() != 1) {
            throw new IOException("Only one csv file was supposed to be in the directory but instead [" + files.size() + "] were found");
        }

        return files.toArray(new File[0])[0].getName();

    }

    public static String readDownloadedFile(File downloads, int waitTime) throws IOException, InterruptedException {
        String fileName = getDownloadedFilename(downloads, waitTime);
        File toRead= new File(downloads.getAbsolutePath()+"/"+fileName);

        String content = FileUtils.readFileToString(toRead, Charset.defaultCharset());
        toRead.delete();
        return content;

    }
}

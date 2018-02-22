package me.mjaroszewicz.service;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public enum FileUtils {;

    private final static Logger log = LoggerFactory.getLogger(FileUtils.class);


    /**
     * Unzips provided file to specified directory
     * @param file - file to be unzipped
     * @param directory - extraction directory
     */
    public static void unzip(Path file, Path directory){

        File directoryFile = directory.toFile();
        File zipFile = file.toFile();

        //create directory if not found
        if(!directoryFile.exists())
            directoryFile.mkdir();

        if(!directoryFile.isDirectory())
            throw new IllegalArgumentException("File: " + directoryFile.getAbsolutePath() + " is not a directory. ");

        if(zipFile.isDirectory())
            throw new IllegalArgumentException("File: " + zipFile.getAbsolutePath() + " is a directory. ");

        try{
            ZipFile zip = new ZipFile(zipFile);
            zip.extractAll(directory.toAbsolutePath().toString());

        }catch (ZipException ex){
            ex.printStackTrace();
        }


    }
}

package com.microservices.arch.async.pdfextractservice.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Utils {
//    private static final String INPUT_DIR = "C:/Users/User/Documents/input";
//    private static final String INPUT_DIR = "D:/input_1";
    private static final String INPUT_DIR = "D:/input_2";
    private static final String UPLOAD_DIR_TEXT = "D:/output_2/text";

    public static void main(String[] args) throws IOException {

//        createPdfs();
            createCsv();
    }

    private static void createCsv() throws IOException {
        StringBuilder fileData = new StringBuilder("file,rerun\r\n");

        for(int i=0; i<500; i++) {
            fileData.append(INPUT_DIR + "/" + "10840_" + (i+1) + ".pdf,false\r\n");
        }
        String filename = "input_2.csv";
        Files.write(Paths.get(INPUT_DIR + "/" + filename),
                fileData.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE);
        System.out.println(fileData.toString());
    }

    private static void createPdfs() throws IOException {
        Path sourcePath = Paths.get(INPUT_DIR + "/" + "10840.pdf");
        for (int i = 0; i < 500; i++) {
            Files.copy(sourcePath,
                    Paths.get(INPUT_DIR + "/" + "10840_" + (i+1) + ".pdf"));

        }
    }
}

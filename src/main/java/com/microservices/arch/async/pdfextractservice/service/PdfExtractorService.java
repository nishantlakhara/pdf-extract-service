package com.microservices.arch.async.pdfextractservice.service;

import com.microservices.arch.async.pdfextractservice.constants.Status;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

@Service
public class PdfExtractorService {
    private static final String SEPARATOR = "/";
    private static final int MAX_NO_OF_PAGES = 50;
    private static final int MAX_DOC_SIZE_TO_PROCESS = 50;
    public static final int NO_OF_PDFBOX_THREADS = 50;

    @Value("${pdf-text-service.output-pdf-dir}")
    private String UPLOAD_DIR;

    @Value("${pdf-text-service.output-text-dir}")
    private String UPLOAD_DIR_TEXT;

    @Value("${pdf-text-service.enable-multithreading-pdfbox}")
    private Boolean isPdfboxMutithreaded;

    @Autowired
    @Qualifier("hazelcastCacheService")
    CacheService hazelcastCacheService;

    private Logger LOGGER = LoggerFactory.getLogger(PdfExtractorService.class);

    ExecutorService executor = Executors.newFixedThreadPool(NO_OF_PDFBOX_THREADS);

    @Async
    public String convertPdfToTextAsync(MultipartFile multipartFile) {
        long currentTime = System.currentTimeMillis();
        String pdfFileName = multipartFile.getOriginalFilename();
        LOGGER.info("Thread " + Thread.currentThread().getName() + " Converting the pdffile to text: " + pdfFileName);
        try {
            //Process using PDFBox
            hazelcastCacheService.put(pdfFileName, Status.PROCESSING_STARTED.getStatus());
            extractTextFromPdf(pdfFileName);
            hazelcastCacheService.put(pdfFileName, Status.PROCESSING_COMPLETED.getStatus());
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("An exception occured: ", e);
            hazelcastCacheService.put(pdfFileName, Status.ERROR.getStatus());
        }
        long elapsedTime = System.currentTimeMillis() - currentTime;
        LOGGER.info("Thread " + Thread.currentThread().getName() + " Converting the pdffile to text: " + pdfFileName + " took " + elapsedTime + "ms");

        return hazelcastCacheService.get(pdfFileName);
    }

    public void uploadFile(MultipartFile multipartFile, String pdfFileName) throws IOException {
        //synchronous file upload
        hazelcastCacheService.put(pdfFileName, Status.UPLOAD_STARTED.getStatus());
        try {
            LOGGER.info("Upload started for file: " + pdfFileName);
            Files.copy(multipartFile.getInputStream(),
                    Paths.get(UPLOAD_DIR + "/" + pdfFileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            hazelcastCacheService.put(pdfFileName, Status.ERROR.getStatus());
            throw e;
        }
        hazelcastCacheService.put(pdfFileName, Status.UPLOAD_COMPLETED.getStatus());
        LOGGER.info("Upload completed for file: " + pdfFileName);
    }

    public String getExtractedText(String filename) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(UPLOAD_DIR_TEXT + "/"
                + filename.substring(0, filename.lastIndexOf(".")) + ".txt"));

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractTextFromPdf(String filename) throws IOException, ExecutionException, InterruptedException {
        long currentTime = System.currentTimeMillis();
        String text = "";
        PDDocument document = null;
        try {
            //Convert pdf to text.
            //Loading an existing document
            File file = new File(UPLOAD_DIR + SEPARATOR + filename);
            document = PDDocument.load(file);

            int numberOfPages = document.getNumberOfPages();
            if (numberOfPages < MAX_NO_OF_PAGES || !isPdfboxMutithreaded) {
                LOGGER.info("Extracting pdf by single thread for file: "+ filename);
                //Initialize a PdfStripper
                PDFTextStripper pdfTextStripper = new PDFTextStripper();

                //Retrieving text from PDF document
                text = pdfTextStripper.getText(document);

            } else {
                LOGGER.info("Extracting pdf by multithreading for file: "+ filename);
                text = extractPdfMultithreaded(document, numberOfPages);
            }

            Files.write(Paths.get(UPLOAD_DIR_TEXT + SEPARATOR + filename.substring(0, filename.lastIndexOf(".")) + ".txt"),
                    text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE);
        } finally {
            //Closing the document
            if (document != null)
                document.close();
        }

        long elapsedTime = System.currentTimeMillis() - currentTime;
        LOGGER.info("The file conversion took " + elapsedTime + "ms");
        return text;
    }

    private String extractPdfMultithreaded(PDDocument document, int numberOfPages) throws IOException, InterruptedException, ExecutionException {
        String text;
        Splitter splitter = new Splitter();
        splitter.setStartPage(1);
        splitter.setEndPage(numberOfPages);
        splitter.setSplitAtPage(MAX_DOC_SIZE_TO_PROCESS);
        List<PDDocument> splittedList = splitter.split(document);
        LOGGER.info("Number of splitted docs = " + splittedList.size());

        List<Future> futures = new ArrayList<>();
        for (PDDocument doc : splittedList) {
            Callable<String> callableTask = () -> {
                PDFTextStripper pdfTextStripper = new PDFTextStripper();
                String data = pdfTextStripper.getText(doc);
                doc.close();
                return data;
            };
            futures.add(executor.submit(callableTask));
            //System.out.println(text);
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Future<String> future : futures) {
            textBuilder.append(future.get());
        }

//        executor.shutdown();
//        if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
//            executor.shutdownNow();
//        }

        text = textBuilder.toString();
        return text;
    }
}
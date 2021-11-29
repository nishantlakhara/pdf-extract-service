package com.microservices.arch.async.pdfextractservice.rest;

import com.hazelcast.org.json.JSONObject;
import com.microservices.arch.async.pdfextractservice.constants.Status;
import com.microservices.arch.async.pdfextractservice.service.CacheService;
import com.microservices.arch.async.pdfextractservice.service.PdfExtractorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("pdf-extract")
public class PdfExtractRestController {
    public static final String getStatusUrl = "http://localhost:8083/pdf-extract/get-status?filename=";
    public static final String getExtractedDataUrl = "http://localhost:8083/pdf-extract/get-extracted-data?filename=";
    private Logger LOGGER = LoggerFactory.getLogger(PdfExtractRestController.class);

    @Autowired
    private PdfExtractorService pdfExtractorService;

    @Autowired
    @Qualifier("hazelcastCacheService")
    CacheService hazelcastCacheService;

    @Value("${pdf-text-service.forceReRun}")
    Boolean forceReRun;

    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile multipartFile) {
        String pdfFileName = multipartFile.getOriginalFilename();
        LOGGER.info("filename:" + pdfFileName);
        LOGGER.info("file size:" + String.valueOf(multipartFile.getSize()));
        LOGGER.info("Content type:" + multipartFile.getContentType());

        try {
            //Validations
            if (multipartFile.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            if (!multipartFile.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body("Invalid content type. Only application/pdf allowed");
            }

            String status = hazelcastCacheService.get(pdfFileName);
            LOGGER.info("Current status pdfFile: "+status);
//             Null status means processed for first time.
//             PROCESSING_COMPLETED means rerun
//             Overriden by forceReRun
            if (status == null || status.equals(Status.PROCESSING_COMPLETED.getStatus())
                            || status.equals(Status.ERROR.getStatus())
                            || forceReRun) {
                LOGGER.info("UPLOAD and PROCESSING started for " + pdfFileName);
                //Save file to server.
                pdfExtractorService.uploadFile(multipartFile, pdfFileName);

                //Process file asynchronously using PDFBox apis.
                pdfExtractorService.convertPdfToTextAsync(multipartFile);
            }
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .header("Location", getStatusUrl + pdfFileName)
                    .body("Current processing status: " + hazelcastCacheService.get(pdfFileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("/get-status")
    public ResponseEntity<String> getStatus(
            @RequestParam("filename") String filename) {

        String status = hazelcastCacheService.get(filename);
        try {
            if (status == null) {
                return ResponseEntity.notFound().build();
            } else if (status.equals(Status.ERROR.getStatus())) {
                return ResponseEntity.noContent().build();
            } else if (!status.equals(Status.PROCESSING_COMPLETED.getStatus())) {
                JSONObject resp = new JSONObject();
                resp.put("filename", filename);
                resp.put("processingStatus", hazelcastCacheService.get(filename));
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .header("Location", getStatusUrl + filename)
                        .body(resp.toString());
            } else {
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header("Location", getExtractedDataUrl + filename)
                        .build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .build();
        }
    }

    @GetMapping("/get-extracted-data")
    public ResponseEntity<String> getExtractedText(
            @RequestParam("filename") String filename) {
        String text = null;
        try {
            text = pdfExtractorService.getExtractedText(filename);
            JSONObject resp = new JSONObject();
            resp.put("filename", filename);
            resp.put("extractedText", text);
            resp.put("processingStatus", hazelcastCacheService.get(filename));

            return ResponseEntity.ok().body(resp.toString());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .build();
        }
    }

    @GetMapping("/clear-status-cache")
    public ResponseEntity<String> clearStatusCache(
            @RequestParam("filename") String filename) {
        try {
            String key = hazelcastCacheService.remove(filename);
            JSONObject resp = new JSONObject();
            resp.put("key", key);

            return ResponseEntity.ok().body(resp.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .build();
        }
    }

    @GetMapping("/clear-status-cache-all")
    public ResponseEntity<String> clearStatusCache() {
        String text = null;
        try {
            hazelcastCacheService.clearCache();
            
            return ResponseEntity.ok().body("Cache cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .build();
        }
    }
}
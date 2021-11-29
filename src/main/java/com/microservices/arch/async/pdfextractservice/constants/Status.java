package com.microservices.arch.async.pdfextractservice.constants;

public enum Status {
    UPLOAD_STARTED("UPLOAD_STARTED"),
    UPLOAD_COMPLETED("UPLOAD_COMPLETED"),
    PROCESSING_STARTED("PROCESSING_STARTED"),
    PROCESSING_COMPLETED("PROCESSING_COMPLETED"),
    ERROR("ERROR");
//    ALREADY_PROCESSING("ALREADY_PROCESSING"),
//    ALREADY_PROCESSED_ONCE("ALREADY_PROCESSED_ONCE");

    private final String status;

    // private enum constructor
    private Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

package com.example.apihub.services.cloudbase.dto;

import lombok.Data;

import java.util.List;

@Data
public class CloudbaseDatabaseInsertResult {

    private String code;
    private String message;
    private String requestId;
    private DataDTO data;

    @Data
    static public class DataDTO {

        private List<String> insertedIds;

    }
}

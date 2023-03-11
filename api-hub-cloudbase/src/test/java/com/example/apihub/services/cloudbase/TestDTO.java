package com.example.apihub.services.cloudbase;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TestDTO {

    private String str;

    private Integer intVal;

    private Long longVal;

    private List<Object> list;

    private Map<String, String> mapStr;

    private Map<String, Object> mapObj;



}

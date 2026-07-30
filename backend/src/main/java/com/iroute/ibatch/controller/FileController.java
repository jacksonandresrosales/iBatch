package com.iroute.ibatch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.infrastructure.file.InputFileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final InputFileService inputFileService;

    public FileController(InputFileService inputFileService) {
        this.inputFileService = inputFileService;
    }

    @GetMapping("/available")
    public List<AvailableFileResponse> getAvailableFiles() {
        return inputFileService.findAvailableCsvFiles();
    }
}

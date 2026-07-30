package com.iroute.ibatch.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.dto.request.ProcessFileRequest;
import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.application.usecase.ProcessedFileService;
import com.iroute.ibatch.infrastructure.file.InputFileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final InputFileService inputFileService;
    private final ProcessedFileService processedFileService;

    public FileController(InputFileService inputFileService, ProcessedFileService processedFileService) {
        this.inputFileService = inputFileService;
        this.processedFileService = processedFileService;
    }

    @GetMapping
    public List<ProcessedFileResponse> getProcessedFiles() {
        return processedFileService.findAll();
    }

    @GetMapping("/available")
    public List<AvailableFileResponse> getAvailableFiles() {
        return inputFileService.findAvailableCsvFiles();
    }

    @PostMapping("/process")
    public ProcessFileResponse processFile(@Valid @RequestBody ProcessFileRequest request) {
        return inputFileService.validateFileForProcessing(request.fileName());
    }
}

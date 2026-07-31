package com.iroute.ibatch.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.AvailableFileService;
import com.iroute.ibatch.application.usecase.FileProcessingService;
import com.iroute.ibatch.dto.request.ProcessFileRequest;
import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.application.usecase.ProcessedFileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final AvailableFileService availableFileService;
    private final FileProcessingService fileProcessingService;
    private final ProcessedFileService processedFileService;

    public FileController(
            AvailableFileService availableFileService,
            FileProcessingService fileProcessingService,
            ProcessedFileService processedFileService) {
        this.availableFileService = availableFileService;
        this.fileProcessingService = fileProcessingService;
        this.processedFileService = processedFileService;
    }

    @GetMapping
    public List<ProcessedFileResponse> getProcessedFiles() {
        return processedFileService.findAll();
    }

    @GetMapping("/available")
    public List<AvailableFileResponse> getAvailableFiles() {
        return availableFileService.findAvailableFiles();
    }

    @PostMapping("/process")
    public ProcessFileResponse processFile(@Valid @RequestBody ProcessFileRequest request) {
        return fileProcessingService.registerFileForProcessing(request.fileName());
    }
}

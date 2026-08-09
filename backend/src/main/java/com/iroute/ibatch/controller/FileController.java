package com.iroute.ibatch.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.AvailableFileService;
import com.iroute.ibatch.application.usecase.FileProcessingService;
import com.iroute.ibatch.application.usecase.FileProgressTracker;
import com.iroute.ibatch.dto.request.ProcessFileRequest;
import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.dto.response.FileDetailResponse;
import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.domain.model.FileProgress;
import com.iroute.ibatch.application.usecase.ProcessedFileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final AvailableFileService availableFileService;
    private final FileProcessingService fileProcessingService;
    private final ProcessedFileService processedFileService;
    private final FileProgressTracker fileProgressTracker;

    public FileController(
            AvailableFileService availableFileService,
            FileProcessingService fileProcessingService,
            ProcessedFileService processedFileService,
            FileProgressTracker fileProgressTracker) {
        this.availableFileService = availableFileService;
        this.fileProcessingService = fileProcessingService;
        this.processedFileService = processedFileService;
        this.fileProgressTracker = fileProgressTracker;
    }

    @GetMapping
    public List<ProcessedFileResponse> getProcessedFiles() {
        return processedFileService.findAll();
    }

    @GetMapping("/{id}")
    public FileDetailResponse getProcessedFileDetail(@PathVariable Long id) {
        return processedFileService.findDetailById(id);
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<FileProgress> getFileProgress(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return fileProgressTracker.getProgress(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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

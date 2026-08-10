package com.iroute.ibatch.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
@Validated
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
    public FileDetailResponse getProcessedFileDetail(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no es valida") int page,
            @RequestParam(defaultValue = "50") @Min(value = 1, message = "El tamano de pagina no es valido")
            @Max(value = 100, message = "El tamano maximo de pagina es 100") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 100, message = "La cuenta no puede exceder 100 caracteres") String account) {
        return processedFileService.findDetailById(id, page, size, status, account);
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvailableFileResponse> uploadFile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(availableFileService.uploadCsv(file));
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessFileResponse> processFile(@Valid @RequestBody ProcessFileRequest request) {
        return ResponseEntity.accepted()
                .body(fileProcessingService.registerFileForProcessing(request.fileName()));
    }
}

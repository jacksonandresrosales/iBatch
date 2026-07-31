package com.iroute.ibatch.domain.model;

import java.time.LocalDate;

public record InputFileMetadata(
        String fileName,
        String originalPath,
        LocalDate fileDate) {
}

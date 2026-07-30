package com.iroute.ibatch.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProcessFileRequest(
        @NotBlank(message = "El nombre del archivo es obligatorio")
        String fileName) {
}

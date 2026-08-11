package com.iroute.ibatch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 100, message = "El usuario no puede superar 100 caracteres")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 72, message = "La contrasena no puede superar 72 caracteres")
        String password) {
}

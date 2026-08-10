package com.iroute.ibatch.infrastructure.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

public final class CsvFileValidator {

    private static final List<String> REQUIRED_HEADERS = List.of("cuenta", "monto", "fecha");

    private CsvFileValidator() {
    }

    public static void validateUploadedFile(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                var parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)) {
            validateHeaders(parser.getHeaderMap());

            if (!parser.iterator().hasNext()) {
                throw new IllegalArgumentException("El archivo CSV no contiene transacciones");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalArgumentException("El archivo debe ser un CSV UTF-8 valido", exception);
        }
    }

    public static void validateHeaders(Map<String, Integer> headers) {
        if (headers == null || !new ArrayList<>(headers.keySet()).equals(REQUIRED_HEADERS)) {
            throw new IllegalArgumentException("La estructura del archivo no es valida. Use los encabezados cuenta,monto,fecha");
        }
    }
}

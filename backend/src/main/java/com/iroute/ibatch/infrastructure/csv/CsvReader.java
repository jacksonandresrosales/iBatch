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
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.InputFileMetadata;

@Component
public class CsvReader {

    private static final List<String> REQUIRED_HEADERS = List.of("cuenta", "monto", "fecha");

    public CSVParser openParser(InputFileMetadata inputFile) {
        try {
            BufferedReader reader = Files.newBufferedReader(Path.of(inputFile.originalPath()), StandardCharsets.UTF_8);
            var csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();
            var parser = csvFormat.parse(reader);

            validateHeaders(parser.getHeaderMap());

            return parser;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo CSV", exception);
        }
    }

    private void validateHeaders(Map<String, Integer> headers) {
        if (headers == null || !new ArrayList<>(headers.keySet()).equals(REQUIRED_HEADERS)) {
            throw new IllegalArgumentException("La estructura del archivo no es valida");
        }
    }

    public int countTotalRecords(InputFileMetadata inputFile) {
        try (var lines = Files.lines(Path.of(inputFile.originalPath()), StandardCharsets.UTF_8)) {
            long count = lines.count();
            return (int) Math.max(0, count - 1);
        } catch (IOException exception) {
            return 0;
        }
    }
}



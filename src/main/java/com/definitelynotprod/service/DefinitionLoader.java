package com.definitelynotprod.service;

import com.definitelynotprod.config.MockDefinitionsProperties;
import com.definitelynotprod.domain.definition.MockDefinitionFile;
import com.definitelynotprod.exception.DefinitionLoadException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DefinitionLoader {

    private final MockDefinitionsProperties properties;
    private final ObjectReader definitionReader;

    public DefinitionLoader(MockDefinitionsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.definitionReader = objectMapper.readerFor(MockDefinitionFile.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public List<LoadedDefinitionFile> loadAll() {
        Path directory = resolveDefinitionsPath();
        if (!Files.exists(directory)) {
            throw new DefinitionLoadException("Definitions directory does not exist: " + directory.toAbsolutePath());
        }
        if (!Files.isDirectory(directory)) {
            throw new DefinitionLoadException("Definitions path is not a directory: " + directory.toAbsolutePath());
        }

        try (Stream<Path> pathStream = Files.walk(directory)) {
            List<Path> files = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.naturalOrder())
                    .toList();

            List<LoadedDefinitionFile> loadedFiles = new ArrayList<>();
            for (Path file : files) {
                loadedFiles.add(new LoadedDefinitionFile(file, parse(file)));
            }
            return loadedFiles;
        } catch (IOException e) {
            throw new DefinitionLoadException("Failed to read definitions directory: " + directory.toAbsolutePath(), e);
        }
    }

    private MockDefinitionFile parse(Path file) {
        try {
            return definitionReader.readValue(file);
        } catch (JacksonException e) {
            throw new DefinitionLoadException("Invalid JSON in definition file " + file.toAbsolutePath() + ": " + e.getOriginalMessage(), e);
        }
    }

    private Path resolveDefinitionsPath() {
        Path configured = Paths.get(properties.path());
        return configured.isAbsolute() ? configured : configured.toAbsolutePath().normalize();
    }

    public record LoadedDefinitionFile(Path path, MockDefinitionFile definition) {
    }
}

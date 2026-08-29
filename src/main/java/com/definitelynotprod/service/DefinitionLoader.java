package com.definitelynotprod.service;

import com.definitelynotprod.DefinitelyNotProdApplication;
import com.definitelynotprod.config.MockDefinitionsProperties;
import com.definitelynotprod.domain.definition.MockDefinitionFile;
import com.definitelynotprod.exception.DefinitionLoadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
public class DefinitionLoader {

    private final MockDefinitionsProperties properties;
    private final ObjectReader definitionReader;
    private final Supplier<Path> workingDirectorySupplier;
    private final Supplier<Path> applicationHomeSupplier;

    @Autowired
    public DefinitionLoader(MockDefinitionsProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper,
                () -> Path.of("").toAbsolutePath().normalize(),
                () -> new ApplicationHome(DefinitelyNotProdApplication.class).getDir().toPath().toAbsolutePath().normalize());
    }

    DefinitionLoader(MockDefinitionsProperties properties,
                     ObjectMapper objectMapper,
                     Supplier<Path> workingDirectorySupplier,
                     Supplier<Path> applicationHomeSupplier) {
        this.properties = properties;
        this.definitionReader = objectMapper.readerFor(MockDefinitionFile.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.workingDirectorySupplier = workingDirectorySupplier;
        this.applicationHomeSupplier = applicationHomeSupplier;
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
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path fromWorkingDirectory = workingDirectorySupplier.get().resolve(configured).normalize();
        if (Files.exists(fromWorkingDirectory)) {
            return fromWorkingDirectory;
        }

        Path fromApplicationHome = resolveFromApplicationHome(configured);
        if (fromApplicationHome != null) {
            return fromApplicationHome;
        }

        return fromWorkingDirectory;
    }

    private Path resolveFromApplicationHome(Path configured) {
        Path current = applicationHomeSupplier.get();
        while (current != null) {
            Path candidate = current.resolve(configured).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    public record LoadedDefinitionFile(Path path, MockDefinitionFile definition) {
    }
}

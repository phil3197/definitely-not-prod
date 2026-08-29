package com.definitelynotprod.service;

import com.definitelynotprod.config.MockDefinitionsProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefinitionLoaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldResolveRelativeDefinitionsPathFromApplicationHomeAncestors() throws IOException {
        Path projectRoot = Files.createTempDirectory("dnp-loader-root-");
        Path definitionsDir = Files.createDirectories(projectRoot.resolve("definitions"));
        Files.writeString(definitionsDir.resolve("example.json"), """
                {
                  "apiName": "example-api",
                  "version": "v1",
                  "basePath": "/api/example",
                  "endpoints": [
                    {
                      "name": "get-example",
                      "method": "GET",
                      "path": "/status",
                      "response": {
                        "status": 200,
                        "body": {
                          "ok": true
                        }
                      }
                    }
                  ]
                }
                """);

        Path unrelatedWorkingDirectory = Files.createTempDirectory("dnp-loader-cwd-");
        Path applicationHome = Files.createDirectories(projectRoot.resolve("build/classes/java/main"));

        DefinitionLoader loader = new DefinitionLoader(
                new MockDefinitionsProperties("definitions"),
                objectMapper,
                () -> unrelatedWorkingDirectory,
                () -> applicationHome
        );

        List<DefinitionLoader.LoadedDefinitionFile> loadedFiles = loader.loadAll();

        assertThat(loadedFiles).hasSize(1);
        assertThat(loadedFiles.getFirst().path()).isEqualTo(definitionsDir.resolve("example.json"));
        assertThat(loadedFiles.getFirst().definition().getApiName()).isEqualTo("example-api");
    }
}

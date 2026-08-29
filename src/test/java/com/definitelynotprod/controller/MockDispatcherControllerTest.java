package com.definitelynotprod.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MockDispatcherControllerTest {

    static final Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("dnp-tests-");
            Files.writeString(tempDir.resolve("test-definition.json"), """
                    {
                      "apiName": "test-api",
                      "version": "v1",
                      "basePath": "/api/items",
                      "endpoints": [
                        {
                          "name": "get-item",
                          "enabled": true,
                          "priority": 5,
                          "method": "GET",
                          "path": "/1",
                          "headers": {
                            "X-Test": "yes"
                          },
                          "response": {
                            "status": 200,
                            "body": {
                              "id": 1,
                              "name": "demo"
                            }
                          }
                        },
                        {
                          "name": "post-item",
                          "enabled": true,
                          "priority": 10,
                          "method": "POST",
                          "path": "/1",
                          "requestBodyMatch": {
                            "name": "created"
                          },
                          "response": {
                            "status": 201,
                            "body": {
                              "created": true
                            }
                          }
                        },
                        {
                          "name": "search-item",
                          "enabled": true,
                          "priority": 7,
                          "method": "GET",
                          "path": "/search",
                          "queryParams": {
                            "status": "active"
                          },
                          "response": {
                            "status": 200,
                            "body": {
                              "results": [
                                {
                                  "id": 2,
                                  "status": "active"
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                    """);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.mock-definitions.path", () -> tempDir.toString());
    }

    @Test
    void shouldDispatchMatchedMock() throws Exception {
        mockMvc.perform(get("/api/items/1").header("X-Test", "yes"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturn404WhenNoMockMatches() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No mock matched"));
    }

    @Test
    void shouldReturn405WhenPathMatchesButMethodDoesNot() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/items/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturn400ForInvalidJsonWhenBodyMatchingIsConfigured() throws Exception {
        mockMvc.perform(post("/api/items/1").contentType(APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON request body"));
    }

    @Test
    void shouldMatchQueryParameters() throws Exception {
        mockMvc.perform(get("/api/items/search").queryParam("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("active"));
    }
}

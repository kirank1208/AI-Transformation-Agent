package com.example.mcpclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final RestTemplate restTemplate;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mcp.server.url}")
    private String mcpServerUrl;

    @Autowired
    public McpService(RestTemplate restTemplate, AiService aiService) {
        this.restTemplate = restTemplate;
        this.aiService = aiService;
    }

    public JsonNode processUserRequest(Map<String, Object> userRequest) {
        log.info("Starting MCP client process");

        JsonNode tools = getTools();
        if (tools == null) {
            log.error("Failed to retrieve tools from MCP server");
            return null;
        }
        log.debug("Retrieved tools: {}", tools);

        String selectedTool = aiService.selectTool(userRequest, tools);
        if (selectedTool == null) {
            log.error("Failed to select a tool");
            return null;
        }
        log.info("Selected tool: {}", selectedTool);

        JsonNode schema = tools.get(selectedTool).get("schema");
        JsonNode transformedInput = aiService.transformQuery(userRequest, schema);
        if (transformedInput == null) {
            log.error("Failed to transform query");
            return null;
        }
        log.debug("Transformed input: {}", transformedInput);

        JsonNode response = executeTool(selectedTool, transformedInput);
        if (response != null) {
            log.info("Successfully executed tool");
            log.info("Response from MCP server: {}", response);
            return response;
        } else {
            log.error("Failed to execute tool");
            return null;
        }
    }

    private JsonNode getTools() {
        String url = mcpServerUrl + "/mcp/tools";
        log.debug("Requesting tools from: {}", url);

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Error getting tools: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while getting tools", e);
            return null;
        }
    }

    private JsonNode executeTool(String tool, JsonNode input) {
        String url = mcpServerUrl + "/mcp/execute";
        log.debug("Executing tool at: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("tool", tool);
            requestBody.set("input", input);

            HttpEntity<JsonNode> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Error executing tool: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while executing tool", e);
            return null;
        }
    }

    private Map<String, Object> createSampleUserRequest() {
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("title", "Submission Intake Request");
        userRequest.put("submission", Map.of(
                "initialInformation", Map.of(
                        "submissionDescription", "Test Submission",
                        "underWritingYear", "2025",
                        "expiryDate", "2026-12-30T17:32:28Z",
                        "inceptionDate", "2025-02-04T01:01:01Z",
                        "codeISIC", "123456789" // Added ISIC code
                ),
                "parties", Map.of(
                        "partyName", "WTWFEB",
                        "role", "Insured",
                        "dunsNumber", "079481909"
                )
        ));
        return userRequest;
    }
}
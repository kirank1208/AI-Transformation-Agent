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

    /**
     * Process a user request through the MCP pipeline.
     */
    public JsonNode processUserRequest(Map<String, Object> userRequest) {
        log.info("Starting MCP client process");

        // 1. Get tools from MCP server
        JsonNode tools = getTools();
        if (tools == null) {
            log.error("Failed to retrieve tools from MCP server");
            return null;
        }
        log.debug("Retrieved tools: {}", tools);

        // 2. NEW: Check for ISIC code and enrich the request with the corresponding AOC code.
        enrichRequestWithAocCode(userRequest);

        // 3. Select appropriate tool using the (potentially enriched) AI request
        String selectedTool = aiService.selectTool(userRequest, tools);
        if (selectedTool == null) {
            log.error("Failed to select a tool");
            return null;
        }

        // 4. Transform the request using AI
        JsonNode schema = tools.get(selectedTool).get("schema");
        JsonNode transformedInput = aiService.transformQuery(
                userRequest,
                schema
        );
        if (transformedInput == null) {
            log.error("Failed to transform query");
            return null;
        }
        log.debug("Transformed input: {}", transformedInput);

        // 5. Execute the tool via MCP server
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

    /**
     * NEW HELPER METHOD
     * Checks for an ISIC code in the request, calls the mapping tool, and adds the AOC code.
     */
    private void enrichRequestWithAocCode(Map<String, Object> userRequest) {
        try {
            // Safely navigate the map to find the ISIC code
            Map<String, Object> submission = (Map<String, Object>) userRequest.get("submission");
            if (submission == null) return;

            Map<String, Object> initialInformation = (Map<String, Object>) submission.get("initialInformation");
            if (initialInformation == null || !initialInformation.containsKey("codeISIC")) {
                log.debug("No ISIC code found in the request. Skipping enrichment.");
                return; // No ISIC code present, nothing to do
            }

            Map<String, Object> isicCodeMap = (Map<String, Object>) initialInformation.get("codeISIC");
            String isicCode = (String) isicCodeMap.get("value");

            if (isicCode != null && !isicCode.isEmpty()) {
                log.info("ISIC code found: {}. Attempting to fetch corresponding AOC code.", isicCode);

                // Prepare the input for the isicToAocMapping tool
                ObjectNode isicInput = objectMapper.createObjectNode();
                isicInput.put("isicCode", isicCode);

                // Execute the specific tool to get the AOC code
                JsonNode aocResponse = executeTool("isicToAocMapping", isicInput);

                if (aocResponse != null && aocResponse.has("aocCode")) {
                    String aocCode = aocResponse.get("aocCode").asText();
                    log.info("Successfully fetched AOC code: {}", aocCode);

                    // Enrich the original user request with the new AOC code
                    Map<String, String> aocCodeMap = new HashMap<>();
                    aocCodeMap.put("value", aocCode);
                    aocCodeMap.put("description", "Activity on Location code derived from ISIC.");
                    initialInformation.put("codeAOC", aocCodeMap);
                    log.info("User request has been enriched with AOC code.");
                } else {
                    log.warn("Failed to retrieve AOC code for ISIC: {}. Response: {}", isicCode, aocResponse);
                }
            }
        } catch (Exception e) {
            // Log the exception but don't stop the main process.
            // The transformation can still proceed without the AOC code.
            log.error("Error during ISIC to AOC enrichment process. The main flow will continue.", e);
        }
    }

    /**
     * Retrieves available tools from the MCP server.
     */
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

    /**
     * Executes a selected tool on the MCP server with transformed input.
     */
    private JsonNode executeTool(String tool, JsonNode input) {
        String url = mcpServerUrl + "/mcp/execute";
        log.debug("Executing tool '{}' at: {}", tool, url);

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
                log.error("Error executing tool '{}': {}", tool, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while executing tool '{}'", tool, e);
            return null;
        }
    }
}

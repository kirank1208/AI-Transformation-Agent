package com.example.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${submission.intake.url}")
    private String submissionIntakeUrl;

    // Store tool definitions in a map for easy access
    private final Map<String, ObjectNode> tools = new HashMap<>();

    // ISIC to AOC mapping (simplified for demonstration)
    private final Map<String, String> isicToAocMapping = new HashMap<>();

    public McpController(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        initTools(); // Initialize tools on startup
        initIsicToAocMapping(); // Initialize ISIC to AOC mapping
    }

    private void initTools() {
        try {
            // Load "submissionIntake" tool
            ObjectNode submissionIntake = objectMapper.createObjectNode();
            submissionIntake.put("description", "Handles new submission requests");
            submissionIntake.put("title", "Submission Intake");
            submissionIntake.set("schema", loadSchema("schemas/submission-intake-schema.json"));
            tools.put("submissionIntake", submissionIntake);

            // Load "simpleTool" tool
            ObjectNode simpleTool = objectMapper.createObjectNode();
            simpleTool.put("description", "A very simple tool for demonstration");
            simpleTool.put("title", "Simple Tool");
            simpleTool.set("schema", loadSchema("schemas/simple-tool-schema.json"));
            tools.put("simpleTool", simpleTool);

            // Load "isicToAocMapping" tool
            ObjectNode isicToAocTool = objectMapper.createObjectNode();
            isicToAocTool.put("description", "Converts ISIC code to AOC code");
            isicToAocTool.put("title", "ISIC to AOC Mapping");
            isicToAocTool.set("schema", loadSchema("schemas/isic-to-aoc-schema.json"));
            tools.put("isicToAocMapping", isicToAocTool);

        } catch (IOException e) {
            throw new RuntimeException("Error loading schemas", e);
        }
    }

    private void initIsicToAocMapping() {
        // Simplified mapping for demonstration
        isicToAocMapping.put("123456789", "AOC123");
        isicToAocMapping.put("987654321", "AOC456");
    }

    private ObjectNode loadSchema(String schemaFilePath) throws IOException {
        Resource schemaResource = resourceLoader.getResource("classpath:" + schemaFilePath);
        return (ObjectNode) objectMapper.readTree(schemaResource.getInputStream());
    }

    @GetMapping("/tools")
    public ResponseEntity<JsonNode> getTools() {
        return ResponseEntity.ok(objectMapper.valueToTree(tools));
    }

    @PostMapping("/execute")
    public ResponseEntity<JsonNode> execute(@RequestBody JsonNode request) {
        ObjectNode responseNode = objectMapper.createObjectNode();

        // Basic validation
        if (!request.has("tool") || !request.has("input")) {
            responseNode.put("error", "Invalid request format. Expected 'tool' and 'input' fields.");
            return ResponseEntity.badRequest().body(responseNode);
        }

        String tool = request.get("tool").asText();
        JsonNode input = request.get("input");

        if (tools.containsKey(tool)) {
            // Tool-specific logic here
            if ("submissionIntake".equals(tool)) {
                // Extract data from the input (according to submission-intake-schema.json)
                JsonNode submission = input.get("submission");
                JsonNode submissionGeneralInfo = submission.get("submissionGeneralInfo");
                JsonNode intermediaries = submission.get("intermediaries");

                String submissionDescription = submissionGeneralInfo.get("submissionDescription").asText();
                String underWritingYear = submissionGeneralInfo.get("underWritingYear").asText();
                JsonNode inceptionDate = submissionGeneralInfo.get("inceptionDate");
                JsonNode expiryDate = submissionGeneralInfo.get("expiryDate");

                // Create a response (simulated)
                ObjectNode submissionResponse = objectMapper.createObjectNode();
                ObjectNode submissionGeneralInfoResponse = submissionResponse.putObject("submissionGeneralInfo");
                submissionGeneralInfoResponse.put("submissionDescription", submissionDescription);
                submissionGeneralInfoResponse.put("underWritingYear", underWritingYear);
                submissionGeneralInfoResponse.set("inceptionDate", inceptionDate);
                submissionGeneralInfoResponse.set("expiryDate", expiryDate);
                submissionResponse.set("intermediaries", intermediaries);

                responseNode.set("submission", submissionResponse);

                ObjectNode metadataNode = objectMapper.createObjectNode();
                metadataNode.put("executionTime", 123);
                metadataNode.put("responseSize", 456);
                responseNode.set("metadata", metadataNode);

                return ResponseEntity.ok(responseNode);

            } else if ("simpleTool".equals(tool)) {
                // Extract data from input (according to simple-tool-schema.json)
                String toolInput = input.get("toolInput").asText();

                // Create a simple response
                responseNode.put("toolOutput", "Processed: " + toolInput);
                return ResponseEntity.ok(responseNode);

            } else if ("isicToAocMapping".equals(tool)) {
                // Extract ISIC code from input
                String isicCode = input.get("isicCode").asText();

                // Convert ISIC to AOC
                if (isicToAocMapping.containsKey(isicCode)) {
                    String aocCode = isicToAocMapping.get(isicCode);
                    responseNode.put("aocCode", aocCode);
                    return ResponseEntity.ok(responseNode);
                } else {
                    responseNode.put("error", "ISIC code '" + isicCode + "' not found.");
                    return ResponseEntity.badRequest().body(responseNode);
                }

            } else {
                // Should not happen due to the tools.containsKey check, but handle it anyway
                responseNode.put("error", "Tool '" + tool + "' not found.");
                return ResponseEntity.badRequest().body(responseNode);
            }

        } else {
            responseNode.put("error", "Tool '" + tool + "' not found.");
            return ResponseEntity.badRequest().body(responseNode);
        }
    }

    @GetMapping("/isicToAoc")
    public ResponseEntity<JsonNode> convertIsicToAoc(@RequestParam("isicCode") String isicCode) {
        ObjectNode responseNode = objectMapper.createObjectNode();

        if (!isicToAocMapping.containsKey(isicCode)) {
            responseNode.put("error", "ISIC code '" + isicCode + "' not found.");
            return ResponseEntity.badRequest().body(responseNode);
        }

        String aocCode = isicToAocMapping.get(isicCode);
        responseNode.put("aocCode", aocCode);
        return ResponseEntity.ok(responseNode);
    }
}
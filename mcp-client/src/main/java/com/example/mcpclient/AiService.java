package com.example.mcpclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Uses the AI model to select the most appropriate tool based on the user request.
     */
    public String selectTool(Map<String, Object> userRequest, JsonNode tools) {
        try {
            //log.debug("Selecting tool for request: {}", userRequest.get("title"));
            String toolsDescription = buildToolsDescription(tools);

            String promptTemplateString = """
            You are an expert at selecting the appropriate tool based on a user request and a list of available tools.

            == User Request ==
            {{userRequest}}

            == Available Tools ==
            {{toolsDescription}}

            == Task ==
            I need you to carefully analyze the user request and determine which tool is most appropriate. Please think through this decision step by step:

            1. Analyze the user request to understand its core requirements
            2. Review each tool's description and capabilities
            3. Compare the tools against the user's needs
            4. Eliminate tools that don't fully meet the requirements
            5. Justify your final selection

            == Response Format ==
            Please format your response as follows:

            ------
            [Your detailed reasoning process here]
            ------
            Final Answer: [Tool Name]

            Rules:
            * The final answer must be one of the tool names provided
            * Do not include any extra text after the final answer
            * Keep the reasoning detailed but concise
            """;

            Map<String, Object> variables = Map.of(
                    "userRequest", userRequest,
                    "toolsDescription", toolsDescription
            );

            Prompt prompt = PromptTemplate.from(promptTemplateString).apply(variables);
            //log.info("Prompt sent to LLM: {}", prompt.text());

            String response = chatLanguageModel.generate(prompt.text()).toString().trim();
            log.debug("Raw response from LLM: {}", response);

            String[] parts = response.split("------");
            if (parts.length >= 2) {
                String reasoning = parts[1].trim();
                String finalAnswer = parts[2].trim().replace("Final Answer: ", "").trim();

                //log.info("=== LLM Reasoning Steps ===");
                //log.info(reasoning);
               // log.info("=== End of Reasoning ===");
                log.info("Selected Tool: {}", finalAnswer);

                // Normalize the finalAnswer to match the keys in the tools object
                String normalizedFinalAnswer = finalAnswer.trim().toLowerCase();

                // Check if the normalized finalAnswer exists in the tools object
                if (tools.has(finalAnswer)) {
                    return finalAnswer;
                } else {
                    log.error("Invalid tool selected: {}", finalAnswer);
                    return null;
                }
            } else {
                log.error("Response format invalid: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("Error during tool selection", e);
            return null;
        }
    }

    /**
     * Uses the AI model to transform a user request to match the required schema
     */
    public JsonNode transformQuery(Map<String, Object> userRequest, JsonNode schema) {
        String response=null;
        try {
            log.debug("Transforming query to match schema");

            //  Create prompt template and variables
            String promptTemplateString = """
                You are an expert at transforming user data into a JSON format that conforms to a given schema.

                Here is the user data:
                {{userData}}

                Here is the JSON schema you must adhere to:
                {{schema}}
                
                Transform the user data into a valid JSON object that matches the schema.
                    
                IMPORTANT INSTRUCTIONS:
                    
                * Return ONLY a valid JSON object.
                * Do not include any other text or explanations before or after the JSON.
                * Do not wrap the JSON in code blocks (e.g., ```json).
                * Ensure the JSON object is parsable by a JSON parser.
                * The JSON output MUST represent a valid object.
                """;

            Map<String, Object> variables = new HashMap<>();
            variables.put("userData", userRequest);
            variables.put("schema", schema.toString()); //  Pass the schema JsonNode

            PromptTemplate promptTemplate = PromptTemplate.from(promptTemplateString);
            Prompt prompt = promptTemplate.apply(variables);
            log.info("PROMPT FEED-->" + prompt.text());

            //  Get response from AI model
            response = chatLanguageModel.generate(prompt.text()).toString();
            log.debug("AI transformation response received");
            log.info("LLM Generated transformation response before cleanning"+ response.toString());
            //  Clean the response to remove any markdown formatting
            String cleanedResponse = cleanJson(response);

            //  Parse JSON response
            return objectMapper.readTree(cleanedResponse);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("Error parsing JSON from AI response. Raw response: {}", response, e);
            //  Fallback: Attempt manual extraction if regex cleaning failed
            String extractedJson = extractJson(response);
            if (extractedJson != null) {
                try {
                    return objectMapper.readTree(extractedJson);
                } catch (Exception ex) {
                    log.error("Manual JSON extraction failed.", ex);
                    return null; //  Or a default JsonNode
                }
            }
            return null; //  Or a default JsonNode
        } catch (Exception e) {
            log.error("Error transforming query with AI", e);
            return null;
        }
    }

    /**
     * Helper method to build a description of available tools
     */
    private String buildToolsDescription(JsonNode tools) {
        StringBuilder toolsDescription = new StringBuilder();
        Iterator<String> toolNames = tools.fieldNames();

        while (toolNames.hasNext()) {
            String toolName = toolNames.next();
            JsonNode tool = tools.get(toolName);
            String description = tool.get("description").asText();
            String title = tool.get("title").asText();
            toolsDescription.append("Tool Name: ")
                    .append(toolName)
                    .append(", Title: ")
                    .append(title)
                    .append(", Description: ")
                    .append(description)
                    .append("\n");
        }
        return toolsDescription.toString();
    }

    /**
     * Helper method to trim the prompt if it exceeds the context window
     */
    private String trimPrompt(String prompt, int maxContextLength) {
        //  Assuming a rough estimate of 4 characters per token
        log.info("BIN the Triming prompt method");
        int maxCharacters = maxContextLength * 4;
        if (prompt.length() > maxCharacters) {
            log.warn("Prompt exceeds context window. Trimming prompt.");
            return prompt.substring(0, maxCharacters);
        }
        return prompt;
    }

    /**
     * Helper method to estimate the token count of a string
     */
    private int estimateTokenCount(String text) {
        //  Assuming a rough estimate of 4 characters per token
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Cleans a string to extract a valid JSON payload.
     *
     * @param input The input string from the LLM.
     * @return A cleaned JSON string, or the original string if cleaning fails.
     */
    private String cleanJson(String input) {
        String cleaned = input.trim();
        //  Remove code blocks, extraneous text, etc.
        cleaned = cleaned.replaceAll("```json", "");
        cleaned = cleaned.replaceAll("```", "");
        cleaned = cleaned.replaceAll("^.*?\\{", "{"); //  Remove anything before the first '{'
        cleaned = cleaned.replaceAll("\\}[^}]*$", "}"); //  Remove anything after the last '}'
        return cleaned;
    }

    /**
     * Attempts to extract a JSON object from a string using basic string manipulation.
     * This is a fallback for when regex cleaning fails.
     *
     * @param input The input string.
     * @return The extracted JSON string, or null if extraction fails.
     */
    private String extractJson(String input) {
        int start = input.indexOf('{');
        int end = input.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return input.substring(start, end + 1);
        }
        return null;
    }
}
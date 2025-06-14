package com.example.mcpclient.controller;

import com.example.mcpclient.McpService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mcpclient")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;

    @Autowired
    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @PostMapping("/processRequest")
    public ResponseEntity<JsonNode> processRequest(@RequestBody Map<String, Object> userRequest) {
        //log.info("Received user request: {}", userRequest);
        JsonNode response = mcpService.processUserRequest(userRequest);

        if (response != null) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

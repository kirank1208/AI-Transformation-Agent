# AI Data Transformation Agent-POC
 The AI Transformation Agent with Model Context Protocol (MCP) addresses a critical challenge in the commercial insurance industry: the costly and complex process of transforming data between organizational models and API platforms. This solution leverages an intelligent agent architecture to automate data transformation and tool selection, making integration between carriers, brokers, and downstream systems more efficient. The system demonstrates its practical application through a "Submission Intake Request" workflow, showcasing how the MCP standardizes communication between tools and services.

**Core Features of AI Transformation Agent with MCP**

**Intelligent Data Transformation:** Automatically transforms API requests/responses between different organizational data models, eliminating the complex, time-consuming manual mapping process for insurance carriers and brokers.

**AI-Powered Tool Selection:** LLM analyzes user requests, selects appropriate tools, and transforms data to match required schemas without human intervention.

**Standardized Protocol Framework:** Model Context Protocol (MCP) provides a consistent method for discovering tools, executing requests, and handling responses across disparate systems.

**Centralized Tool Catalog:** MCP Server maintains a discoverable repository of available tools with their schemas, allowing for simple extension without client-side changes.

**End-to-End Request Processing:** Handles the complete lifecycle from receiving user requests through tool selection, data transformation, service execution, and response delivery.

**Insurance-Specific Optimization:** Designed specifically to address commercial insurance integration challenges, as demonstrated by the Submission Intake workflow.


 **Overview**
This diagram illustrates the architecture of an AI Transformation Agent that leverages the Message Context Protocol (MCP) to interact with various tools and services. In this specific use case, the agent handles a "Submission Intake Request".


![image](https://github.developer.allianz.io/agcs/AI-Data-Transformation-Agent-POC/assets/8416/993492a8-5284-448a-bfbf-fab5eb6e6bec)

**Components:**

**1. User:** Initiates the process by sending a request to the AI Transformation Agent.

**2. MCP Client:**
  ○ Receives the user's request. 
  ○ Queries the MCP Server for available tools and their descriptions. 
  ○ Sends the user's request and tool information to the LLM. 
  ○ Receives the LLM's instructions (tool selection and transformed query).
  ○ Executes the selected tool by sending a request to the MCP Server.
  ○ Receives the result from the MCP Server. ○ Sends the result back to the User. 
  
**3. MCP Server:**
  ○ Provides a catalog of available tools and their schemas.
  ○ Executes tools based on requests from the MCP Client.
  ○ In this case, it handles the "Submission Intake" request.
  
**4. LLM (Large Language Model):**
  ○ Analyzes the user's request and available tools.
  ○ Selects the appropriate tool ("Submission Intake").
  ○ Transforms the user's request into a structured format that conforms to the schema of the selected tool.
  
**5. Submission Intake Service:**
  ○ This represents the actual service that processes the submission intake request. It could be a backend API, a database, or any other system.
  ○ Receives the transformed request from the MCP Server.
  ○ Processes the submission data. ○ Returns a response to the MCP Server.

**Flow:**

1. User Request: The user sends a "Submission Intake Request" to the AI Transformation Agent (via the MCP Client). 
2. Tool Discovery: The MCP Client queries the MCP Server for the list of available tools and their schemas. 
3. LLM Processing: The MCP Client sends the user request and the tool information to the LLM. 
4. Tool Selection & Transformation: The LLM selects the "Submission Intake" tool. 
5. The MCP Client sends the user request and the schema associated with the tool to the LLM. 
6. Transformation: The LLM transforms the user's request data into the format required by the Submission Intake Service(Submission Hub). 
7. Tool Execution: The MCP Client sends the transformed request to the MCP Server, specifying the "Submission Intake" tool. 
8. Submission Intake Processing: The MCP Server forwards the request to the Submission Intake Service(Submission Hub). 
9. The Submission Intake Service processes the request and sends a response to the MCP Server. 
10. Response to User: The MCP Server sends the response back to the MCP Client, which then relays it to the user.

**Key Points:** 

● The LLM acts as the "brain" of the agent, making decisions and transforming data.

● The MCP provides a standardized way for the agent to interact with various tools and services.

● The MCP Client and Server handle the communication and execution of tools.

● The Submission Intake Service represents the specific functionality that the agent is accessing.

**Sample Request**

**path:** /mcpclient/processRequest


{
  "submission": {
    "initialInformation": {
      "submissionDescription": {
        "value": "Test Submission",
        "description": "A brief description of the submission."
      },
      "underWritingYear": {
        "value": "2025",
        "description": "The underwriting year for the submission."
      },
      "expiryDate": {
        "value": "2026-12-30T17:32:28Z",
        "description": "The expiry date of the submission in ISO 8601 format."
      },
      "inceptionDate": {
        "value": "2025-02-04T01:01:01Z",
        "description": "The inception date of the submission in ISO 8601 format."
      }
    },
    "parties": [
      {
        "description": "List of parties involved in the submission.",
        "partyName": {
          "value": "WTWFEB",
          "description": "The name of the party."
        },
        "role": {
          "value": "Insured",
          "description": "The role of the party in the submission."
        },
        "dunsNumber": {
          "value": "079481909",
          "description": "The D-U-N-S number, a 9-digit unique business identifier."
        }
      }
    ]
  }
}

**Response**

{
    "submission": {
        "submissionGeneralInfo": {
            "submissionDescription": "Test Submission",
            "underWritingYear": "2025",
            "inceptionDate": [
                2025,
                2,
                4
            ],
            "expiryDate": [
                2026,
                12,
                30
            ]
        },
        "intermediaries": [
            {
                "party": {
                    "role": "Insured",
                    "name": "WTWFEB",
                    "dunsNumber": "079481909"
                }
            }
        ]
    },
    "metadata": {
        "executionTime": 123,
        "responseSize": 456
    }
}

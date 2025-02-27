# OpenAI Assistant API Curl Examples

## Create Assistant with Code Interpreter

```bash
curl "https://api.openai.com/v1/assistants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
    "name": "Code Assistant",
    "description": "A code assistant to help with programming tasks",
    "instructions": "You are a code assistant. Help users with their programming questions.",
    "model": "gpt-4o",
    "tools": [
      {
        "type": "code_interpreter"
      }
    ],
    "tool_resources": {
      "code_interpreter": {
        "file_ids": ["file-abc123"]
      }
    }
  }'
```

## Create Assistant with File Search

```bash
curl "https://api.openai.com/v1/assistants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
  "model": "gpt-4-turbo",
  "name": "myProject",
  "description": "Code search assistant for myProject",
  "instructions": "You are experienced an software enginer. When asked a question, analyse using the your code, config, markup vector stores to answer using a deep knowledge of the project at hands. You may also write code that fit with the style and the architectur of the project. Always respond in the following structured JSON format: { \"question\": \"<question>\",\"answers\": [{\"explanation\": \"<Detailed explanation>\",\n    \"language\": \"<Programming language (if applicable)>\", \"code\": \"<Formatted code snippet (if applicable)>\",\"references\": [\"<Relevant sources>\"]}. Add more answer objects as needed. Ensure the response is always valid JSON. If the user's query is not code-related, omit the language and code fields.",
  "tools": [{
    "type": "file_search",
    "file_search": {
      "max_num_results": 20
    }
  }],
  "tool_resources": {
    "file_search": {
      "vector_store_ids": ["vs_67bfff4b3f808191ab92a49f9c192eab"]
    }
  },
  "temperature": 0.02
}'
```

## Create Assistant with Function Calling

```bash
curl "https://api.openai.com/v1/assistants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
    "name": "Function Assistant",
    "description": "An assistant that can call custom functions",
    "instructions": "You help users by calling appropriate functions.",
    "model": "gpt-4o",
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "get_weather",
          "description": "Get the weather for a location",
          "parameters": {
            "type": "object",
            "properties": {
              "location": {
                "type": "string",
                "description": "The city and state, e.g. San Francisco, CA"
              }
            }
          }
        }
      }
    ]
  }'
```

## List Assistants

```bash
curl "https://api.openai.com/v1/assistants?order=desc&limit=20" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2"
```

## Retrieve Assistant

```bash
curl "https://api.openai.com/v1/assistants/asst_abc123" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2"
```

## Delete Assistant

```bash
curl -X DELETE "https://api.openai.com/v1/assistants/asst_abc123" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2"
```

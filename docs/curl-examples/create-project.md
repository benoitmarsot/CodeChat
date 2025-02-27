# Create Project API Example

Basic curl command to create a new project:

```bash
curl -X POST 'http://localhost:8080/api/v1/codechat/create-project' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -d '{
    "name": "My Project",
    "description": "A sample project",
    "sourcePath": "/path/to/your/project/source"
  }'
```

The response will be a JSON object containing the created project details:

```json
{
  "projectId": 1,
  "name": "My Project",
  "description": "A sample project",
  "userId": 123,
  "assistantId": 456
}
```

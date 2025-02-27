# API Examples

## Delete All Resources

Delete all resources including messages, threads, projects, vector stores, files, and assistants:

```bash
curl -X DELETE http://localhost:8080/api/v1/codechat/delete-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response:
```json
"All data deleted"
```

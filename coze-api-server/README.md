# Coze API Server 使用指南

## 简介
Coze API Server 提供了一个 RESTful API 接口，用于与 Coze AI 进行交互。支持创建聊天、轮询状态和流式响应等功能。

## 配置
在使用 API 之前，需要在 `application.yml` 中配置以下参数：

```yaml
coze:
  apiKey: ${COZE_API_KEY:your-api-key-here}  # Coze API 密钥
  baseUrl: ${COZE_BASE_URL:https://api.coze.cn}  # API 基础 URL
  connectTimeout: ${COZE_CONNECT_TIMEOUT:10000}  # 连接超时时间（毫秒）
  readTimeout: ${COZE_READ_TIMEOUT:30000}  # 读取超时时间（毫秒）
```

## API 端点

### 1. 创建聊天会话
```http
POST /api/chat/chat
Content-Type: application/json

{
  "botID": "your-bot-id",
  "userID": "your-user-id",
  "messages": [
    {
      "role": "user",
      "content": "你好，请问你能做什么？"
    }
  ]
}
```

响应示例：
```json
{
  "success": true,
  "data": {
    "chat": {
      "id": "chat-id",
      "conversationID": "conversation-id",
      "status": "IN_PROGRESS"
    }
  },
  "requestId": "uuid"
}
```

### 2. 创建并自动轮询（推荐）
```http
POST /api/chat/auto-poll
Content-Type: application/json

{
  "botID": "your-bot-id",
  "userID": "your-user-id",
  "messages": [
    {
      "role": "user",
      "content": "你好，请问你能做什么？"
    }
  ]
}
```

响应示例：
```json
{
  "success": true,
  "data": {
    "id": "chat-id",
    "conversationID": "conversation-id",
    "status": "COMPLETED",
    "messages": [
      {
        "role": "assistant",
        "content": "我是 Coze AI 助手..."
      }
    ]
  },
  "requestId": "uuid"
}
```

### 3. 轮询聊天状态
```http
GET /api/chat/poll/{chatId}?conversationId={conversationId}
```

响应示例：
```json
{
  "success": true,
  "data": {
    "id": "chat-id",
    "conversationID": "conversation-id",
    "status": "COMPLETED",
    "messages": [
      {
        "role": "assistant",
        "content": "我是 Coze AI 助手..."
      }
    ]
  },
  "requestId": "uuid"
}
```

### 4. 流式聊天（实时响应）
```http
POST /api/chat/stream
Content-Type: application/json

{
  "botID": "your-bot-id",
  "userID": "your-user-id",
  "messages": [
    {
      "role": "user",
      "content": "你好，请问你能做什么？"
    }
  ]
}
```

使用 JavaScript 接收流式响应：
```javascript
const eventSource = new EventSource('/api/chat/stream');
eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(data.content);
  if (data.done) {
    eventSource.close();
  }
};
eventSource.onerror = (error) => {
  console.error('Error:', error);
  eventSource.close();
};
```

## 使用建议

1. 对于简单的聊天场景，推荐使用 `/api/chat/auto-poll` 端点，它会自动处理轮询过程。

2. 如果需要更细粒度的控制：
   - 先调用 `/api/chat/chat` 创建聊天
   - 然后使用 `/api/chat/poll/{chatId}` 轮询状态

3. 对于需要实时响应的场景，使用 `/api/chat/stream` 端点。

4. 所有请求都需要在 HTTP 头中包含正确的 Content-Type。

5. 错误处理：
   - 所有接口都会返回统一的响应格式
   - 如果发生错误，`success` 字段将为 `false`
   - 错误信息将在 `message` 字段中返回

## 示例代码

### cURL
```bash
# 创建聊天
curl -X POST http://localhost:8080/api/chat/chat \
  -H "Content-Type: application/json" \
  -d '{
    "botID": "your-bot-id",
    "userID": "your-user-id",
    "messages": [
      {
        "role": "user",
        "content": "你好，请问你能做什么？"
      }
    ]
  }'

# 轮询状态
curl http://localhost:8080/api/chat/poll/chat-id?conversationId=conversation-id
```

### Java
```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

CreateChatReq request = CreateChatReq.builder()
    .botID("your-bot-id")
    .userID("your-user-id")
    .messages(Collections.singletonList(
        Message.buildUserQuestionText("你好，请问你能做什么？")
    ))
    .build();

HttpEntity<CreateChatReq> entity = new HttpEntity<>(request, headers);
ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
    "http://localhost:8080/api/chat/auto-poll",
    entity,
    ApiResponse.class
);
```

### Python
```python
import requests

url = "http://localhost:8080/api/chat/auto-poll"
headers = {"Content-Type": "application/json"}
data = {
    "botID": "your-bot-id",
    "userID": "your-user-id",
    "messages": [
        {
            "role": "user",
            "content": "你好，请问你能做什么？"
        }
    ]
}

response = requests.post(url, json=data, headers=headers)
print(response.json())
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权（API Key 无效） |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

## 注意事项

1. API Key 安全：
   - 不要在客户端代码中硬编码 API Key
   - 建议使用环境变量或配置中心来管理 API Key

2. 请求限制：
   - 建议实现适当的重试机制
   - 注意遵守 API 的速率限制

3. 错误处理：
   - 实现合适的错误处理机制
   - 记录关键错误日志
   - 为用户提供友好的错误提示

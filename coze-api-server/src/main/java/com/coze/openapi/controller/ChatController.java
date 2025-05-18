package com.coze.openapi.controller;

import com.coze.openapi.client.chat.CancelChatReq;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.CreateChatResp;
import com.coze.openapi.client.chat.RetrieveChatReq;
import com.coze.openapi.client.chat.RetrieveChatResp;
import com.coze.openapi.client.chat.model.Chat;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatPoll;
import com.coze.openapi.client.chat.model.ChatStatus;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.model.ApiResponse;
import com.coze.openapi.service.service.CozeAPI;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final CozeAPI cozeAPI;

    @PostMapping("/chat")
    public ApiResponse<CreateChatResp> createChat(@RequestBody @Validated CreateChatReq request) throws Exception {
        log.info("Received chat request: {}", request);
        try {
            CreateChatResp response = cozeAPI.chat().create(request);
            return ApiResponse.<CreateChatResp>builder()
                    .success(true)
                    .data(response)
                    .requestId(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error creating chat", e);
            throw e;
        }
    }
    
    @PostMapping("/chat/auto-poll")
    public ApiResponse<Chat> createAndPollChat(@RequestBody @Validated CreateChatReq request) throws Exception {
        log.info("Received auto-poll chat request: {}", request);
        try {
            ChatPoll poll = cozeAPI.chat().createAndPoll(request);
            return ApiResponse.<Chat>builder()
                    .success(true)
                    .data(poll.getChat())
                    .requestId(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error in create and poll chat", e);
            throw e;
        }
    }

    @GetMapping("/poll/{chatId}")
    public ApiResponse<Chat> pollChat(@PathVariable String chatId, @RequestParam String conversationId) throws Exception {
        log.info("Polling chat status for chatId: {} in conversation: {}", chatId, conversationId);
        
        long timeout = 10L; // 10 seconds timeout
        long start = System.currentTimeMillis() / 1000;
        Chat chat = null;
        
        try {
            while (true) {
                RetrieveChatResp resp = cozeAPI.chat().retrieve(RetrieveChatReq.of(conversationId, chatId));
                chat = resp.getChat();
                
                if (!ChatStatus.IN_PROGRESS.equals(chat.getStatus())) {
                    break;
                }
                
                if ((System.currentTimeMillis() / 1000) - start > timeout) {
                    // Cancel the chat if it exceeds timeout
                    cozeAPI.chat().cancel(CancelChatReq.of(conversationId, chatId));
                    throw new RuntimeException("Chat polling timeout exceeded");
                }
                
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Chat polling interrupted", e);
                }
            }

            return ApiResponse.<Chat>builder()
                    .success(true)
                    .data(chat)
                    .requestId(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error polling chat", e);
            throw e;
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody @Validated CreateChatReq request) throws Exception {
        log.info("Received stream chat request: {}", request);
        SseEmitter emitter = new SseEmitter();

        try {
            Flowable<ChatEvent> eventFlowable = cozeAPI.chat().stream(request);
            eventFlowable
                    .subscribe(
                            event -> {
                                try {
                                    emitter.send(event);
                                    if (event.isDone()) {
                                        emitter.complete();
                                    }
                                } catch (IOException e) {
                                    log.error("Error sending chat event", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            error -> {
                                log.error("Error in chat stream", error);
                                emitter.completeWithError(error);
                            });

            return emitter;
        } catch (Exception e) {
            log.error("Error setting up chat stream", e);
            emitter.completeWithError(e);
            throw e;
        }
    }
}

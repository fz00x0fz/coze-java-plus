package com.coze.openapi.controller;

import com.coze.openapi.client.chat.*;
import com.coze.openapi.client.chat.model.Chat;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatPoll;
import com.coze.openapi.client.chat.model.ChatStatus;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.model.ApiResponse;
import com.coze.openapi.service.service.CozeAPI;
import com.coze.openapi.service.service.chat.ChatService;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private CozeAPI cozeAPI;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private CreateChatReq createChatReq;
    private CreateChatResp createChatResp;
    private Chat chat;
    private ChatPoll chatPoll;

    @BeforeEach
    void setUp() {
        // Mock ChatService
        when(cozeAPI.chat()).thenReturn(chatService);

        // Prepare test data
        createChatReq = CreateChatReq.builder()
                .botID("7504704597571862591")
                .userID("ID1234567890")
                .messages(Collections.singletonList(Message.buildUserQuestionText("test message")))
                .build();

        chat = new Chat();
        chat.setId("test-chat-id" + System.currentTimeMillis());
        chat.setConversationID("test-conv-id");
        chat.setStatus(ChatStatus.COMPLETED);

        createChatResp = new CreateChatResp();
        createChatResp.setChat(chat);

        chatPoll = new ChatPoll();
        chatPoll.setChat(chat);
    }

    @Test
    void createChat_Success() throws Exception {
        when(chatService.create(any(CreateChatReq.class))).thenReturn(createChatResp);

        ApiResponse<CreateChatResp> response = chatController.createChat(createChatReq);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(createChatResp, response.getData());
        verify(chatService).create(createChatReq);
    }

    @Test
    void createAndPollChat_Success() throws Exception {
        when(chatService.createAndPoll(any(CreateChatReq.class))).thenReturn(chatPoll);

        ApiResponse<Chat> response = chatController.createAndPollChat(createChatReq);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(chat, response.getData());
        verify(chatService).createAndPoll(createChatReq);
    }

    @Test
    void pollChat_Success() throws Exception {
        String chatId = "test-chat-id";
        String conversationId = "test-conv-id";
        chat.setStatus(ChatStatus.COMPLETED);

        RetrieveChatResp retrieveResp = new RetrieveChatResp();
        retrieveResp.setChat(chat);

        when(chatService.retrieve(any(RetrieveChatReq.class))).thenReturn(retrieveResp);

        ApiResponse<Chat> response = chatController.pollChat(chatId, conversationId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(chat, response.getData());
        verify(chatService).retrieve(any(RetrieveChatReq.class));
    }

    @Test
    void pollChat_Timeout() throws Exception {
        String chatId = "test-chat-id";
        String conversationId = "test-conv-id";
        chat.setStatus(ChatStatus.IN_PROGRESS);

        RetrieveChatResp retrieveResp = new RetrieveChatResp();
        retrieveResp.setChat(chat);

        when(chatService.retrieve(any(RetrieveChatReq.class))).thenReturn(retrieveResp);
        when(chatService.cancel(any(CancelChatReq.class))).thenReturn(new CancelChatResp());

        assertThrows(RuntimeException.class, () -> chatController.pollChat(chatId, conversationId));
        verify(chatService, atLeastOnce()).retrieve(any(RetrieveChatReq.class));
        verify(chatService).cancel(any(CancelChatReq.class));
    }

    @Test
    void streamChat_Success() throws Exception {
        ChatEvent event = new ChatEvent();
        event.isDone();
        //event.setDone(true);

        when(chatService.stream(any(CreateChatReq.class))).thenReturn(Flowable.just(event));

        SseEmitter emitter = chatController.streamChat(createChatReq);

        assertNotNull(emitter);
        verify(chatService).stream(createChatReq);
    }

    @Test
    void streamChat_Error() throws Exception {
        when(chatService.stream(any(CreateChatReq.class))).thenThrow(new RuntimeException("Test error"));

        assertThrows(RuntimeException.class, () -> chatController.streamChat(createChatReq));
        verify(chatService).stream(createChatReq);
    }

    @Test
    void createChat_Error() throws Exception {
        when(chatService.create(any(CreateChatReq.class))).thenThrow(new RuntimeException("Test error"));

        assertThrows(RuntimeException.class, () -> chatController.createChat(createChatReq));
        verify(chatService).create(createChatReq);
    }

    @Test
    void createAndPollChat_Error() throws Exception {
        when(chatService.createAndPoll(any(CreateChatReq.class))).thenThrow(new RuntimeException("Test error"));

        assertThrows(RuntimeException.class, () -> chatController.createAndPollChat(createChatReq));
        verify(chatService).createAndPoll(createChatReq);
    }
}

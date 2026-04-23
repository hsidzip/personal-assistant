package com.university.assistant.service;

import com.university.assistant.domain.Conversation;
import com.university.assistant.domain.Message;
import com.university.assistant.domain.User;
import com.university.assistant.dto.ChatRequest;
import com.university.assistant.repository.ConversationRepository;
import com.university.assistant.repository.MessageRepository;
import com.university.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final WebClient webClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Value("${groq.api.url}")
    private String groqUrl;

    @Value("${groq.api.key}")
    private String groqKey;

    @Value("${groq.api.model}")
    private String groqModel;

    public String chat(ChatRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else {
            conversation = Conversation.builder()
                    .title(request.getMessage().substring(0, Math.min(50, request.getMessage().length())))
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            conversationRepository.save(conversation);
        }

        Message userMessage = Message.builder()
                .role("user")
                .content(request.getMessage())
                .conversation(conversation)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(userMessage);

        String aiResponse = callGroqApi(request.getMessage());

        Message assistantMessage = Message.builder()
                .role("assistant")
                .content(aiResponse)
                .conversation(conversation)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(assistantMessage);

        return aiResponse;
    }

    private String callGroqApi(String userMessage) {
        String modelName = groqModel.trim();
        
        System.out.println("DEBUG: Sending request to Groq...");
        System.out.println("DEBUG: Model: [" + modelName + "]");

        Map<String, Object> requestBody = Map.of(
            "model", modelName,
            "messages", List.of(
                Map.of("role", "system", "content", "You are a helpful personal assistant."),
                Map.of("role", "user", "content", userMessage)
            )
        );

        try {
            Map response = webClient.post()
                    .uri(groqUrl)
                    .header("Authorization", "Bearer " + groqKey.trim())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    // Обработка ошибки 400 для вывода деталей в консоль
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> 
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            System.out.println("GROQ ERROR DETAIL: " + errorBody);
                            return Mono.error(new RuntimeException("Groq API error: " + errorBody));
                        })
                    )
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            System.err.println("FATAL: Failed to call Groq API: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public List<Conversation> getConversations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return conversationRepository.findByUserId(user.getId());
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }
}
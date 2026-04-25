package com.university.assistant.service;

import com.university.assistant.domain.Conversation;
import com.university.assistant.domain.Message;
import com.university.assistant.domain.User;
import com.university.assistant.dto.ChatRequest;
import com.university.assistant.repository.ConversationRepository;
import com.university.assistant.repository.MessageRepository;
import com.university.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Transactional
    public Map<String, Object> chat(ChatRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Находим диалог или создаем новый
        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else {
            conversation = Conversation.builder()
                    .title(request.getMessage().substring(0, Math.min(30, request.getMessage().length())) + "...")
                    .user(user)
                    .build();
            conversation = conversationRepository.save(conversation);
        }

        // 2. Сохраняем текущее сообщение пользователя
        Message userMessage = Message.builder()
                .role("user")
                .content(request.getMessage())
                .conversation(conversation)
                .build();
        messageRepository.save(userMessage);

        // 3. Берем историю (последние 15 сообщений для контекста)
        List<Message> history = messageRepository.findByConversationId(conversation.getId());
        
        // 4. Запрос к Groq
        String aiResponse = callGroqApi(history);

        // 5. Сохраняем ответ ИИ
        Message assistantMessage = Message.builder()
                .role("assistant")
                .content(aiResponse)
                .conversation(conversation)
                .build();
        messageRepository.save(assistantMessage);

        // ВАЖНО: возвращаем и ответ, и ID диалога, чтобы фронтенд его запомнил
        return Map.of(
            "response", aiResponse,
            "conversationId", conversation.getId()
        );
    }

    private String callGroqApi(List<Message> history) {
        List<Map<String, String>> groqMessages = new ArrayList<>();
        groqMessages.add(Map.of("role", "system", "content", "You are a helpful personal assistant."));
        
        groqMessages.addAll(history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList()));

        try {
            Map<?, ?> response = webClient.post()
                    .uri(groqUrl)
                    .header("Authorization", "Bearer " + groqKey.trim())
                    .bodyValue(Map.of("model", groqModel.trim(), "messages", groqMessages))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                return (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
            }
            return "ИИ не смог сформировать ответ.";
        } catch (Exception e) {
            log.error("Groq API error", e);
            return "Ошибка: " + e.getMessage();
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
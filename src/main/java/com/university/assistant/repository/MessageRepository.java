package com.university.assistant.repository;

import com.university.assistant.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Найти все сообщения, у которых conversation.id = conversationId
    List<Message> findByConversationId(Long conversationId);
}
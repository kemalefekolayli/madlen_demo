package com.madlen.chat.repository;

import com.madlen.chat.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    long countByUserId(String userId);
    
    void deleteByUserIdAndId(String userId, String id);
}

package com.example.madlen_demo2.repository;


import com.example.madlen_demo2.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    long countByUserId(String userId);
    
    void deleteByUserIdAndId(String userId, String id);
}

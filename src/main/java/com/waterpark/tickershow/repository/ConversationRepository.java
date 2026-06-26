package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Conversation;
import com.waterpark.tickershow.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    // Tim danh sach cac phong chat theo trang thai (Vi du: tim tat ca cac phong dang OPEN de Staff ho tro)
    List<Conversation> findByStatus(ConversationStatus status);
    List<Conversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status);
     List<Conversation> findByAssignedStaffIdAndStatusOrderByLastMessageAtDesc(
            Long assignedStaffId,
            ConversationStatus status
    );
}
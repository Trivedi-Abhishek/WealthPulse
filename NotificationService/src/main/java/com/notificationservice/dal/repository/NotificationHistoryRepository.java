package com.notificationservice.dal.repository;

import com.notificationservice.dal.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
     List<NotificationHistory> findByPortfolioIdOrderByTriggeredAtDesc(Long portfolioId);
}
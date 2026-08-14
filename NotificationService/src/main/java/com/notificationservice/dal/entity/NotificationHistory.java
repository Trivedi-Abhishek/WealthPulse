package com.notificationservice.dal.entity;

import com.notificationservice.dal.enums.AlertTypeEnum;
import com.notificationservice.dal.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_history")
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_configuration_id", nullable = false)
    private Long alertConfigurationId;

    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Column(name = "symbol", length = 100)
    private String symbol;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertTypeEnum alertType;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name="status", nullable=false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "triggered_value", nullable = false)
    private BigDecimal triggeredValue;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}

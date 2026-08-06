package com.alertservice.dal.entity;

import com.alertservice.dal.enums.AlertConditionEnum;
import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.dal.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "alert_configuration")
@EntityListeners(AuditingEntityListener.class)
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Column(name = "symbol", length = 100)
    private String symbol;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertTypeEnum alertType;

    @Column(name = "condition", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertConditionEnum condition;

    @Column(name = "threshold_value", nullable = false)
    private BigDecimal thresholdValue;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "created_date", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;
}

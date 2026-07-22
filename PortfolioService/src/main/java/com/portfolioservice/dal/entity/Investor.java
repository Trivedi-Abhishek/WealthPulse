package com.portfolioservice.dal.entity;

import com.portfolioservice.enums.RiskProfileEnum;
import com.portfolioservice.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="investors")
@EntityListeners(AuditingEntityListener.class)
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "risk_profile", nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskProfileEnum riskProfile;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "created_date", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedDate;
}

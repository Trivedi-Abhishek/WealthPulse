package com.portfolioservice.dal.entity;

import com.portfolioservice.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="holdings")
@EntityListeners(AuditingEntityListener.class)
public class Holdings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "symbol", length=100, nullable = false)
    private String symbol;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "average_price", nullable = false)
    private BigDecimal averagePrice;

    @Version// for optimistic locking
    @Column(name = "version")
    private Integer version;

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

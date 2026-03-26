package com.ptithcm.apt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "monthly_metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "apartment_id", "billing_month", "billing_year" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "electricity_old", precision = 10, scale = 2)
    private BigDecimal electricityOld;

    @Column(name = "electricity_new", precision = 10, scale = 2)
    private BigDecimal electricityNew;

    @Column(name = "water_old", precision = 10, scale = 2)
    private BigDecimal waterOld;

    @Column(name = "water_new", precision = 10, scale = 2)
    private BigDecimal waterNew;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

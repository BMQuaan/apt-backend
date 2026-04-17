package com.ptithcm.apt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import com.ptithcm.apt.enums.BillStatus;

@Entity
@Table(name = "bills", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "apartment_id", "billing_month", "billing_year" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

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

    @Column(name = "electricity_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal electricityFee = BigDecimal.ZERO;

    @Column(name = "water_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal waterFee = BigDecimal.ZERO;

    @Column(name = "management_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal managementFee = BigDecimal.ZERO;

    @Column(name = "sanitation_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sanitationFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "bill_status") // <--- THÊM DÒNG NÀY
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private BillStatus status = BillStatus.UNPAID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "due_date", insertable = false, updatable = false)
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}

package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentApartmentRepository extends JpaRepository<ResidentApartment, Long> {

    Page<ResidentApartment> findByApartment_RoomNumberContainingIgnoreCaseAndIsActiveTrue(String roomNumber,
            Pageable pageable);

    Optional<ResidentApartment> findByResidentIdAndApartmentIdAndIsActiveTrue(Long residentId, Long apartmentId);

    long countByApartmentIdAndIsActiveTrue(Long apartmentId);

    Page<ResidentApartment> findByIsActiveTrue(Pageable pageable);

    // Lấy danh sách cư dân đang ở trong 1 phòng
    List<ResidentApartment> findByApartmentIdAndIsActiveTrue(Long apartmentId);

    // Kiểm tra xem phòng này đã có người đứng tên thuê (TENANT) đang active chưa
    boolean existsByApartmentIdAndRoleAndIsActiveTrue(Long apartmentId, String role);

    boolean existsByApartmentIdAndIsHeadTrueAndIsActiveTrue(Long apartmentId);

    boolean existsByResidentIdAndIsActiveTrue(Long residentId);

    // Lấy tất cả các hợp đồng/quan hệ với căn hộ đang còn hiệu lực của 1 cư dân
    List<ResidentApartment> findByResident_IdAndIsActiveTrue(Long residentId);

    // Lấy tất cả người đang ở trong 1 căn hộ (còn hiệu lực)
    List<ResidentApartment> findByApartment_IdAndIsActiveTrue(Long apartmentId);

    Optional<ResidentApartment> findByApartmentIdAndIsHeadTrueAndIsActiveTrue(Long apartmentId);

    // Tìm người thuê (TENANT) đang hoạt động để gửi hóa đơn tiền nhà
    @Query("SELECT ra FROM ResidentApartment ra " +
            "WHERE ra.apartment.id = :aptId AND ra.role = 'TENANT' AND ra.isActive = true")
    Optional<ResidentApartment> findActiveTenant(@Param("aptId") Long aptId);

    // Tìm chủ sở hữu (OWNER) để biết tiền này thuộc về ai (Snapshot)
    @Query("SELECT ra.resident FROM ResidentApartment ra " +
            "WHERE ra.apartment.id = :aptId AND ra.role = 'OWNER' AND ra.isActive = true")
    Optional<Resident> findActiveOwner(@Param("aptId") Long aptId);
}
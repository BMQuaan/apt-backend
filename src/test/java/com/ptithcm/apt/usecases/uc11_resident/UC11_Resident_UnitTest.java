package com.ptithcm.apt.usecases.uc11_resident;

import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.impl.ResidentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC04_Resident_UnitTest - Kiểm thử Logic Nghiệp vụ")
public class UC11_Resident_UnitTest {

    @Mock
    private ResidentRepository residentRepository;
    @Mock
    private ResidentApartmentRepository residentApartmentRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResidentServiceImpl residentService;

    @Test
    @DisplayName("TC-02: Branch - Thêm thành viên nhưng Căn hộ chưa có Người đại diện/Chủ hộ")
    void testAddMember_Fail_NoHeadContract() {
        Apartment apt = new Apartment();
        apt.setId(101L);
        when(apartmentRepository.findByRoomNumber("101")).thenReturn(Optional.of(apt));

        // Không tìm thấy Hợp đồng chủ hộ nào đang Active
        when(residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(101L))
                .thenReturn(Optional.empty());

        MemberRequest req = new MemberRequest();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> residentService.addMemberToApartment("101", req));

        assertTrue(ex.getMessage().contains("Phòng này chưa có người thuê/chủ hộ"));
    }

    @Test
    @DisplayName("TC-04: Branch - Thêm thành viên: CCCD đã bị người khác đăng ký (Khác Email)")
    void testAddMember_Fail_CccdExistsWithOtherEmail() {
        Apartment apt = new Apartment();
        when(apartmentRepository.findByRoomNumber("101")).thenReturn(Optional.of(apt));
        when(residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(any()))
                .thenReturn(Optional.of(new com.ptithcm.apt.entity.ResidentApartment()));

        // Giả lập Email chưa có, nhưng CCCD đã có trong DB
        when(residentRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(residentRepository.findByCitizenIdentity(anyString())).thenReturn(Optional.of(new Resident()));

        MemberRequest req = new MemberRequest();
        req.setFullName("Nguyen Van A");
        req.setDob(java.time.LocalDate.of(2000, 1, 1));
        req.setPhone("0123456789");
        req.setCitizenIdentity("123456789012");
        req.setEmail("test@gmail.com");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> residentService.addMemberToApartment("101", req));

        assertEquals("Căn cước công dân này đã tồn tại với Email khác!", ex.getMessage());
    }

    @Test
    @DisplayName("TC-08: Side Effect - Cập nhật Email Cư dân thành công -> Kéo theo thay đổi Username trong bảng User")
    void testUpdateResident_Success_CascadeUpdateUser() {
        User user = new User();
        user.setUsername("old_email@gmail.com");

        Resident resident = new Resident();
        resident.setId(1L);
        resident.setEmail("old_email@gmail.com");
        resident.setUser(user); // Cư dân có tài khoản User liên kết

        when(residentRepository.findById(1L)).thenReturn(Optional.of(resident));

        // Trùng hợp Email mới không bị ai dùng
        when(residentRepository.existsByEmail("new_email@gmail.com")).thenReturn(false);
        when(residentRepository.save(any())).thenReturn(resident);
        when(residentApartmentRepository.findAllByResidentIdAndIsActiveTrue(1L))
                .thenReturn(java.util.Collections.emptyList());

        // Sử dụng Setter do DTO không có AllArgsConstructor
        UpdateResidentRequest req = new UpdateResidentRequest();
        req.setEmail("new_email@gmail.com");

        residentService.updateResident(1L, req);

        // Xác nhận Email trong Resident đổi
        assertEquals("new_email@gmail.com", resident.getEmail());
        // Xác nhận SIDE EFFECT: Username trong bảng User cũng được tự động update
        assertEquals("new_email@gmail.com", user.getUsername());

        verify(userRepository, times(1)).save(user); // Đảm bảo hàm lưu User được gọi
    }
}
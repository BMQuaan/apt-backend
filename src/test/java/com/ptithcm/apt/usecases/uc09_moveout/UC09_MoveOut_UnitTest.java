package com.ptithcm.apt.usecases.uc09_moveout;

import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.impl.ResidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC03_MoveOut_UnitTest - Kiểm thử Logic Trả phòng")
public class UC09_MoveOut_UnitTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private ResidentApartmentRepository residentApartmentRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResidentServiceImpl residentService;

    private ResidentApartment raHead;
    private Apartment apartment;
    private Resident resident;

    @BeforeEach
    void setUp() {
        apartment = new Apartment();
        apartment.setId(101L);
        apartment.setStatus("RENTED");

        User user = new User();
        user.setIsActive(true);

        resident = new Resident();
        resident.setId(1L);
        resident.setUser(user);

        raHead = new ResidentApartment();
        raHead.setApartment(apartment);
        raHead.setResident(resident);
        raHead.setIsHead(true); // Là chủ hộ
        raHead.setIsActive(true);
    }

    @Test
    @DisplayName("TC-03: Branch - Căn hộ vẫn còn hóa đơn chưa thanh toán (UNPAID)")
    void testMoveOut_Fail_UnpaidBills() {
        when(billRepository.existsByApartmentIdAndStatus(101L, BillStatus.UNPAID)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> residentService.moveOutResident(1L, 101L));

        assertEquals("Không thể chuyển đi! Căn hộ này vẫn còn hóa đơn chưa thanh toán.", ex.getMessage());
        verify(residentApartmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Branch - Chuyển đi thành công đối với Chủ hộ (isHead = true)")
    void testMoveOut_Success_IsHead() {
        when(billRepository.existsByApartmentIdAndStatus(101L, BillStatus.UNPAID)).thenReturn(false);
        when(residentApartmentRepository.findByResidentIdAndApartmentIdAndIsActiveTrue(1L, 101L))
                .thenReturn(Optional.of(raHead));

        // Giả lập danh sách các thành viên trong phòng (gồm cả chủ hộ)
        when(residentApartmentRepository.findByApartmentIdAndIsActiveTrue(101L))
                .thenReturn(List.of(raHead));

        // Giả lập người này không còn thuê phòng nào khác
        when(residentApartmentRepository.existsByResidentIdAndIsActiveTrue(1L)).thenReturn(false);

        residentService.moveOutResident(1L, 101L);

        // Xác nhận Hợp đồng bị vô hiệu hóa
        assertFalse(raHead.getIsActive());
        assertNotNull(raHead.getContractEnd());
        verify(residentApartmentRepository, times(1)).saveAll(anyList());

        // Xác nhận trạng thái Căn hộ trở về Trống
        assertEquals("AVAILABLE", apartment.getStatus());
        verify(apartmentRepository, times(1)).save(apartment);

        // Xác nhận tài khoản User bị xóa
        verify(userRepository, times(1)).delete(resident.getUser());
    }
}
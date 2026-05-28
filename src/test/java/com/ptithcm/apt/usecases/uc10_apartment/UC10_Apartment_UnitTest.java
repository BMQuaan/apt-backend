package com.ptithcm.apt.usecases.uc10_apartment;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.service.impl.ApartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC02_Apartment_UnitTest - Kiểm thử Đơn vị Logic Nghiệp vụ")
public class UC10_Apartment_UnitTest {

    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private ResidentApartmentRepository residentApartmentRepository;
    @InjectMocks
    private ApartmentServiceImpl apartmentService;

    private ApartmentRequest request;

    @BeforeEach
    void setUp() {
        request = new ApartmentRequest("201", 2, new BigDecimal("50.5"), "AVAILABLE");
    }

    @Test
    @DisplayName("TC-06: Branch - Sai định dạng đầu số phòng (VD: Tầng 3 nhưng gõ phòng 401)")
    void testCreateApartment_Fail_WrongFormat() {
        request.setFloor(3);
        request.setRoomNumber("401"); // Sai định dạng (đầu số phải là 30x)

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apartmentService.createApartment(request));

        assertTrue(ex.getMessage().contains("Wrong room number format"));
        verify(apartmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08: Branch - Số phòng đã tồn tại trong Database")
    void testCreateApartment_Fail_RoomExists() {
        when(apartmentRepository.existsByRoomNumber("201")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apartmentService.createApartment(request));

        assertEquals("The room number already exists!", ex.getMessage());
    }

    @Test
    @DisplayName("TC-13: Branch - Không cho đổi phòng về Trống (AVAILABLE) nếu vẫn còn Cư dân đang ở")
    void testUpdateApartment_Fail_ActiveResidents() {
        Apartment existingApt = new Apartment();
        existingApt.setId(1L);
        existingApt.setRoomNumber("201");
        existingApt.setFloor(2);
        existingApt.setArea(new BigDecimal("50.5"));
        existingApt.setStatus("RENTED"); // Trạng thái hiện tại đang cho thuê

        request.setStatus("AVAILABLE"); // Yêu cầu đổi về trống

        when(apartmentRepository.findById(1L)).thenReturn(Optional.of(existingApt));
        // Giả lập phòng này vẫn đang có người ở (isActive = true)
        when(residentApartmentRepository.existsByApartmentIdAndIsActiveTrue(1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apartmentService.updateApartment(1L, request));

        assertTrue(ex.getMessage()
                .contains("Không thể chuyển căn hộ về trạng thái trống (AVAILABLE) vì vẫn đang có cư dân"));
    }
}
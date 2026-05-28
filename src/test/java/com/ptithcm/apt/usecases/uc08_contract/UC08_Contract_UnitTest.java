package com.ptithcm.apt.usecases.uc08_contract;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.service.impl.ContractServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC01_Contract_UnitTest - Kiểm thử Đơn vị")
public class UC08_Contract_UnitTest {

    @Nested
    @DisplayName("1. Kiểm thử Logic Nghiệp vụ (ContractServiceImpl)")
    class ServiceLogicTest {

        @Mock
        private ApartmentRepository apartmentRepository;
        @Mock
        private ResidentRepository residentRepository;
        @Mock
        private ResidentApartmentRepository residentApartmentRepository;

        @InjectMocks
        private ContractServiceImpl contractService;

        private ContractRequest request;

        @BeforeEach
        void setUp() {
            request = new ContractRequest("Nguyen Van A", LocalDate.of(1995, 1, 1),
                    "0123456789", "123456789012", "a@gmail.com", 1L, "TENANT",
                    new BigDecimal("5000000"), new BigDecimal("5000000"),
                    LocalDate.now(), LocalDate.now().plusYears(1));
        }

        @Test
        @DisplayName("TC-05: Branch - Khách hàng chưa đủ 18 tuổi")
        void testCreateContract_Fail_Under18() {
            request.setDob(LocalDate.now().minusYears(17));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> contractService.createContract(request));

            assertEquals("Người đại diện lập hợp đồng phải từ đủ 18 tuổi trở lên!", exception.getMessage());
            verify(apartmentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("TC-06: Branch - Căn hộ không tồn tại")
        void testCreateContract_Fail_ApartmentNotFound() {
            when(apartmentRepository.findById(1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> contractService.createContract(request));

            assertTrue(exception.getMessage().contains("Không tìm thấy phòng với ID"));
        }

        @Test
        @DisplayName("TC-07: Branch - Thuê phòng đang trạng thái không hợp lệ (MAINTENANCE)")
        void testCreateContract_Fail_InvalidApartmentStatus() {
            Apartment apt = new Apartment();
            apt.setId(1L);
            apt.setStatus("MAINTENANCE");
            when(apartmentRepository.findById(1L)).thenReturn(Optional.of(apt));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> contractService.createContract(request));

            assertEquals("Chỉ có thể thuê căn hộ đang trống hoặc căn hộ đã có chủ sở hữu!", exception.getMessage());
        }
    }
}
package com.ptithcm.apt.usecases.uc07_view_profile;

import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC07_ViewProfile_UnitTest - Kiểm thử Đơn vị (Profile Logic)")
public class UC07_ViewProfile_UnitTest {

    @Mock
    private UserService userService;
    @Mock
    private ResidentService residentService;
    @Mock
    private ResidentApartmentService residentApartmentService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User mockUser;
    private Resident mockResident;
    private Apartment mockApartment;

    @BeforeEach
    void setUp() {
        // Giả lập SecurityContext
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("resident@gmail.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockUser = new User();
        mockUser.setId(1L);

        mockResident = new Resident();
        mockResident.setId(100L);
        mockResident.setFullName("Nguyen Van A");

        mockApartment = new Apartment();
        mockApartment.setId(999L);
        mockApartment.setRoomNumber("A-101");

        // Khi hàm getCurrentResident() chạy, nó sẽ trả về mockResident
        when(userService.findByUsername("resident@gmail.com")).thenReturn(mockUser);
        when(residentService.findByUserId(1L)).thenReturn(mockResident);
    }

    @Test
    @DisplayName("TC-06: Người dùng chưa được gán vào bất kỳ căn hộ nào")
    void testGetDashboard_NoApartment() {
        when(residentApartmentService.findActiveByResidentId(100L)).thenReturn(Collections.emptyList());

        ProfileDashboardResponse response = profileService.getProfileDashboard();

        // Kiểm tra thông tin cá nhân lấy đúng
        assertNotNull(response.personalInfo());
        assertEquals("Nguyen Van A", response.personalInfo().fullName());
        
        // Sống lang thang, chưa gán vào đâu
        assertNull(response.livingApartment(), "Living Apartment phải là null");
        assertTrue(response.ownedApartments().isEmpty(), "Owned Apartments phải rỗng");
        assertTrue(response.familyMembers().isEmpty(), "Family Members phải rỗng");
    }

    @Test
    @DisplayName("TC-04: Đang thuê căn hộ (TENANT) -> Lọc đúng thông tin Living Apartment")
    void testGetDashboard_TenantRole() {
        ResidentApartment tenantLink = new ResidentApartment();
        tenantLink.setResident(mockResident);
        tenantLink.setApartment(mockApartment);
        tenantLink.setRole("TENANT");
        tenantLink.setIsHead(false); // Tenant không nhất thiết phải là isHead mới được coi là đang ở

        when(residentApartmentService.findActiveByResidentId(100L)).thenReturn(Collections.singletonList(tenantLink));
        // Vì đã xác định được livingAptId, code sẽ đi tìm family members. Trả về rỗng để bỏ qua.
        when(residentApartmentService.findActiveByApartmentId(999L)).thenReturn(Collections.singletonList(tenantLink));

        ProfileDashboardResponse response = profileService.getProfileDashboard();

        assertNotNull(response.livingApartment(), "Phải lấy ra được căn hộ đang thuê");
        assertEquals("A-101", response.livingApartment().roomNumber());
        assertEquals("TENANT", response.livingApartment().role());
        
        assertTrue(response.ownedApartments().isEmpty(), "Không được có căn hộ sở hữu nào");
    }

    @Test
    @DisplayName("TC-05: Chủ hộ đang ở (OWNER + isHead=true) -> Vừa ở, vừa sở hữu")
    void testGetDashboard_OwnerHeadRole() {
        ResidentApartment ownerLink = new ResidentApartment();
        ownerLink.setResident(mockResident);
        ownerLink.setApartment(mockApartment);
        ownerLink.setRole("OWNER");
        ownerLink.setIsHead(true); // Quyết định xem có đang ở trong căn hộ này không

        when(residentApartmentService.findActiveByResidentId(100L)).thenReturn(Collections.singletonList(ownerLink));
        when(residentApartmentService.findActiveByApartmentId(999L)).thenReturn(Collections.singletonList(ownerLink));

        ProfileDashboardResponse response = profileService.getProfileDashboard();

        assertNotNull(response.livingApartment(), "Phải lấy ra được vì là Chủ hộ (isHead=true)");
        assertEquals(1, response.ownedApartments().size(), "Phải có 1 căn hộ nằm trong danh sách sở hữu");
    }

    @Test
    @DisplayName("TC-07: Lọc danh sách thành viên gia đình (Skip bản thân mình)")
    void testGetDashboard_FamilyMembersList() {
        ResidentApartment myLink = new ResidentApartment();
        myLink.setResident(mockResident); // Bản thân mình
        myLink.setApartment(mockApartment);
        myLink.setRole("TENANT");

        // Tạo ra 1 người vợ ở cùng
        Resident mockWife = new Resident();
        mockWife.setId(101L);
        mockWife.setFullName("Tran Thi B");

        ResidentApartment wifeLink = new ResidentApartment();
        wifeLink.setResident(mockWife);
        wifeLink.setApartment(mockApartment);
        wifeLink.setRole("MEMBER");

        when(residentApartmentService.findActiveByResidentId(100L)).thenReturn(Collections.singletonList(myLink));
        
        // Khi gọi hàm tìm tất cả những người đang sống trong A-101 -> Trả về cả mình và vợ
        when(residentApartmentService.findActiveByApartmentId(999L)).thenReturn(Arrays.asList(myLink, wifeLink));

        ProfileDashboardResponse response = profileService.getProfileDashboard();

        // Kiểm tra danh sách Family Members
        assertEquals(1, response.familyMembers().size(), "Danh sách gia đình chỉ trả về 1 người (đã bỏ qua bản thân mình)");
        assertEquals("Tran Thi B", response.familyMembers().get(0).fullName());
    }
}

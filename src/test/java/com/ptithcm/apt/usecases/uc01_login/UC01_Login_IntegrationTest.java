package com.ptithcm.apt.usecases.uc01_login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
// Tự động cấu hình MockMvc để giả lập các request HTTP gửi đến Controller
@AutoConfigureMockMvc
@DisplayName("UC01_Login_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC01_Login_IntegrationTest {

    // Công cụ đóng vai trò như Postman để gọi API
    @Autowired
    private MockMvc mockMvc;

    // Công cụ dùng để Serialize đối tượng LoginRequest thành chuỗi JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("TC-09: Đăng nhập thành công với tài khoản ĐÚNG trong Database")
    void testLogin_Success_RealDatabase() throws Exception {
        // tài khoản CÓ THẬT trong DB
        LoginRequest request = new LoginRequest("admin@gmail.com", "admin123");

        // Gửi một HTTP POST request đến API login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON) // Định dạng gửi đi là JSON
                        .content(objectMapper.writeValueAsString(request))) // Ép kiểu Object thành JSON String
                // Kiểm tra mã HTTP trả về phải là 200 OK
                .andExpect(status().isOk())
                // Kiểm tra các trường dữ liệu trong Body JSON trả về (Sử dụng $.tên_trường)
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.username").value("admin@gmail.com"));
    }

    @Test
    @DisplayName("TC-07: Cố tình đăng nhập bằng sai Password")
    void testLogin_Fail_WrongPassword() throws Exception {
        // Tài khoản có thật nhưng mật khẩu sai
        LoginRequest request = new LoginRequest("admin@gmail.com", "sai_mat_khau_ne");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Spring Security phải chặn lại và trả về mã 401 Unauthorized
                .andExpect(status().isUnauthorized()) 
                // Đồng thời Body phải chứa thông báo lỗi
                .andExpect(jsonPath("$.message").exists()); 
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-05, EP - Bỏ trống Email, '', 123456",
            "TC-06, EP - Bỏ trống Password, admin@gmail.com, ''",
            "TC-03, BVA - Password < 6 ký tự, admin@gmail.com, 12345"
    })
    @DisplayName("Kiểm tra HTTP 400 Bad Request khi truyền dữ liệu sai định dạng")
    void testLogin_Fail_Validation(String testCaseId, String description, String username, String password) throws Exception {
        if (username != null && username.isEmpty()) username = null;
        if (password != null && password.isEmpty()) password = null;

        LoginRequest request = new LoginRequest(username, password);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Tầng Controller với Annotation @Valid sẽ tự động chặn và ném ra lỗi HTTP 400
                // Test kiểm chứng cơ chế chặn tự động này hoạt động tốt
                .andExpect(status().isBadRequest());
    }
}

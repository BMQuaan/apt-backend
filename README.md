# 🏢 APT Backend (Apartment Management System)

Đây là RESTful API Backend cho hệ thống Quản lý Chung cư (Apartment Management System). Dự án được xây dựng dựa trên framework **Spring Boot** kết hợp với **PostgreSQL**, cung cấp các API để quản lý cư dân, hợp đồng, dịch vụ, hóa đơn, và phân quyền người dùng.

---

## 🚀 Công nghệ sử dụng

- **Framework:** Spring Boot
- **Ngôn ngữ:** Java 21
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security & JWT (JSON Web Token)
- **API Docs:** Springdoc OpenAPI (Swagger 3)
- **Khác:** MapStruct (Object Mapping), JavaMailSender (Email), Google API Client
- **Testing:** JUnit 5, Mockito, MockMvc

---

## 🛠️ Cài đặt và Chạy dự án

### Yêu cầu hệ thống
- **Java 21** trở lên
- **PostgreSQL** (hoặc Docker để chạy PostgreSQL container)
- **Maven** (nếu không dùng Maven Wrapper tích hợp sẵn)

### 1. Cấu hình Database
Tạo một cơ sở dữ liệu PostgreSQL. Sau đó, cấu hình các thông số kết nối trong tệp `src/main/resources/application.properties` (hoặc `application.yml` / `.env` nếu có):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/apt_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 2. Khởi chạy ứng dụng
Chạy lệnh sau tại thư mục gốc của backend (`backend/apt`):

```bash
# Sử dụng Maven Wrapper
./mvnw spring-boot:run

# Hoặc chạy trên Windows
mvnw.cmd spring-boot:run
```

Server sẽ khởi chạy mặc định tại cổng `8080`.

---

## 📚 Tài liệu API (Swagger UI)

Khi ứng dụng đã chạy thành công, bạn có thể truy cập tài liệu API trực tiếp thông qua Swagger UI:

👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

---

## 🛡️ Phân quyền (Role-Based Access Control)

Hệ thống hỗ trợ quản lý truy cập dựa trên các vai trò:
- **`ROLE_ADMIN`**: Toàn quyền trên toàn hệ thống (Cấu hình dịch vụ, Quản lý tài khoản, v.v.).
- **`ROLE_STAFF`**: Truy cập các trang quản trị cơ bản, quản lý hợp đồng cư dân.
- **`ROLE_ACCOUNTANT`**: Chuyên trách quản lý module Hóa đơn & Thanh toán.
- **`ROLE_USER`**: Cư dân/Người thuê sử dụng ứng dụng để xem thông tin, thanh toán phí.

---

## 🧪 Testing (Kiểm thử)

Dự án tuân thủ chiến lược Test Pyramid:
- **Unit Tests:** Sử dụng `Mockito` để kiểm thử độc lập các class Business Logic (Services) và Validation (DTOs).
- **Integration Tests:** Sử dụng `MockMvc` để kiểm tra các API endpoints và luồng xử lý từ Controller -> Service -> Repository.

Lệnh chạy toàn bộ Test Suites:
```bash
# Linux/macOS
./mvnw test

# Windows
mvnw.cmd test
```

---

## 📂 Cấu trúc thư mục chuẩn

```text
backend/apt/src/main/java/com/ptithcm/apt/
├── AptApplication.java   # Main class khởi chạy ứng dụng Spring Boot
├── config/               # Chứa các file cấu hình (Security, Swagger, CORS, Beans, v.v.)
├── constant/             # Định nghĩa các hằng số tĩnh (Constants) sử dụng chung
├── controller/           # Chứa các REST API Endpoints, tiếp nhận và trả về dữ liệu (Controller Layer)
├── dto/                  # Data Transfer Objects (Payloads) và các cấu hình Validation (@Valid)
├── entity/               # Các lớp thực thể (Entities) ánh xạ trực tiếp với các bảng trong Database
├── enums/                # Định nghĩa các tập giá trị cố định (Enum) như Trạng thái, Vai trò...
├── exception/            # Xử lý ngoại lệ tập trung (@ControllerAdvice) và các Custom Exceptions
├── filter/               # Các bộ lọc (Filters), chủ yếu xử lý chặn bắt Request/Response (vd: JWT Filter)
├── mapper/               # Các Interface MapStruct hỗ trợ convert dữ liệu qua lại giữa Entity <-> DTO
├── repository/           # Giao tiếp với Database thông qua Spring Data JPA (Repository Layer)
├── service/              # Nơi xử lý các nghiệp vụ cốt lõi (Business Logic Layer)
└── utils/                # Các hàm, lớp tiện ích (Helper/Utils) hỗ trợ xử lý chung
```

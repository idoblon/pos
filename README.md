<div align="center">

# 🏪 POS — Point of Sale System

**A production-ready, multi-tenant Point of Sale REST API built with Spring Boot.**  
Manage stores, branches, products, employees, orders, refunds, and cashier shifts — all secured with JWT authentication.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.5-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.palletsprojects.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Stripe](https://img.shields.io/badge/Payments-Stripe-635BFF?style=flat-square&logo=stripe&logoColor=white)](https://stripe.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

</div>

---

## ✨ Overview

POS is a fully-featured backend system designed for retail businesses operating across multiple stores and branches. It exposes a clean RESTful API consumed by a frontend client (e.g., a React/Vue cashier dashboard).

The system supports a **hierarchical role model** — from platform admins down to branch-level cashiers — with JWT-based stateless authentication securing every protected endpoint.

---

## 🚀 Features

| Module | Capabilities |
|---|---|
| 🔐 **Authentication** | JWT signup & login, BCrypt password hashing, stateless sessions |
| 🏬 **Store Management** | Create, update, delete stores; moderate store status (PENDING → ACTIVE) |
| 🏢 **Branch Management** | Add and manage branches under a store |
| 📦 **Product & Category** | Full CRUD for products (SKU, MRP, selling price, brand, image) and categories |
| 🛒 **Orders** | Create orders with line items; filter by branch, cashier, customer, status, payment type |
| 💳 **Payments** | Cash, eSewa, and Khalti payment types supported |
| 👥 **Customers** | Register and look up customers tied to orders |
| 👨‍💼 **Employees** | Create store or branch employees with specific roles; update and remove |
| 📋 **Inventory** | Track stock levels per branch |
| 🔄 **Refunds** | Process refunds; query by cashier, branch, shift, or date range |
| ⏱️ **Shift Reports** | Start/end cashier shifts; auto-calculate net sales, total orders, refunds per shift |
| 📊 **Analytics** | Store & branch-level sales totals, charts, top products, payment breakdowns |

---

## 🏗️ Architecture

```
src/main/java/com/springboot/POS/
│
├── PosApplication.java          # Spring Boot entry point
│
├── configuration/               # Security & JWT
│   ├── SecurityConfig.java      # Filter chain, CORS, BCrypt, stateless sessions
│   ├── JwtProvider.java         # Token generation
│   ├── JwtValidator.java        # Token validation filter
│   └── JwtConstant.java         # JWT secret key constant
│
├── controller/                  # REST endpoints (14 controllers)
│   ├── AuthController.java      # POST /auth/signup, /auth/login
│   ├── StoreController.java     # /api/stores
│   ├── BranchController.java    # /api/branches
│   ├── ProductController.java   # /api/products
│   ├── CategoryController.java  # /api/categories
│   ├── OrderController.java     # /api/orders
│   ├── InventoryController.java # /api/inventory
│   ├── CustomerController.java  # /api/customers
│   ├── EmployeeController.java  # /api/employees
│   ├── RefundController.java    # /api/refunds
│   ├── shiftReportController.java # /api/shift-reports
│   ├── AnalyticsController.java # /api/analytics
│   ├── UserController.java      # /api/users
│   └── HomeController.java      # Health check
│
├── service/                     # Business logic interfaces + impl/
│
├── modal/                       # JPA entities
│   ├── User, Store, Branch
│   ├── Product, Category, Inventory
│   ├── Order, OrderItem, Customer
│   ├── Refund, ShiftReport, PaymentSummary
│   └── StoreContact
│
├── domain/                      # Enums
│   ├── UserRole                 # ADMIN, STORE_ADMIN, STORE_MANAGER, BRANCH_MANAGER, BRANCH_CASHIER, USER
│   ├── PaymentType              # CASH, ESEWA, KHALTI
│   ├── OrderStatus
│   └── StoreStatus              # PENDING, ACTIVE, ...
│
├── payload/
│   ├── dto/                     # Data Transfer Objects
│   └── response/                # ApiResponse, AuthResponse
│
├── mapper/                      # Entity ↔ DTO mappers
├── repository/                  # Spring Data JPA repositories
└── exceptions/                  # UserException and global error handling
```

---

## 🔑 Role Hierarchy

```
ROLE_ADMIN
  └── ROLE_STORE_ADMIN
        ├── ROLE_STORE_MANAGER
        └── ROLE_BRANCH_MANAGER
              └── ROLE_BRANCH_CASHIER
```

| Role | Access |
|---|---|
| `ROLE_ADMIN` | Platform-level super admin (`/api/super-admin/**`) |
| `ROLE_STORE_ADMIN` | Full control over own store and its branches |
| `ROLE_STORE_MANAGER` | Manage store employees and products |
| `ROLE_BRANCH_MANAGER` | Manage branch inventory and employees |
| `ROLE_BRANCH_CASHIER` | Create orders and manage their own shifts |

---

## ⚡ Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8.0+**

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/POS.git
cd POS
```

### 2. Configure the Database

Create a MySQL database:

```sql
CREATE DATABASE POS;
```

Update `src/main/resources/application.properties` with your credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/POS
spring.datasource.username=your_username
spring.datasource.password=your_password
```

> The schema is auto-generated on first run via `spring.jpa.hibernate.ddl-auto=update`.

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at **[http://localhost:8080](http://localhost:8080)**.

---

## 🌐 API Reference

All `/api/**` routes require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <your_jwt_token>
```

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/signup` | Register a new user |
| `POST` | `/auth/login` | Login and receive a JWT token |

### Core Resources

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/stores` | Create a new store |
| `GET` | `/api/stores` | List all stores |
| `PUT` | `/api/stores/{id}/moderate` | Change store status |
| `DELETE` | `/api/stores/{id}` | Delete a store |
| `POST` | `/api/products` | Add a product |
| `GET` | `/api/products/store/{storeId}` | Get products by store |
| `POST` | `/api/orders` | Create an order |
| `GET` | `/api/orders/branch/{branchId}` | Get orders by branch (filterable) |
| `GET` | `/api/orders/today/branch/{id}` | Get today's orders for a branch |
| `POST` | `/api/refunds` | Process a refund |
| `POST` | `/api/shift-reports/start` | Start a cashier shift |
| `PATCH` | `/api/shift-reports/end` | End a cashier shift |
| `GET` | `/api/shift-reports/current` | Get current shift progress |

### Analytics

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/store/{storeId}` | Store-wide totals |
| `GET` | `/api/analytics/store/{storeId}/range` | Totals by date range |
| `GET` | `/api/analytics/branch/{branchId}` | Branch-level totals |
| `GET` | `/api/analytics/store/{storeId}/sales-chart` | Sales chart data (monthly) |
| `GET` | `/api/analytics/store/{storeId}/top-products` | Best-selling products |
| `GET` | `/api/analytics/store/{storeId}/payment-summary` | Breakdown by payment method |

---

## 🛡️ Security

- **Stateless JWT authentication** — no server-side sessions
- **BCrypt** password hashing
- **CORS** pre-configured for `http://localhost:5173` (Vite) and `http://localhost:3000`
- All `/api/**` routes are authenticated; `/auth/**` is public
- Super-admin routes restricted to `ROLE_ADMIN`

---

## 🧩 Tech Stack

| Technology | Purpose |
|---|---|
| Spring Boot 3.1.5 | Core framework |
| Spring Data JPA + Hibernate | ORM & database access |
| Spring Security | Authentication & authorization |
| JJWT 0.12.6 | JWT token creation & validation |
| MySQL | Relational database |
| Stripe Java SDK | Payment processing integration |
| Spring Mail | Email notifications |
| Lombok | Boilerplate reduction |
| Spring Validation | Request validation (`@Email`, `@NotNull`, etc.) |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<div align="center">
  <sub>Built with ☕ Java & Spring Boot.</sub>
</div>

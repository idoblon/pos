# 🎨 POS Frontend Development Specifications

## 📋 Overview
This document provides complete frontend specifications for the POS system based on the Spring Boot backend API. The frontend should be a modern, responsive web application that consumes the REST API endpoints.

## 🏗️ Recommended Tech Stack

### Core Framework
- **React 18+** with TypeScript
- **Vite** for build tooling
- **React Router v6** for navigation
- **Axios** for API calls

### UI Framework
- **Material-UI (MUI)** or **Ant Design** or **Chakra UI**
- **Tailwind CSS** for custom styling
- **React Hook Form** for form management
- **Zod** for validation

### State Management
- **Zustand** or **Redux Toolkit** for global state
- **React Query/TanStack Query** for server state

### Additional Libraries
- **Chart.js** or **Recharts** for analytics
- **React Table** for data grids
- **date-fns** for date manipulation
- **React Hot Toast** for notifications

## 🔐 Authentication System

### Login/Signup Pages
```typescript
// API Endpoints
POST /auth/signup
POST /auth/login  
POST /auth/refresh

// Required Forms
interface LoginForm {
  email: string;
  password: string;
}

interface SignupForm {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  role: UserRole;
  storeId?: number;
  branchId?: number;
}
```

### JWT Token Management
- Store JWT in localStorage/sessionStorage
- Auto-refresh tokens before expiration
- Redirect to login on 401 errors
- Include `Authorization: Bearer <token>` in all API calls

## 👥 User Roles & Access Control

### Role Hierarchy
```typescript
enum UserRole {
  ROLE_ADMIN = "ROLE_ADMIN",
  ROLE_STORE_ADMIN = "ROLE_STORE_ADMIN", 
  ROLE_STORE_MANAGER = "ROLE_STORE_MANAGER",
  ROLE_BRANCH_MANAGER = "ROLE_BRANCH_MANAGER",
  ROLE_BRANCH_CASHIER = "ROLE_BRANCH_CASHIER",
  ROLE_USER = "ROLE_USER"
}
```

### Route Protection
- **Admin Dashboard**: `/admin/**` (ROLE_ADMIN only)
- **Store Management**: `/store/**` (STORE_ADMIN+)
- **Branch Operations**: `/branch/**` (BRANCH_MANAGER+)
- **Cashier Interface**: `/pos/**` (BRANCH_CASHIER+)

## 📱 Application Layout

### Main Navigation Structure
```
├── Dashboard (Role-based)
├── POS System (Cashiers)
├── Inventory Management
├── Orders & Sales
├── Customer Management
├── Employee Management
├── Analytics & Reports
├── Store/Branch Settings
└── Profile & Settings
```

### Responsive Design
- **Desktop**: Full sidebar navigation
- **Tablet**: Collapsible sidebar
- **Mobile**: Bottom navigation tabs

## 🏪 Core Modules & Pages

### 1. Dashboard (Role-based)
```typescript
// Different dashboards per role
- Admin: Platform overview, store approvals
- Store Admin: Store performance, branch management
- Branch Manager: Branch operations, staff management  
- Cashier: Shift status, quick actions
```

### 2. POS System (Cashier Interface)
```typescript
// Main POS Screen
interface POSInterface {
  productSearch: ProductSearch;
  cart: CartItem[];
  customerInfo: Customer;
  paymentMethod: PaymentType;
  currentShift: ShiftReport;
}

// API Endpoints
GET /api/products/store/{storeId}
POST /api/orders
GET /api/shift-reports/current
POST /api/shift-reports/start
PATCH /api/shift-reports/end
```

### 3. Store Management
```typescript
// Store CRUD Operations
GET /api/stores
POST /api/stores  
PUT /api/stores/{id}
DELETE /api/stores/{id}
PUT /api/stores/{id}/moderate

// Store Form Fields
interface StoreForm {
  brand: string;
  description: string;
  storeType: string;
  contact: {
    address: string;
    city: string;
    state: string;
    zipCode: string;
    phone: string;
    email: string;
  };
}
```

### 4. Branch Management
```typescript
// Branch Operations
GET /api/branches/store/{storeId}
POST /api/branches
PUT /api/branches/{id}
DELETE /api/branches/{id}

interface BranchForm {
  name: string;
  address: string;
  phone: string;
  storeId: number;
}
```

### 5. Product Management
```typescript
// Product CRUD
GET /api/products/store/{storeId}
POST /api/products
PUT /api/products/{id}
DELETE /api/products/{id}

interface ProductForm {
  name: string;
  sku: string;
  brand: string;
  categoryId: number;
  mrp: number;
  sellingPrice: number;
  description: string;
  imageUrl: string;
  storeId: number;
}
```

### 6. Inventory Management
```typescript
// Inventory Operations
GET /api/inventory/branch/{branchId}
POST /api/inventory
PUT /api/inventory/{id}

interface InventoryItem {
  productId: number;
  branchId: number;
  quantity: number;
  minStockLevel: number;
  maxStockLevel: number;
}
```

### 7. Order Management
```typescript
// Order Operations
GET /api/orders/branch/{branchId}
GET /api/orders/today/branch/{id}
POST /api/refunds

// Order Filters
interface OrderFilters {
  branchId?: number;
  cashierId?: number;
  customerId?: number;
  status?: OrderStatus;
  paymentType?: PaymentType;
  dateFrom?: string;
  dateTo?: string;
}
```

### 8. Customer Management
```typescript
// Customer Operations
GET /api/customers
POST /api/customers
PUT /api/customers/{id}

interface CustomerForm {
  name: string;
  email: string;
  phone: string;
  address: string;
}
```

### 9. Employee Management
```typescript
// Employee Operations
GET /api/employees/store/{storeId}
POST /api/employees
PUT /api/employees/{id}
DELETE /api/employees/{id}

interface EmployeeForm {
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  storeId?: number;
  branchId?: number;
}
```

### 10. Analytics Dashboard
```typescript
// Analytics Endpoints
GET /api/analytics/store/{storeId}
GET /api/analytics/store/{storeId}/range
GET /api/analytics/branch/{branchId}
GET /api/analytics/store/{storeId}/sales-chart
GET /api/analytics/store/{storeId}/top-products
GET /api/analytics/store/{storeId}/payment-summary
GET /api/analytics/branch/{branchId}/daily-comparison
GET /api/analytics/store/{storeId}/peak-hours

// Chart Components Needed
- Sales Trend Chart (Line/Bar)
- Payment Method Pie Chart  
- Top Products Table
- Peak Hours Heatmap
- Daily Comparison Cards
```

## 🎨 UI Components Library

### Common Components
```typescript
// Reusable Components
- DataTable with sorting/filtering
- SearchableSelect for products
- DateRangePicker
- ConfirmDialog
- LoadingSpinner
- ErrorBoundary
- NotificationToast
- RoleGuard (HOC)
- PrivateRoute (HOC)
```

### Form Components
```typescript
// Form Building Blocks
- FormInput
- FormSelect  
- FormTextarea
- FormCheckbox
- FormRadioGroup
- FormDatePicker
- FormNumberInput
- FormFileUpload
```

### Business Components
```typescript
// POS Specific Components
- ProductCard
- CartItem
- PaymentSelector
- CustomerLookup
- ShiftStatus
- InventoryAlert
- SalesMetrics
- OrderStatusBadge
```

## 📊 Data Models (TypeScript Interfaces)

### Core Entities
```typescript
interface User {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: UserRole;
  store?: Store;
  branch?: Branch;
  createdAt: string;
  lastLogin?: string;
}

interface Store {
  id: number;
  brand: string;
  description?: string;
  storeType?: string;
  status: StoreStatus;
  contact: StoreContact;
  storeAdmin: User;
  createdAt: string;
}

interface Branch {
  id: number;
  name: string;
  address: string;
  phone?: string;
  store: Store;
  createdAt: string;
}

interface Product {
  id: number;
  name: string;
  sku: string;
  brand?: string;
  category: Category;
  mrp: number;
  sellingPrice: number;
  description?: string;
  imageUrl?: string;
  store: Store;
  deleted: boolean;
}

interface Order {
  id: number;
  totalAmount: number;
  createdAt: string;
  branch: Branch;
  cashier: User;
  customer?: Customer;
  items: OrderItem[];
  paymentType: PaymentType;
  status: OrderStatus;
}

interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
  order: Order;
}

interface Customer {
  id: number;
  name: string;
  email?: string;
  phone?: string;
  address?: string;
}

interface ShiftReport {
  id: number;
  cashier: User;
  branch: Branch;
  startTime: string;
  endTime?: string;
  totalSales: number;
  totalOrders: number;
  totalRefunds: number;
  netSales: number;
}
```

### Enums
```typescript
enum UserRole {
  ROLE_ADMIN = "ROLE_ADMIN",
  ROLE_STORE_ADMIN = "ROLE_STORE_ADMIN",
  ROLE_STORE_MANAGER = "ROLE_STORE_MANAGER", 
  ROLE_BRANCH_MANAGER = "ROLE_BRANCH_MANAGER",
  ROLE_BRANCH_CASHIER = "ROLE_BRANCH_CASHIER",
  ROLE_USER = "ROLE_USER"
}

enum PaymentType {
  CASH = "CASH",
  ESEWA = "ESEWA", 
  KHALTI = "KHALTI"
}

enum OrderStatus {
  PENDING = "PENDING",
  COMPLETED = "COMPLETED",
  CANCELLED = "CANCELLED"
}

enum StoreStatus {
  PENDING = "PENDING",
  ACTIVE = "ACTIVE",
  SUSPENDED = "SUSPENDED"
}
```

## 🔧 API Integration

### Base Configuration
```typescript
// axios.config.ts
const API_BASE_URL = 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for JWT
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### API Service Layer
```typescript
// services/api.ts
export const authAPI = {
  login: (credentials: LoginForm) => 
    apiClient.post('/auth/login', credentials),
  signup: (userData: SignupForm) => 
    apiClient.post('/auth/signup', userData),
  refresh: () => 
    apiClient.post('/auth/refresh'),
};

export const storeAPI = {
  getAll: () => apiClient.get('/api/stores'),
  create: (store: StoreForm) => apiClient.post('/api/stores', store),
  update: (id: number, store: StoreForm) => 
    apiClient.put(`/api/stores/${id}`, store),
  delete: (id: number) => apiClient.delete(`/api/stores/${id}`),
  moderate: (id: number, status: StoreStatus) => 
    apiClient.put(`/api/stores/${id}/moderate`, { status }),
};

// Similar patterns for other entities...
```

## 📱 Page Layouts & Wireframes

### 1. POS Interface Layout
```
┌─────────────────────────────────────────────────────────┐
│ Header: Store Name | Cashier | Shift Status | Logout    │
├─────────────────────────────────────────────────────────┤
│ Product Search: [Search Box] [Barcode Scanner]          │
├─────────────────┬───────────────────────────────────────┤
│ Product Grid    │ Shopping Cart                         │
│ [Product Cards] │ ┌─────────────────────────────────┐   │
│                 │ │ Item 1    Qty: 2    $10.00     │   │
│                 │ │ Item 2    Qty: 1    $5.00      │   │
│                 │ └─────────────────────────────────┘   │
│                 │ Customer: [Select/Add Customer]       │
│                 │ Payment: [Cash][eSewa][Khalti]        │
│                 │ Total: $15.00                         │
│                 │ [Process Order] [Clear Cart]          │
└─────────────────┴───────────────────────────────────────┘
```

### 2. Dashboard Layout
```
┌─────────────────────────────────────────────────────────┐
│ Header: Logo | Navigation | User Menu                   │
├─────────────────────────────────────────────────────────┤
│ Sidebar        │ Main Content Area                      │
│ - Dashboard    │ ┌─────────────────────────────────┐    │
│ - POS          │ │ Metrics Cards                   │    │
│ - Inventory    │ │ [Sales] [Orders] [Customers]    │    │
│ - Orders       │ └─────────────────────────────────┘    │
│ - Customers    │ ┌─────────────────────────────────┐    │
│ - Analytics    │ │ Charts & Graphs                 │    │
│ - Settings     │ │                                 │    │
│                │ └─────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Development Phases

### Phase 1: Foundation (Week 1-2)
- [ ] Project setup with Vite + React + TypeScript
- [ ] Authentication system (login/signup/JWT handling)
- [ ] Basic routing and layout structure
- [ ] API client configuration
- [ ] Role-based access control

### Phase 2: Core POS (Week 3-4)
- [ ] POS interface for cashiers
- [ ] Product search and cart functionality
- [ ] Order processing and payment
- [ ] Shift management
- [ ] Basic inventory display

### Phase 3: Management (Week 5-6)
- [ ] Store and branch management
- [ ] Product and category CRUD
- [ ] Employee management
- [ ] Customer management
- [ ] Order history and refunds

### Phase 4: Analytics (Week 7-8)
- [ ] Analytics dashboard
- [ ] Sales charts and reports
- [ ] Inventory reports
- [ ] Performance metrics
- [ ] Export functionality

### Phase 5: Polish (Week 9-10)
- [ ] UI/UX improvements
- [ ] Mobile responsiveness
- [ ] Error handling and validation
- [ ] Performance optimization
- [ ] Testing and bug fixes

## 🎯 Key Features to Implement

### Must-Have Features
- ✅ JWT Authentication with auto-refresh
- ✅ Role-based access control
- ✅ Real-time POS interface
- ✅ Inventory management
- ✅ Order processing and history
- ✅ Basic analytics dashboard
- ✅ Responsive design

### Nice-to-Have Features
- 🔄 Real-time notifications
- 📱 PWA capabilities
- 🖨️ Receipt printing
- 📊 Advanced analytics
- 🔍 Advanced search and filters
- 📤 Data export (CSV/PDF)
- 🌙 Dark mode theme

## 📋 Environment Configuration

### Development Environment
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=POS System
VITE_JWT_STORAGE_KEY=pos_jwt_token
```

### Production Environment
```env
VITE_API_BASE_URL=https://your-api-domain.com
VITE_APP_NAME=POS System
VITE_JWT_STORAGE_KEY=pos_jwt_token
```

## 🧪 Testing Strategy

### Unit Testing
- Component testing with React Testing Library
- API service testing with MSW (Mock Service Worker)
- Utility function testing with Jest

### Integration Testing
- User flow testing with Cypress
- API integration testing
- Authentication flow testing

### E2E Testing
- Complete POS workflow
- Order processing end-to-end
- Multi-role user scenarios

This specification provides a complete roadmap for developing a modern, feature-rich frontend that perfectly integrates with your Spring Boot POS backend system.
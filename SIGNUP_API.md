# Signup API Documentation

## Endpoint
`POST /auth/signup`

## Purpose
This endpoint is ONLY for Store Admin registration. When a new business owner wants to register their store on the platform, they use this endpoint.

## Important Notes
- ✅ Only `ROLE_STORE_ADMIN` can signup through this endpoint
- ❌ Other roles (Store Manager, Branch Manager, Cashier) MUST be added by the Store Admin through Employee Management endpoints
- ✅ Creates both the Store and Store Admin user in one request
- ✅ Store is automatically set to ACTIVE status

## Request Body

```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "phone": "9841234567",
  "role": "ROLE_STORE_ADMIN",
  
  "storeName": "My Retail Store",
  "storeDescription": "A premium retail store offering quality products",
  "storeType": "RETAIL",
  "storeEmail": "store@example.com",
  "storePhone": "9851234567",
  "storeAddress": "123 Main Street, Kathmandu, Nepal"
}
```

## Field Descriptions

### User Fields (All Required)
- `fullName` (string, required): Full name of the store admin
- `email` (string, required): Valid email address (must be unique)
- `password` (string, required): User password
- `phone` (string, optional): Contact phone number
- `role` (string, required): Must be `ROLE_STORE_ADMIN`

### Store Fields
- `storeName` (string, required): Name/Brand of the store
- `storeDescription` (string, optional): Description of the store
- `storeType` (string, optional): Type of store (defaults to "RETAIL")
  - Examples: "RETAIL", "WHOLESALE", "RESTAURANT", "PHARMACY", "GROCERY", "ELECTRONICS", "CLOTHING"
- `storeEmail` (string, optional): Store contact email
- `storePhone` (string, optional): Store contact phone
- `storeAddress` (string, optional): Store physical address

## Response

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Registered Successfully",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "9841234567",
    "role": "ROLE_STORE_ADMIN",
    "storeId": 1,
    "branchId": null
  },
  "role": "ROLE_STORE_ADMIN",
  "storeId": 1,
  "branchId": null,
  "storeName": "My Retail Store"
}
```

## Frontend Signup Form Fields

### Store Admin Information
1. Full Name (text input, required)
2. Email (email input, required)
3. Password (password input, required)
4. Phone (tel input, optional)

### Store Information
5. Store Name (text input, required)
6. Store Description (textarea, optional)
7. Store Type (select dropdown, optional)
   - Options: RETAIL, WHOLESALE, RESTAURANT, PHARMACY, GROCERY, ELECTRONICS, CLOTHING, OTHER
8. Store Email (email input, optional)
9. Store Phone (tel input, optional)
10. Store Address (textarea, optional)

**Note:** Role field should be hidden and automatically set to `ROLE_STORE_ADMIN`

## Validation Rules

1. Email must be unique
2. Email must be valid format
3. Password minimum length (implement as needed)
4. Phone should be valid format (optional)
5. Store name is required
6. Role is automatically set to ROLE_STORE_ADMIN (no user input)

---

## Adding Employees (Store Manager, Branch Manager, Cashier)

After the Store Admin signs up and logs in, they can add employees using:

### Employee Management Endpoints
- `POST /api/employees/store/{storeId}` - Add store-level employee (Store Manager)
- `POST /api/employees/branch/{branchId}` - Add branch-level employee (Branch Manager, Cashier)

### Example: Add Store Manager
```json
POST /api/employees/store/1
{
  "fullName": "Jane Smith",
  "email": "jane@example.com",
  "password": "TempPass123",
  "phone": "9841234568",
  "role": "ROLE_STORE_MANAGER"
}
```

### Example: Add Branch Cashier
```json
POST /api/employees/branch/1
{
  "fullName": "Mike Johnson",
  "email": "mike@example.com",
  "password": "TempPass123",
  "phone": "9841234569",
  "role": "ROLE_BRANCH_CASHIER"
}
```

## Error Responses

### 400 Bad Request
```json
{
  "message": "Full name is required"
}
```

```json
{
  "message": "email id already registered !"
}
```

```json
{
  "message": "Store name is required for store admin"
}
```

```json
{
  "message": "Only store admin can signup. Other roles must be added by admin through employee management."
}
```

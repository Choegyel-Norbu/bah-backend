# Auth API — User registration & sign-in

Base URL: **`/api/v1/auth`**  
All requests and responses use **JSON** (`Content-Type: application/json`).

---

## 1. User registration

Register a new user account.

**Endpoint:** `POST /api/v1/auth/register`  
**Auth:** None (public)

### Request body

| Field        | Type   | Required | Constraints                          | Description              |
|-------------|--------|----------|--------------------------------------|--------------------------|
| `email`     | string | Yes      | Valid email format                   | User email (unique)      |
| `password`  | string | Yes      | 8–100 characters                     | Account password         |
| `firstName` | string | Yes      | Max 100 characters                   | First name               |
| `lastName`  | string | Yes      | Max 100 characters                   | Last name                |
| `phoneNumber` | string | No     | Max 20 characters                    | Optional phone number    |

### Example request

```json
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

### Success response

**Status:** `201 Created`

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "+1234567890",
      "role": "CUSTOMER",
      "emailVerified": false,
      "createdAt": "2026-02-13T12:00:00"
    }
  },
  "timestamp": "2026-02-13T12:00:00"
}
```

| Field          | Type   | Description                                                       |
|----------------|--------|-------------------------------------------------------------------|
| `accessToken`  | string | JWT for authenticated API requests                                |
| `refreshToken` | string | JWT to obtain a new access token when expired                     |
| `tokenType`    | string | Always `"Bearer"`                                                 |
| `expiresIn`    | number | Access token lifetime in **seconds** (e.g. 900 for 15 min)       |
| `user`         | object | Logged-in user details (see [User object](#user-object-in-auth-response)) |

### Error responses

| Status  | Condition              | Example message / cause                    |
|---------|------------------------|--------------------------------------------|
| `400`   | Validation error       | Invalid email, short password, missing fields |
| `409`   | Email already exists   | User already registered with this email    |

---

## 2. User sign-in (login)

Authenticate and receive tokens.

**Endpoint:** `POST /api/v1/auth/login`  
**Auth:** None (public)

### Request body

| Field      | Type   | Required | Constraints    | Description   |
|------------|--------|----------|----------------|---------------|
| `email`    | string | Yes      | Valid email    | User email    |
| `password` | string | Yes      | Non-blank      | Password      |

### Example request

```json
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

### Success response

**Status:** `200 OK`

Response shape is the same as registration: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, and **`user`** (id, email, firstName, lastName, phoneNumber, **role**, emailVerified, createdAt). See [User object](#user-object-in-auth-response).

### Error responses

| Status  | Condition           | Example message / cause                                           |
|---------|---------------------|-------------------------------------------------------------------|
| `400`   | Validation error    | Invalid email or missing password                                 |
| `401`   | Invalid credentials | Wrong email or password                                           |
| `403`   | Email not verified  | Please verify your email before signing in. Check your inbox for the verification link. |

---

### User object in auth response

Login, registration, and refresh-token responses include a **`user`** object so the client can show the current user and role without calling `/api/v1/users/me`.

| Field          | Type    | Description                          |
|----------------|---------|--------------------------------------|
| `id`           | number  | User ID                              |
| `email`        | string  | User email                           |
| `firstName`    | string  | First name (nullable)                |
| `lastName`     | string  | Last name (nullable)                 |
| `phoneNumber`  | string  | Phone (nullable)                     |
| `role`         | string  | `"CUSTOMER"` or `"ADMIN"`            |
| `emailVerified`| boolean | Whether email is verified           |
| `createdAt`    | string  | ISO-8601 registration time           |

---

## 3. Using the access token

After registration or login, send the access token on protected endpoints:

```
Authorization: Bearer <accessToken>
```

Example:

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 4. Refresh token (optional)

To get a new access token without re-entering password:

**Endpoint:** `POST /api/v1/auth/refresh-token`  
**Auth:** None (body contains refresh token)

**Request body:**

```json
{
  "refreshToken": "<refreshToken from login/register>"
}
```

**Success:** `200 OK` — same `data` shape as login (new `accessToken`, `refreshToken`, `expiresIn`, and `user`).

---

---

## 5. Verify email

Verify the user's email address using the token sent in the registration verification email.

**Endpoint:** `POST /api/v1/auth/verify-email`  
**Auth:** None (public)

### Request body

| Field   | Type   | Required | Description                      |
|---------|--------|----------|----------------------------------|
| `token` | string | Yes      | Token from the verification email |

### Example request

```json
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "token": "a1b2c3d4e5f6..."
}
```

### Success response

**Status:** `200 OK` — `"Email verified successfully"`

### Error responses

| Status  | Condition              | Example message                                   |
|---------|------------------------|---------------------------------------------------|
| `400`   | Invalid/expired token  | Invalid or expired verification token             |
| `400`   | Token already used     | Verification token has already been used          |

---

## 6. Forgot password

Request a password reset email. If the email exists, a reset link is sent. Always returns success (no user enumeration).

**Endpoint:** `POST /api/v1/auth/forgot-password`  
**Auth:** None (public)

### Request body

| Field   | Type   | Required | Constraints     | Description   |
|---------|--------|----------|-----------------|---------------|
| `email` | string | Yes      | Valid email     | User email    |

### Example request

```json
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "john.doe@example.com"
}
```

### Success response

**Status:** `200 OK` — `"If an account exists with this email, you will receive a password reset link."`

---

## 7. Reset password

Reset the password using the token from the password reset email. Token expires in 1 hour (configurable).

**Endpoint:** `POST /api/v1/auth/reset-password`  
**Auth:** None (public)

### Request body

| Field         | Type   | Required | Constraints     | Description   |
|---------------|--------|----------|-----------------|---------------|
| `token`       | string | Yes      | Non-blank       | Token from reset email |
| `newPassword` | string | Yes      | 8–100 chars     | New password  |

### Example request

```json
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "token": "a1b2c3d4e5f6...",
  "newPassword": "NewSecurePass123"
}
```

### Success response

**Status:** `200 OK` — `"Password has been reset successfully"`

### Error responses

| Status  | Condition              | Example message                        |
|---------|------------------------|----------------------------------------|
| `400`   | Invalid/expired token  | Invalid or expired reset token         |
| `400`   | Token already used     | Reset token has already been used      |
| `400`   | Validation error       | Password must be 8-100 characters      |

---

## Summary

| Action          | Method | Path                        | Auth   |
|-----------------|--------|-----------------------------|--------|
| Register        | POST   | `/api/v1/auth/register`     | Public |
| Sign-in         | POST   | `/api/v1/auth/login`        | Public |
| Refresh         | POST   | `/api/v1/auth/refresh-token`| Public |
| Verify email    | POST   | `/api/v1/auth/verify-email` | Public |
| Forgot password | POST   | `/api/v1/auth/forgot-password` | Public |
| Reset password  | POST   | `/api/v1/auth/reset-password`  | Public |

Base URL when running locally (default port 8081): **`http://localhost:8081`**

### Email configuration

To send verification and password-reset emails, configure `application.yml` or environment variables:

- `spring.mail.username` / `MAIL_USERNAME` — SMTP username (e.g. Gmail address)
- `spring.mail.password` / `MAIL_PASSWORD` — SMTP password (e.g. Gmail app password)
- `app.frontend-base-url` / `FRONTEND_BASE_URL` — Base URL for links in emails (default: `http://localhost:5173`)

If mail is not configured, verification/reset emails are logged only (no actual send).

# Roles and Admin — How role is managed

This document describes how **roles** work in AttireHub (including **admin**) and how to create or change an admin user.

---

## 1. Role model

### 1.1 Defined roles

Roles are defined in `com.attirehub.shared.enums.Role`:

| Role       | Purpose |
|-----------|---------|
| `CUSTOMER` | Default for registered users; can browse, cart, order, profile. |
| `ADMIN`    | Back-office: manage products, categories, orders, users, etc. (when you add admin APIs). |

Spring Security expects authorities with the `ROLE_` prefix. The app maps each enum to a single authority:

- `Role.CUSTOMER` → `ROLE_CUSTOMER`
- `Role.ADMIN` → `ROLE_ADMIN`

So when we say “user has role ADMIN”, we mean the user has authority `ROLE_ADMIN`.

---

## 2. Where role is stored and used

| Layer | How role is used |
|-------|-------------------|
| **Database** | `users.role` column (`VARCHAR(20)`), default `'CUSTOMER'`). Indexed for filtering. |
| **Entity** | `User.role` (`Role` enum), default `Role.CUSTOMER`. |
| **UserDetails** | `User.getAuthorities()` returns `[ROLE_<role.name()>]` (e.g. `ROLE_ADMIN`). |
| **JWT** | Access token includes a `role` claim set from that authority (e.g. `"ROLE_ADMIN"`). |
| **Security** | `SecurityConfig` and/or `@PreAuthorize("hasRole('ADMIN')")` restrict admin-only URLs and methods. |

Registration always creates users with `Role.CUSTOMER`. There is no public API to self-register as ADMIN; admin users are created outside the normal sign-up flow (see below).

---

## 3. How admin is enforced

### 3.1 URL-based rule (SecurityConfig)

All paths under **`/api/v1/admin/**`** require the user to have role **ADMIN**:

```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

- `hasRole("ADMIN")` checks for the authority **`ROLE_ADMIN`** (Spring adds the prefix when using `hasRole`).
- Requests to `/api/v1/admin/*` without a valid JWT → **401 Unauthorized**.
- Requests with a JWT for a **CUSTOMER** user → **403 Forbidden**.
- Only users with `role = ADMIN` in the DB (and thus `ROLE_ADMIN` in the JWT) can access admin endpoints.

### 3.2 Method-level (optional)

For finer control on specific endpoints (e.g. under a different path), use method security:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/v1/some-resource/restricted")
public ResponseEntity<?> adminOnly() { ... }
```

`@EnableMethodSecurity` is already enabled in `SecurityConfig`, so `@PreAuthorize` works as long as the request has passed the filter chain and the user is authenticated.

**Summary:** Put admin-only REST APIs under **`/api/v1/admin/**`** so they are protected by the existing SecurityConfig rule. Use `@PreAuthorize("hasRole('ADMIN')")` only when you need admin-only on other paths.

---

## 4. How to create or change an admin user

There is **no** “register as admin” or “change my role” API exposed to clients. Admin users are managed outside the normal app registration flow.

### 4.1 Option A: Direct database update

After at least one user exists (e.g. from normal registration):

```sql
-- Set a known user (by email) to ADMIN
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@yourdomain.com';
```

Then that user logs in via **POST /api/v1/auth/login** with their existing email/password. The returned JWT will contain `ROLE_ADMIN` and they can call **/api/v1/admin/** endpoints.

### 4.2 Option B: Seed data (Flyway/Liquibase)

You can add a Flyway migration (e.g. `V2__seed_admin_user.sql`) that inserts an initial admin user with a hashed password. For example:

```sql
-- Use a BCrypt hash for a known initial password (e.g. 'AdminChangeMe1!')
-- Generate hash with: new BCryptPasswordEncoder(12).encode("AdminChangeMe1!")
INSERT INTO users (email, password_hash, first_name, last_name, role, is_active, email_verified, created_at, updated_at, version)
VALUES (
  'admin@attirehub.local',
  '$2a$12$...',  -- replace with actual BCrypt hash
  'System',
  'Admin',
  'ADMIN',
  TRUE,
  FALSE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  0
)
ON DUPLICATE KEY UPDATE id = id;
```

Then change the password on first login (if you add a “change password” API) or keep it for local/dev only.

### 4.3 Option C: First-user bootstrap (code)

Alternatively, in a `@PostConstruct` or `ApplicationRunner` bean, check if any user has `ADMIN`; if not, create one (e.g. from a property like `app.bootstrap.admin-email`). This is useful for first deployment; same idea as above but in Java.

---

## 5. JWT and role

- On **login** or **register**, the backend loads the `User` (with `User.role`), builds `UserDetails` (authorities = `ROLE_<role>`), and generates an **access token**.
- The JWT access token includes a **`role`** claim set from that authority (e.g. `"ROLE_ADMIN"`).
- The frontend does **not** need to parse the JWT to enforce security (the backend does), but it can decode the token to show/hide admin UI or redirect to an admin area when the user has the admin role.

---

## 6. Summary

| Topic | Detail |
|-------|--------|
| **Roles** | `CUSTOMER` (default), `ADMIN`. Stored in `users.role` and exposed as `ROLE_CUSTOMER` / `ROLE_ADMIN`. |
| **Registration** | Always assigns `CUSTOMER`. No public API to become ADMIN. |
| **Admin access** | Any endpoint under **`/api/v1/admin/**`** requires **ADMIN** (enforced in `SecurityConfig`). |
| **Creating admin** | DB update, seed migration, or bootstrap in code — not via public registration. |
| **JWT** | Access token includes `role`; only users with `ROLE_ADMIN` can call admin APIs. |

Base URL when running locally: **`http://localhost:8081`**. Admin endpoints: **`http://localhost:8081/api/v1/admin/**`.

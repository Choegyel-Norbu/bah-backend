# Notifications API — In-app notifications

Base URL: **`/api/v1/notifications`**  
All endpoints require **authentication** (Bearer JWT in `Authorization` header).  
Responses use the standard **ApiResponse** wrapper with `success`, `message`, `data`, `timestamp`.

---

## Overview

- Notifications are created by the backend when events occur (e.g. **new order placed**).
- Users can **list** their notifications (paginated), **mark one as read**, or **mark all as read**.
- Use `referenceType` and `referenceId` to link the UI to the related resource (e.g. order detail page).

---

## 1. List my notifications

**Endpoint:** `GET /api/v1/notifications`  
**Auth:** Required (Bearer token)

### Query parameters

| Parameter | Type    | Default | Description                    |
|-----------|---------|---------|--------------------------------|
| `page`    | number  | `0`     | Page index (0-based)          |
| `size`    | number  | `20`    | Page size                     |
| `read`    | boolean | —       | Filter by read: `true` / `false` |
| `type`    | string  | —       | Filter by type: `NEW_ORDER`, `ORDER_STATUS_UPDATE`, `PROMO` |

### Example request

```
GET /api/v1/notifications?page=0&size=20&read=false
Authorization: Bearer <accessToken>
```

### Success response (200 OK)

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "type": "NEW_ORDER",
        "title": "Order placed",
        "message": "Your order ORD-20250220-0001 has been placed. Total: 99.00",
        "referenceType": "ORDER",
        "referenceId": "ORD-20250220-0001",
        "read": false,
        "createdAt": "2026-02-20T14:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-02-20T14:00:05"
}
```

### Notification object (in `content[]`)

| Field          | Type    | Description                                      |
|----------------|---------|--------------------------------------------------|
| `id`           | number  | Notification ID                                  |
| `type`         | string  | `NEW_ORDER` \| `ORDER_STATUS_UPDATE` \| `PROMO` |
| `title`        | string  | Short title (e.g. "Order placed")                |
| `message`      | string  | Body text                                        |
| `referenceType`| string  | Linked entity type (e.g. `ORDER`)                |
| `referenceId`  | string  | Linked ID (e.g. order number for orders)         |
| `read`         | boolean | Whether the user has marked it read              |
| `createdAt`    | string  | ISO 8601 datetime                                |

**Frontend usage:** For `referenceType === "ORDER"`, link to order detail using `referenceId` as the order number.

---

## 2. Mark one notification as read

**URL:** `PATCH /api/v1/notifications/{notificationId}/read`  
**Auth:** Required (Bearer token in `Authorization` header)

| Item | Value |
|------|--------|
| **URL** | `PATCH /api/v1/notifications/{notificationId}/read` (e.g. `PATCH /api/v1/notifications/1/read`) |
| **Payload** | None (no request body) |
| **Response body (200 OK)** | See below |

### Response body (200 OK)

```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "id": 1,
    "type": "NEW_ORDER",
    "title": "Order placed",
    "message": "Your order ORD-20250220-0001 has been placed. Total: 99.00",
    "referenceType": "ORDER",
    "referenceId": "ORD-20250220-0001",
    "read": true,
    "createdAt": "2026-02-20T14:00:00"
  },
  "timestamp": "2026-02-20T14:01:00"
}
```

**Errors:** `404` — Notification not found or not owned by the authenticated user.

---

## 3. Mark all as read

**Endpoint:** `PATCH /api/v1/notifications/read-all`  
**Auth:** Required (Bearer token)

### Example request

```
PATCH /api/v1/notifications/read-all
Authorization: Bearer <accessToken>
```

(No request body.)

### Success response (200 OK)

```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": null,
  "timestamp": "2026-02-20T14:02:00"
}
```

---

## 4. Admin: Mark any notification as read

Admins can mark **any** notification as read (e.g. when viewing the full list at `GET /api/v1/admin/notifications`). For marking **only their own** notifications, use the user endpoint (section 2) with the admin’s token.

| Item | Value |
|------|--------|
| **URL** | `PATCH /api/v1/admin/notifications/{notificationId}/read` (e.g. `PATCH /api/v1/admin/notifications/1/read`) |
| **Auth** | Admin only (Bearer token with `ROLE_ADMIN`) |
| **Payload** | None (no request body) |
| **Response body (200 OK)** | See below |

### Response body (200 OK)

```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "id": 1,
    "type": "NEW_ORDER",
    "title": "New order received",
    "message": "New order ORD-20260313-0001 from customer@example.com. Total: 99.00",
    "referenceType": "ORDER",
    "referenceId": "ORD-20260313-0001",
    "read": true,
    "createdAt": "2026-03-13T10:00:00",
    "userId": 5,
    "userEmail": "admin@attirehub.com"
  },
  "timestamp": "2026-03-13T10:05:00"
}
```

**Errors:** `404` — Notification not found.

---

## Order shipped and delivered

When an admin updates an order’s status to **SHIPPED** or **DELIVERED**, the backend creates one in-app notification for the **customer** who placed the order. The customer sees it in their notification list (`GET /api/v1/notifications`) and can open the order using `referenceId`.

| Status   | Title shown to customer | When it’s created                          |
|----------|--------------------------|--------------------------------------------|
| Shipped  | **Order shipped**       | Admin sets order status to `SHIPPED`       |
| Delivered| **Order delivered**      | Admin sets order status to `DELIVERED`     |

**Notification fields:** `type` = `ORDER_STATUS_UPDATE`, `referenceType` = `ORDER`, `referenceId` = order number (e.g. `ORD-20260313-0001`). If the admin adds notes (e.g. tracking number), they are appended to the message.

### Example (shipped)

Customer notification after admin marks order as shipped (optional notes: "Tracking: 1Z999AA10123456784"):

```json
{
  "id": 42,
  "type": "ORDER_STATUS_UPDATE",
  "title": "Order shipped",
  "message": "Your order ORD-20260313-0001 is now shipped. Tracking: 1Z999AA10123456784",
  "referenceType": "ORDER",
  "referenceId": "ORD-20260313-0001",
  "read": false,
  "createdAt": "2026-03-13T10:30:00"
}
```

### Example (delivered)

Customer notification after admin marks order as delivered:

```json
{
  "id": 43,
  "type": "ORDER_STATUS_UPDATE",
  "title": "Order delivered",
  "message": "Your order ORD-20260313-0001 is now delivered.",
  "referenceType": "ORDER",
  "referenceId": "ORD-20260313-0001",
  "read": false,
  "createdAt": "2026-03-14T09:00:00"
}
```

**Frontend:** For `referenceType === "ORDER"`, link to the order detail page using `referenceId` as the order number.

---

## When notifications are created (all events)

| Event | Recipient | `type` | `referenceType` | `referenceId` |
|-------|------------|--------|------------------|----------------|
| User places order | Admins | `NEW_ORDER` | `ORDER` | Order number |
| Order status updated (confirmed, shipped, delivered, cancelled, etc.) | Customer (order owner) | `ORDER_STATUS_UPDATE` | `ORDER` | Order number |

Use this to drive badges (e.g. unread count from `GET ...?read=false`) and to navigate to the order page when the user taps a notification.

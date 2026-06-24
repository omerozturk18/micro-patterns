# Physical CQRS Implementation - order-service

## Architecture Overview

This document describes the implementation of **Physical CQRS (Command Query Responsibility Segregation)** in the `order-service` microservice with complete separation of Write and Read databases.

### Key Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Requests                          │
│            (Swagger/Postman/Browser)                         │
└────────────────┬────────────────┬────────────────────────────┘
                 │                │
      ┌──────────▼──────────┐  ┌──▼──────────────────────┐
      │  COMMAND Path       │  │  QUERY Path             │
      │ (POST, PUT, DELETE) │  │ (GET requests)          │
      └──────────┬──────────┘  └──┬──────────────────────┘
                 │                │
      ┌──────────▼──────────┐  ┌──▼──────────────────────┐
      │ OrderCommandService │  │ OrderQueryService       │
      │ (WRITE MODEL)       │  │ (READ MODEL)            │
      └──────────┬──────────┘  └──┬──────────────────────┘
                 │                │
      ┌──────────▼──────────┐  ┌──▼──────────────────────┐
      │   Write DB          │  │   Read DB               │
      │ jdbc:h2:mem:        │  │ jdbc:h2:mem:            │
      │ order_write_db      │  │ order_read_db           │
      │                     │  │                          │
      │ - Order (normalized)│  │ - OrderReadModel        │
      │ - OutboxEvent       │  │   (denormalized)        │
      └──────────┬──────────┘  └──▲──────────────────────┘
                 │                │
                 │   ApplicationEvent  │
      ┌──────────▼────────────────────┘
      │  OrderProjector
      │  (Eventual Consistency)
      │  - Listens: OrderCreatedEvent, OrderUpdatedEvent
      │  - Syncs: Write DB → Read DB
      └──────────────────────
```

## Database Configuration

### Write Database (`order_write_db`)
- **URL:** `jdbc:h2:mem:order_write_db`
- **Purpose:** Stores the command model (source of truth for writes)
- **Tables:**
  - `orders` - Normalized order records
  - `outbox_events` - Event log for reliability

### Read Database (`order_read_db`)
- **URL:** `jdbc:h2:mem:order_read_db`
- **Purpose:** Optimized denormalized view for fast queries
- **Tables:**
  - `order_read_model` - Denormalized order view with all required data

## Physical Separation

### Write Side (Command Model)
```
entity/write/
├── Order.java           # Normalized command entity
└── OutboxEvent.java     # Event store for outbox pattern

repository/write/
├── OrderWriteRepository.java
└── OutboxEventWriteRepository.java

service/
├── OrderCommandService.java  # Business logic for commands
└── SagaOrchestrator.java     # Distributed transaction orchestration
```

### Read Side (Query Model)
```
entity/read/
└── OrderReadModel.java      # Denormalized query entity

repository/read/
└── OrderReadRepository.java  # Optimized queries

service/
└── OrderQueryService.java   # Query logic
```

### Synchronization
```
event/
├── OrderCreatedEvent.java
└── OrderUpdatedEvent.java

service/
└── OrderProjector.java      # Listens to events, syncs to read DB
```

## Data Flow

### 1. Command Flow (Write)
```
POST /api/orders/commands
    ↓
OrderCommandController
    ↓
OrderCommandService.createOrder()
    ↓
OrderWriteRepository.save() → Write DB
    ↓
OutboxEventWriteRepository.save() → Outbox table
    ↓
eventPublisher.publishEvent(OrderCreatedEvent)
    ↓
OrderProjector.handleOrderCreated() → Read DB
```

### 2. Query Flow (Read)
```
GET /api/orders/queries
    ↓
OrderQueryController
    ↓
OrderQueryService.getAllOrders()
    ↓
OrderReadRepository.findAll() → Read DB (fast, denormalized)
    ↓
Return OrderReadModel list
```

### 3. Saga Pattern Flow
```
After OrderCreatedEvent:
    ↓
SagaOrchestrator.processOrder()
    ↓
PaymentClient.processPayment()
    ↓
Update Order status in Write DB
    ↓
eventPublisher.publishEvent(OrderUpdatedEvent)
    ↓
OrderProjector.handleOrderUpdated() → Update Read DB
```

## REST API Endpoints

### Command Endpoints (Write)
```
POST   /api/orders/commands
       Create a new order
       Body: { "productId": 1, "amount": 99.99 }
       Returns: Order from Write DB

PUT    /api/orders/commands/{id}/status
       Update order status
       Params: status=COMPLETED
       Returns: Updated Order
```

### Query Endpoints (Read)
```
GET    /api/orders/queries
       Get all orders
       Returns: List<OrderReadModel>

GET    /api/orders/queries/{id}
       Get order by ID
       Returns: OrderReadModel

GET    /api/orders/queries/status/{status}
       Get orders by status
       Params: PENDING, COMPLETED, CANCELLED
       Returns: List<OrderReadModel>

GET    /api/orders/queries/product/{productId}
       Get orders by product
       Returns: List<OrderReadModel>
```

## Testing Instructions

### Prerequisites
- Application running on `http://localhost:8082`
- H2 Consoles available:
  - Write DB: http://localhost:8082/h2-console (JDBC: `jdbc:h2:mem:order_write_db`)
  - Read DB: http://localhost:8082/h2-console (JDBC: `jdbc:h2:mem:order_read_db`)

### Test 1: Create Order (Write)
```bash
curl -X POST http://localhost:8082/api/orders/commands \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "amount": 99.99}'
```

**Verification:**
1. Response shows Order with status "PENDING"
2. Check Write DB: `SELECT * FROM orders` shows the order
3. Wait 1-2 seconds (eventual consistency)
4. Check Read DB: `SELECT * FROM order_read_model` shows the order
5. Read DB shows denormalized data (productName, etc.)

### Test 2: Query Orders (Read)
```bash
# Get all orders (from Read DB)
curl http://localhost:8082/api/orders/queries

# Get specific order
curl http://localhost:8082/api/orders/queries/{order-id}

# Get orders by status
curl http://localhost:8082/api/orders/queries/status/PENDING

# Get orders by product
curl http://localhost:8082/api/orders/queries/product/1
```

### Test 3: Update Order Status (Write)
```bash
curl -X PUT http://localhost:8082/api/orders/commands/{order-id}/status \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "status=COMPLETED"
```

**Verification:**
1. Write DB: Order status updated
2. Outbox table: New event created
3. Wait 1-2 seconds (eventual consistency)
4. Read DB: OrderReadModel status updated with new projectedAt timestamp

### Test 4: Verify Eventual Consistency
```bash
# 1. Create order
curl -X POST http://localhost:8082/api/orders/commands \
  -H "Content-Type: application/json" \
  -d '{"productId": 2, "amount": 149.99}'

# 2. Immediately query (may not be in Read DB yet)
curl http://localhost:8082/api/orders/queries

# 3. Wait 2 seconds, query again (now should be in Read DB)
curl http://localhost:8082/api/orders/queries
```

### Test 5: H2 Console Inspection

**Write DB:**
```
JDBC URL: jdbc:h2:mem:order_write_db
User: sa
Password: (leave blank)

Queries:
- SELECT * FROM orders;
- SELECT * FROM outbox_events;
```

**Read DB:**
```
JDBC URL: jdbc:h2:mem:order_read_db
User: sa
Password: (leave blank)

Queries:
- SELECT * FROM order_read_model;
```

## Eventual Consistency Behavior

The system implements **eventual consistency**:

- **Write:** Immediate (synchronous)
- **Read:** Eventually consistent (asynchronous projection)
- **Delay:** Typically < 100ms, maximum bounded by event polling

### Components Ensuring Reliability:
1. **Transactional Outbox Pattern:** Events saved in same transaction as write
2. **OrderProjector:** Listens to events and syncs asynchronously
3. **OutboxPoller:** Polls outbox table every 5 seconds for external event publishing

## Benefits of Physical CQRS

1. **Independent Scaling:** Scale read and write paths separately
2. **Optimized Storage:** Read DB can be denormalized for performance
3. **Separation of Concerns:** Clear distinction between commands and queries
4. **Eventual Consistency:** Reliable event-driven synchronization
5. **Audit Trail:** Outbox pattern provides event log
6. **Resilience:** System continues operating even if projection lags

## Troubleshooting

### Orders appear in Write DB but not Read DB
- Check if OrderProjector events are being fired
- Check application logs for event handling
- Wait a moment for eventual consistency
- Check Read DB has table created: `SELECT * FROM order_read_model;`

### H2 Consoles not loading
- Ensure application is running on port 8082
- Check application.yml: `h2.console.enabled: true`
- Navigate to http://localhost:8082/h2-console

### Saga/Payment Processing
- Check PaymentClient connectivity
- Orders should update to COMPLETED or CANCELLED based on payment response
- Update event should trigger another projection

## Future Enhancements

1. **Async Processing:** Configure @Async for OrderProjector
2. **Kafka Integration:** Replace ApplicationEvents with Kafka topics
3. **Multiple Read Models:** Project to different formats (REST, GraphQL, etc.)
4. **Event Sourcing:** Replace current write model with event-sourced aggregates
5. **CQRS Query Optimization:** Add specific indexes for common queries
6. **Resilience:** Implement circuit breakers for projections

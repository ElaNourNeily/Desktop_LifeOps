/**
 * HOW TO USE THE APIs - STEP BY STEP GUIDE
 */

# 🚀 COMPLETE API GUIDE - HOW TO USE

## Quick Overview

You have **8 domain-specific business logic engines** that power **4 different API suites**:

```
MÉTIER (Business Logic) ──→ APIS (Endpoints) ──→ Your Application
────────────────────────────────────────────────────────────────

Pricing Engine ─────────→ Pricing API
Routing Engine ─────────→ Logistics API
Inventory Engine ───────→ Inventory API
HR Engine ──────────────→ HR API
Patient Risk Engine ────→ Healthcare API
Fraud Detection Engine ──→ Fraud API
Recommendation Engine ──→ Recommendation API
Property Valuation ─────→ Real Estate API
```

---

## 🎯 STEP-BY-STEP SETUP

### Step 1: Clone/Get Files
```bash
# You have:
- 06-metier-examples-advanced.ts      (Pricing, Routing, Inventory, HR, Real Estate)
- 07-metier-examples-advanced-2.ts    (Healthcare, Fraud, Recommendations)
- 08-api-with-metier.ts               (API Endpoints)
- 09-api-examples-complete.ts         (Request/Response Examples)
```

### Step 2: Install Dependencies
```bash
npm install express typescript ts-node
# or
yarn add express typescript ts-node
```

### Step 3: Create Main Server File
```typescript
// server.ts
import express from 'express';
import { setupMetierAPIs } from './08-api-with-metier';

const app = express();
app.use(express.json());

setupMetierAPIs(app);

app.listen(3000, () => {
  console.log('🚀 Server running on http://localhost:3000');
});
```

### Step 4: Run Server
```bash
npx ts-node server.ts
```

---

## 📊 API 1: DYNAMIC PRICING API

### Purpose
Calculate optimal product prices based on demand, inventory, competitor prices, and seasonality.

### Endpoints

#### Register Product
```bash
POST /api/v1/pricing/products
Content-Type: application/json

{
  "name": "Wireless Headphones",
  "basePrice": 100,
  "cost": 35,
  "inventory": 250,
  "demandScore": 80,           # 0-100: HIGH demand
  "competitorPrice": 95,
  "seasonalMultiplier": 1.2    # 20% higher for holiday
}

Response:
{
  "product": {...},
  "recommendedPrice": 137.28,
  "margin": 102.28
}
```

#### Calculate Optimal Price
```bash
POST /api/v1/pricing/calculate
Content-Type: application/json

{ "productId": "prod_1234567890" }

Response:
{
  "recommendedPrice": 137.28,
  "margin": 102.28,
  "marginPercentage": 72.65,
  "factors": {
    "demand": "+16%",       # HIGH demand = higher price
    "inventory": "+10%",    # LOW stock = higher price
    "competitor": "-2%",    # 5% undercut
    "seasonal": "+0%"       # Not holiday
  }
}
```

#### Analyze Price Elasticity
```bash
GET /api/v1/pricing/products/prod_1234567890/elasticity

Response:
{
  "elasticity": "-0.42",
  "type": "Inelastic",
  "interpretation": "Customers not price sensitive. Increase price for margin.",
  "recommendation": {
    "action": "Increase price by 10%",
    "expectedImpact": "Quantity down 5%, Revenue up 5%"
  }
}
```

### Use Cases
- 🛒 E-commerce: Auto-adjust prices in real-time
- 💰 Maximize profit margins
- 📊 Competitive pricing intelligence

---

## 🚚 API 2: LOGISTICS ROUTING API

### Purpose
Generate optimized delivery routes to minimize cost and time.

### Endpoints

#### Add Delivery
```bash
POST /api/v1/routing/deliveries
Content-Type: application/json

{
  "toName": "Manhattan Office",
  "toLat": 40.758,
  "toLng": -73.9855,
  "weight": 8,
  "priority": "fast"          # fast|standard|overnight
}

Response:
{
  "delivery": {...},
  "queueSize": 12
}
```

#### Get Pending Deliveries
```bash
GET /api/v1/routing/deliveries/pending

Response:
{
  "totalPending": 12,
  "byPriority": {
    "overnight": 2,
    "fast": 5,
    "standard": 5
  },
  "deliveries": [...]
}
```

#### Optimize Routes
```bash
POST /api/v1/routing/routes/optimize
Content-Type: application/json

{ "deliveryIds": ["del_001", "del_002", "del_003", "del_004", "del_005"] }

Response:
{
  "routeCount": 2,
  "routes": [
    {
      "routeId": "route_1",
      "stops": 4,
      "distance": 23.5,
      "estimatedTime": 45,
      "cost": 97.0,
      "efficiency": 87.3    # Higher = better optimization
    }
  ],
  "summary": {
    "totalDistance": 41.7,
    "totalCost": 183.4,
    "averageEfficiency": 89.4
  }
}
```

### Use Cases
- 📦 Delivery optimization (10-20% cost reduction)
- ⏱️ Real-time route planning
- 🚗 Fleet management
- 🌍 Multi-stop logistics

---

## 📦 API 3: INVENTORY MANAGEMENT API

### Purpose
Optimize inventory levels and determine when to reorder using EOQ formula.

### Endpoints

#### Register SKU
```bash
POST /api/v1/inventory/sku
Content-Type: application/json

{
  "name": "Phone Case",
  "currentStock": 250,
  "reorderPoint": 100,
  "leadTimeDays": 5,
  "holdingCost": 2,          # $ per unit per year
  "orderingCost": 100        # $ per order
}

Response:
{ "sku": {...} }
```

#### Record Demand
```bash
POST /api/v1/inventory/sku/sku_123/demand
Content-Type: application/json

{ "quantity": 25, "date": "2026-04-25" }

Response:
{ "message": "Demand recorded" }
```

#### Check Reorder Analysis
```bash
GET /api/v1/inventory/sku/sku_123/reorder-analysis

Response:
{
  "demandAnalysis": {
    "avgDailyDemand": "27.35",
    "forecastedAnnual": "9983"
  },
  "economicOrderQuantity": {
    "recommendedQty": 1000,
    "explanation": "Balances ordering cost with holding cost"
  },
  "decision": {
    "shouldOrder": true,
    "recommendedQty": 1000,
    "daysUntilStockout": 5,
    "reason": "Stock will run out in 5 days, lead time is 5 days"
  }
}
```

#### ABC Analysis
```bash
GET /api/v1/inventory/abc-analysis

Response:
{
  "categories": {
    "A": {
      "count": 12,
      "priority": "HIGH - Tight control, frequent reviews",
      "items": [...]
    },
    "B": {
      "count": 45,
      "priority": "MEDIUM - Normal controls",
      "items": [...]
    },
    "C": {
      "count": 243,
      "priority": "LOW - Minimal control",
      "items": [...]
    }
  }
}
```

### Use Cases
- 📊 Optimal inventory levels (20% cost reduction)
- 🔄 Automatic reorder recommendations
- 📈 ABC inventory classification
- 💾 Stock prediction

---

## 👥 API 4: HR COMPENSATION API

### Purpose
Calculate fair compensation and identify top performers using 9-box matrix.

### Endpoints

#### Calculate Total Compensation
```bash
POST /api/v1/hr/compensation/calculate
Content-Type: application/json

{ "employeeId": "emp_123" }

Response:
{
  "employee": {
    "name": "Alice Johnson",
    "department": "Engineering",
    "yearsExperience": 6
  },
  "compensation": {
    "baseSalary": 120000,
    "performanceBonus": 24000,  # 20% performance
    "skillBonus": 18000,        # Skills + certifications
    "totalCompensation": 162000,
    "percentageIncrease": 35.0  # Total increase from base
  },
  "recommendation": {
    "message": "Employee should earn $162,000/year (35% above base)"
  }
}
```

#### 9-Box Matrix Analysis
```bash
GET /api/v1/hr/nine-box-analysis

Response:
{
  "nineBox": {
    "stars": {
      "count": 5,
      "description": "High Performance, High Potential",
      "action": "Fast-track for leadership roles",
      "employees": ["Alice Johnson", "Bob Smith", ...]
    },
    "corePlayers": {
      "count": 18,
      "description": "High Performance, Medium Potential",
      "action": "Reward and retain",
      "employees": [...]
    },
    "highPotential": {
      "count": 8,
      "description": "Medium Performance, High Potential",
      "action": "Mentor and develop",
      "employees": [...]
    },
    "developing": {
      "count": 4,
      "description": "Low Performance, High Potential",
      "action": "Performance improvement plan",
      "employees": [...]
    },
    "transition": {
      "count": 2,
      "description": "Low Performance, Low Potential",
      "action": "Consider exit",
      "employees": [...]
    }
  }
}
```

### Use Cases
- 💰 Fair, transparent compensation
- 🎯 Identify flight risks (underpaid stars)
- 📊 Succession planning
- 🎓 Development recommendations

---

## 🧪 TESTING THE APIS

### Option 1: Using cURL
```bash
# Test pricing
curl -X POST http://localhost:3000/api/v1/pricing/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Widget","basePrice":50,"cost":20,"inventory":100,"demandScore":75}'

# Test routing
curl -X POST http://localhost:3000/api/v1/routing/deliveries \
  -H "Content-Type: application/json" \
  -d '{"toName":"Store 1","toLat":40.7128,"toLng":-74.006,"weight":5}'

# Test inventory
curl -X POST http://localhost:3000/api/v1/inventory/sku \
  -H "Content-Type: application/json" \
  -d '{"name":"Widget","currentStock":100,"reorderPoint":20,"leadTimeDays":3,"holdingCost":1,"orderingCost":50}'

# Test HR
curl -X POST http://localhost:3000/api/v1/hr/compensation/calculate \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"emp_001"}'
```

### Option 2: Using Postman
1. Import collection from `09-api-examples-complete.ts`
2. Set base URL: `http://localhost:3000`
3. Run requests in sequence

### Option 3: Using JavaScript Fetch
```javascript
// Pricing
const pricingResponse = await fetch('http://localhost:3000/api/v1/pricing/calculate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ productId: 'prod_123' })
});
const pricing = await pricingResponse.json();
console.log(pricing);

// Routing
const routingResponse = await fetch('http://localhost:3000/api/v1/routing/routes/optimize', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ deliveryIds: ['del_001', 'del_002'] })
});
const routes = await routingResponse.json();
console.log(routes);
```

---

## 📈 REAL-WORLD WORKFLOW

### Scenario: E-Commerce Site

**Day 1: Setup**
```bash
# Register all products
POST /api/v1/pricing/products (10 products)
```

**Daily: Pricing Optimization**
```bash
# Morning: Calculate optimal prices
POST /api/v1/pricing/calculate (for top 100 products)

# Update website with new prices
# Record sales (demand)
```

**Weekly: Inventory Check**
```bash
# Check all SKUs
GET /api/v1/inventory/abc-analysis

# For each A item, check reorder
GET /api/v1/inventory/sku/:skuId/reorder-analysis

# Place orders if needed
```

**Monthly: Delivery Optimization**
```bash
# Optimize all pending deliveries
POST /api/v1/routing/routes/optimize

# Generate shipping labels
# Notify customers of delivery times
```

**Quarterly: HR Reviews**
```bash
# Analyze employee compensation
GET /api/v1/hr/nine-box-analysis

# Plan promotions, raises, development
```

---

## 🔗 FILE DEPENDENCIES

```
Your App Code
    ↓
09-api-examples-complete.ts (Request/Response examples)
    ↓
08-api-with-metier.ts (API Controllers)
    ↓
06-metier-examples-advanced.ts (Business Logic)
07-metier-examples-advanced-2.ts (More Business Logic)
```

---

## 🎓 LEARNING PATH

1. **Start Simple**: Try Pricing API first (easiest to understand)
2. **Add Complexity**: Then Inventory (introduces forecasting)
3. **Add Logistics**: Then Routing (introduces graph algorithms)
4. **Scale Up**: Then HR (people-based decisions)
5. **Advanced**: Add Healthcare, Fraud, Recommendations

---

## 💡 TIPS

- ✅ APIs are **stateless** - use a database to persist data
- ✅ Add **authentication** before production
- ✅ Add **rate limiting** to prevent abuse
- ✅ **Monitor** API response times
- ✅ **Version** your APIs (`/api/v1/`, `/api/v2/`)
- ✅ **Document** with OpenAPI/Swagger

---

## 🚀 NEXT STEPS

1. Run the server: `npx ts-node server.ts`
2. Test endpoints with curl or Postman
3. Connect to a real database (PostgreSQL, MongoDB)
4. Add authentication (JWT, OAuth)
5. Deploy to production (Heroku, AWS, GCP)


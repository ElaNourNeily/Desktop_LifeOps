/**
 * COMPREHENSIVE GUIDE: Advanced Business Logic Architecture
 * 
 * This guide explains the advanced métier patterns implemented in this system
 */

# 📚 ADVANCED BUSINESS LOGIC (MÉTIER AVANCÉE) GUIDE

## Overview
This project demonstrates a complete budget and expense management system with **2 advanced business logic layers** and **2 comprehensive APIs**.

---

## 🏗️ ARCHITECTURE

### LAYER 1: Domain Model (`01-budget-domain-model.ts`)
- **Entity definitions**: Budget, Expense, AlertRule, RecurringConfig
- **Type safety** with TypeScript interfaces
- **Separation of concerns**: Domain logic isolated from API handlers

### LAYER 2: Métier 1 - Budget Forecasting Engine
**Purpose**: Predictive analytics and budget optimization

#### Key Algorithms:
1. **Exponential Smoothing Forecasting**
   - Predicts future spending based on historical data
   - Uses smoothing factor (alpha) to balance recent vs historical trends
   - Returns confidence score (0-1) for prediction reliability

2. **Statistical Anomaly Detection**
   - Uses Z-score analysis to identify unusual transactions
   - Flags expenses deviating >2σ from mean
   - Configurable sensitivity factor

3. **AI-Driven Budget Optimization**
   - Analyzes actual spending patterns per category
   - Recommends budget reallocation based on:
     - Historical usage
     - Category priority (critical, high, medium, low)
     - Predicted future spending
   - Returns percentage-based recommendations

#### Example:
```typescript
// Predict January spending for Travel category
const forecast = forecastEngine.predictMonthlySpending(
  'cat_travel',
  historicalExpenses,
  alpha = 0.3
);
// Returns: { predictedAmount: 1875, confidence: 0.78, trend: 'increasing' }
```

### LAYER 3: Métier 2 - Smart Approval & Alert Engine
**Purpose**: Intelligent expense validation and workflow routing

#### Key Algorithms:
1. **Multi-Factor Risk Scoring**
   - Combines 5+ risk dimensions:
     - Category limit utilization (0-35 points)
     - Amount anomaly vs category average (0-25 points)
     - Missing documentation (0-20 points)
     - Timing anomalies (0-10 points)
     - Frequency fraud detection (0-20 points)
   - Score range: 0-100 (high = risky)

2. **Dynamic Approval Workflow Routing**
   - Routes based on risk score AND amount:
     - Risk > 60 → Board approval (3 required)
     - Risk > 40 → Director approval (2 required)
     - Amount > 25% budget → Manager approval (1 required)
     - Amount > 50% budget → Board approval (3 required)
     - Otherwise → Auto-approval (0 required)

3. **Intelligent Alert Generation**
   - Generates context-aware alerts:
     - Critical: Budget overrun >150%
     - Warning: Budget overrun >100%
     - Info: Standard logging
   - Includes actionable suggestions

#### Example:
```typescript
// Evaluate expense for approval
const riskScore = approvalEngine.calculateRiskScore(expense, budget);
// Returns: 62 (high risk)

const route = approvalEngine.routeForApproval(expense, budget, riskScore);
// Returns: { 
//   approvalPath: 'director', 
//   requiredApprovals: 2,
//   reasoning: ['High risk score: 62', 'Missing receipt']
// }
```

---

## 🔌 API 1: Budget Management (`02-api-budget.ts`)

### Endpoints:

#### GET /api/v1/budgets/:budgetId
**Purpose**: Retrieve budget with real-time analytics
```json
Response includes:
- Budget metadata (name, period, alerts)
- Category breakdown with spending status
- Total/remaining amounts
- Utilization percentages
- Health status (healthy/warning/overspent)
```

#### POST /api/v1/budgets
**Purpose**: Create new budget
```json
Request:
{
  "name": "Q1 2026 Budget",
  "totalLimit": 50000,
  "categories": [{
    "categoryId": "cat_travel",
    "name": "Travel",
    "limit": 10000,
    "priority": "high"
  }],
  "period": { "startDate": "2026-01-01", "endDate": "2026-03-31" }
}
```

#### GET /api/v1/budgets/:budgetId/forecast
**Purpose**: ML-based spending predictions
```json
Response includes:
- Predicted monthly spending per category
- Confidence levels (78%, 92%, etc.)
- Spending trends (increasing/decreasing/stable)
- Projected utilization percentages
- Overrun warnings & recommendations
```

**Use Case**: 
- Finance teams predict Q2 budget needs
- Early warning if category trending toward overrun
- Data-driven budget adjustments

#### GET /api/v1/budgets/:budgetId/anomalies
**Purpose**: Detect unusual spending patterns
```json
Response includes:
- Anomaly score (Z-score: how many σ from mean)
- Reason (e.g., "3.2σ above mean")
- Flagged expense IDs
- Categorized by category
```

**Use Case**:
- Fraud detection (e.g., $5000 office supply purchase vs $200 average)
- Identify one-off vs recurring expenses
- Compliance audits

#### POST /api/v1/budgets/:budgetId/optimize
**Purpose**: AI-driven budget reallocation
```json
Response includes:
- Category-by-category recommendations
- Dollar and percentage changes
- New total budget projection
```

**Use Case**:
- Mid-year budget adjustments
- Resource optimization across departments
- Prevent waste in underutilized categories

---

## 🔌 API 2: Expense Management & Approval (`03-api-expense-approval.ts`)

### Endpoints:

#### POST /api/v1/expenses
**Purpose**: Submit expense with intelligent risk assessment
```json
Request:
{
  "budgetId": "budget_123",
  "description": "Client meeting flights",
  "amount": 450,
  "categoryId": "cat_travel",
  "date": "2026-02-15",
  "tags": ["client-meeting", "billable"],
  "receipt": "https://receipts.example.com/receipt.pdf"
}

Response includes:
- Expense ID & status
- Risk score (0-100)
- Approval path (auto/manager/director/board)
- Alert (critical/warning/info)
- Auto-approval decision
```

**Métier Logic**:
- Calculates risk score
- Routes to appropriate approver
- Generates alert if needed
- Auto-approves if low-risk

#### GET /api/v1/expenses/:expenseId/approval-status
**Purpose**: Track approval history
```json
Response includes:
- Current status (pending/approved/rejected)
- Risk level (critical/medium/low)
- Approval chain with timestamps
- Associated alerts
```

#### POST /api/v1/expenses/:expenseId/approve
**Purpose**: Manager/Director approves expense
```json
Request: {
  "approver": "manager_john@company.com",
  "decision": "approved"
}
```

#### POST /api/v1/expenses/:expenseId/reject
**Purpose**: Reject with reason
```json
Request: {
  "approver": "manager_john@company.com",
  "reason": "Duplicate submission detected"
}
```

#### GET /api/v1/expenses/pending-approvals
**Purpose**: Manager dashboard - prioritized pending approvals
```json
Query params:
- priority: "risk" | "amount" | "date"

Response includes:
- Total pending count
- Critical/medium/low breakdowns
- Sorted by selected priority
- Full alert details for each
```

**Smart Sorting**:
- priority=risk: Critical items first (fraud/policy concerns)
- priority=amount: Largest spends first
- priority=date: Newest submissions first (default)

#### POST /api/v1/expenses/bulk-review
**Purpose**: Batch approve/reject multiple expenses
```json
Request: {
  "expenseIds": ["exp_1", "exp_2", "exp_3"],
  "action": "approve",
  "approver": "admin@company.com"
}

Response includes:
- Total processed count
- Approved/rejected breakdown
- Total amount processed
```

**Use Case**:
- End-of-month batch approval
- Compliance batch rejection
- Policy-based bulk actions

#### GET /api/v1/expenses/statistics
**Purpose**: Spending analytics & reporting
```json
Query params:
- budgetId: Filter by budget
- timeframe: "week" | "month" | all

Response includes:
- Total expenses & amount
- Average expense size
- By status breakdown (approved/pending/rejected)
- By category breakdown (count & amount)
```

---

## 🎯 Advanced Patterns Explained

### Pattern 1: Risk Scoring
Combines multiple signals into single score:
```
Risk = 
  (category_overage * 35) +
  (amount_anomaly * 25) +
  (missing_docs * 20) +
  (timing_anomaly * 10) +
  (frequency_fraud * 20)
```

**Advantage**: Flexible, explainable, extensible

### Pattern 2: Approval Workflow Routing
Routes based on business rules:
```
if (riskScore > 60) → Board approval (3 people)
else if (riskScore > 40) → Director (2 people)
else if (amount > 25% budget) → Manager (1 person)
else → Auto-approved (0 people)
```

**Advantage**: Scales to organization size, prevents fraud

### Pattern 3: Exponential Smoothing Forecasting
```
F(t+1) = α × actual(t) + (1-α) × forecast(t)
```
- α=0.3: Gives 30% weight to recent, 70% to history
- α=0.7: More reactive to recent changes
- Returns confidence based on variance

### Pattern 4: Z-Score Anomaly Detection
```
Z-score = (value - mean) / std_dev

Alert if |Z| > 2 (outlier with 95% confidence)
```

---

## 📊 Real-World Scenarios

### Scenario 1: Normal Expense ($450 flight)
```
1. Submitted with receipt
2. Amount: $450 vs Travel avg: $850 ✓
3. Risk score: 15 (low)
4. Auto-approved immediately
5. Alert: "info" - no action needed
```

### Scenario 2: High-Risk Expense ($8500 software)
```
1. Submitted WITHOUT receipt
2. Amount: $8500 vs budget: $50,000 (17%)
3. Risk score: 62 (critical):
   - Missing receipt: +20
   - Large amount: +25
   - Category already at 42%: +15
   - Timing (Friday): +2
4. Requires director approval (2 approvers)
5. Alert: "critical" - request documentation
6. Status: pending until approved
```

### Scenario 3: Monthly Anomaly Detection
```
Travel category typical: $400-$1500/trip
Detected anomaly: $3,500 (international)
Z-score: 3.2 (highly unusual)
Response: Flag for review, notify manager
```

### Scenario 4: Budget Optimization (End of Q1)
```
Original allocation:
- Travel: $10,000
- Office: $5,000
- Software: $20,000
- Events: $15,000
Total: $50,000

Actual usage:
- Travel trending: 90% utilized
- Office trending: 40% utilized
- Software trending: 120% (will overrun!)
- Events trending: 55% utilized

Recommendation:
- Travel: +$1,250 (increase 12.5%)
- Office: -$800 (reduce 16%)
- Software: +$4,500 (increase 22.5%)
- Events: -$1,950 (reduce 13%)
New total: $53,000
```

---

## 🚀 Deployment

### Dependencies:
```json
{
  "express": "^4.18.0",
  "typescript": "^5.0.0"
}
```

### Quick Start:
```bash
npm install
npx tsc
node dist/04-server-setup.js
```

### Available Ports:
- Default: 3000
- All endpoints available at: http://localhost:3000/api/v1/*

---

## 🔐 Security Considerations

1. **Authentication**: Mock in examples, use JWT in production
2. **Authorization**: Role-based (admin/manager/user) not shown
3. **Input Validation**: Should validate all request bodies
4. **Audit Logging**: Should log all approvals/rejections
5. **Data Encryption**: Use HTTPS, encrypt sensitive data

---

## 📈 Scalability Improvements

1. **Database**: Replace in-memory Maps with PostgreSQL/MongoDB
2. **Caching**: Use Redis for forecast cache
3. **Background Jobs**: Use Bull/RabbitMQ for bulk processing
4. **Monitoring**: Integrate Prometheus/DataDog
5. **Rate Limiting**: Add express-rate-limit middleware

---

## 📚 Further Reading

- Exponential Smoothing: https://en.wikipedia.org/wiki/Exponential_smoothing
- Z-Score Anomaly Detection: https://en.wikipedia.org/wiki/Standard_score
- Risk Scoring: https://en.wikipedia.org/wiki/Credit_scoring
- Workflow Routing: https://en.wikipedia.org/wiki/Workflow_management_system


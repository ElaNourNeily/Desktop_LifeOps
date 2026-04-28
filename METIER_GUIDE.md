/**
 * ADVANCED MÉTIER SUMMARY GUIDE
 * Quick reference for all business logic patterns
 */

# 🎯 ADVANCED MÉTIER (BUSINESS LOGIC) - COMPREHENSIVE GUIDE

## What is Métier?
**Métier** = The core business logic & algorithms that make your application intelligent and valuable. NOT external API integrations, but internal domain expertise.

---

## 📊 MÉTIER EXAMPLES OVERVIEW

### MÉTIER 1: E-COMMERCE - DYNAMIC PRICING ENGINE
**File**: `06-metier-examples-advanced.ts`

**Problem**: How to price products to maximize profit while staying competitive?

**Algorithms**:
1. **Multi-factor pricing formula**
   - Demand elasticity (high demand = higher price)
   - Inventory pressure (low stock = higher price)
   - Competitor undercutting (stay 5% cheaper)
   - Seasonal multipliers (holiday season boost)

2. **Price elasticity calculation**
   - Determines if customers are price-sensitive
   - Elastic (sensitive): Lower prices increase volume
   - Inelastic (insensitive): Raise prices for margin

3. **Dynamic pricing recommendations**
   - Analyzes historical sales vs price
   - Recommends price adjustments
   - Predicts impact on revenue

**Example**:
```typescript
Product: Widget
Base Price: $100
Demand: HIGH (80/100)
Stock: LOW (12 units)
Competitor: $95

Final Price: $128
- Demand factor: 1.16x (+16%)
- Inventory factor: 1.1x (+10%)
- Competitor factor: 0.98x (-2%)
- Margin: 28%

Recommendation: "Price increase won't hurt sales - inelastic demand"
```

**Business Impact**: 
- 15-30% margin improvement
- Fewer stockouts
- Better competitive positioning

---

### MÉTIER 2: LOGISTICS - ROUTE OPTIMIZATION
**File**: `06-metier-examples-advanced.ts`

**Problem**: How to deliver packages with minimal cost & time?

**Algorithms**:
1. **Traveling Salesman Problem (TSP) - Nearest Neighbor Heuristic**
   - Starts at warehouse
   - Greedily picks nearest unvisited stop
   - Returns to warehouse when capacity reached
   - Uses Haversine formula for real-world distances

2. **Priority-based routing**
   - Overnight deliveries routed first
   - Fast deliveries second
   - Standard deliveries last
   - Respects time windows

3. **Route efficiency score**
   - Compares actual vs theoretical minimum distance
   - Lower = better optimization

**Example**:
```
Warehouse: NYC (0, 0)

Deliveries:
- Stop 1: Brooklyn (5 km away)
- Stop 2: Queens (8 km away)
- Stop 3: Bronx (12 km away)

Optimized Route:
NYC → Brooklyn (5 km) → Queens (8 km) → Bronx (12 km) → NYC (12 km)
Total: 37 km, Cost: $124, Time: 55 mins
Efficiency: 87%
```

**Business Impact**:
- 10-20% fuel cost reduction
- 15% faster deliveries
- More deliveries per route

---

### MÉTIER 3: INVENTORY - DEMAND FORECASTING & EOQ
**File**: `06-metier-examples-advanced.ts`

**Problem**: When to reorder stock? How much to order?

**Algorithms**:
1. **Moving Average Forecasting**
   - Predicts average demand over next period
   - Simple, robust, no seasonal adjustments

2. **Economic Order Quantity (EOQ)**
   - Formula: EOQ = √(2 × D × S / H)
   - D = annual demand
   - S = ordering cost per order
   - H = holding cost per unit per year
   - Minimizes total inventory cost

3. **Reorder Point Calculation**
   - ROP = (avg daily demand × lead time) + safety stock
   - Prevents stockouts during lead time

4. **ABC Analysis**
   - A items: 80% of value (high priority)
   - B items: 15% of value (medium priority)
   - C items: 5% of value (low priority)
   - Allocate inventory budget accordingly

**Example**:
```
Product: Phone Case
Annual demand: 10,000 units
Ordering cost: $100 per order
Holding cost: $2 per unit/year

EOQ = √(2 × 10,000 × 100 / 2) = 1,000 units
Orders per year: 10
Reorder point: (27 units/day × 5 day lead time) + 50 safety = 185 units

ABC Category: A (high value)
Recommendation: "Order 1,000 units when stock hits 185"
```

**Business Impact**:
- Optimal order quantities (not too much, not too little)
- 20% reduction in inventory carrying costs
- Fewer stockouts

---

### MÉTIER 4: HR - COMPENSATION & PERFORMANCE SCORING
**File**: `06-metier-examples-advanced.ts`

**Problem**: How to fairly compensate employees & identify top performers?

**Algorithms**:
1. **Total Compensation Score**
   - Base salary + performance bonus + skills bonus + experience bonus
   - Performance bonus: weighted on rating + metrics
   - Skills bonus: 2% per skill, 3% per certification
   - Experience bonus: up to 15%

2. **9-Box Matrix Analysis**
   ```
   Performance vs Potential:
   High Potential + High Performance = "Star" (Promote)
   High Performance + Medium Potential = "Core Player" (Retain)
   Medium Performance + High Potential = "Developing" (Coach)
   Low Performance + Low Potential = "Transition" (Consider exit)
   ```

3. **Market salary adjustments**
   - Compare to market median
   - Adjust for experience
   - Identify underpaid/overpaid

**Example**:
```
Employee: Alice
Base salary: $100,000
Performance rating: 5/5
Skills: 8
Certifications: 3
Years experience: 5

Performance bonus: 20% = $20,000
Skills bonus: (8×2% + 3×3%) = 25% = $25,000
Experience bonus: 15% = $15,000
Total: $160,000

Market median: $105,000 (50th percentile)
Recommended: $120,000 (experience adjusted)
Current: $100,000
Gap: $20,000 UNDERPAID → Recommend raise
```

**Business Impact**:
- Fair, transparent compensation
- Identify flight risks (underpaid top performers)
- Reduce turnover

---

### MÉTIER 5: REAL ESTATE - PROPERTY VALUATION
**File**: `06-metier-examples-advanced.ts`

**Problem**: What's a property actually worth?

**Algorithms**:
1. **Comparable Sales Method (CMP)**
   - Find similar sold properties
   - Weight by similarity:
     - Age difference (-0.5% per year)
     - Size difference (-% per percentage)
     - Beds/baths difference (-10% per bedroom)
   - Calculate weighted average

2. **Investment scoring**
   - Cap rate: Annual rent / Purchase price
   - Cash-on-cash return: Annual cash flow / Down payment
   - 30-year mortgage calculations
   - Maintenance/tax/insurance estimates

**Example**:
```
Property: 3-bed house in Austin
Listing price: $500,000
Expected rent: $2,500/month

Comps:
- Similar 3-bed sold for $485,000
- Similar 3-bed sold for $520,000
- Similar 2-bed sold for $450,000

Estimated value: $505,000
Listing vs estimate: -1% (Fair price)

Cap rate: 6%
Cash-on-cash: 8.2%
Recommendation: "Good investment"
```

**Business Impact**:
- Accurate pricing (not over/undervalued)
- Investment profitability analysis
- Data-driven negotiations

---

### MÉTIER 6: HEALTHCARE - PATIENT RISK SCORING
**File**: `07-metier-examples-advanced-2.ts`

**Problem**: Which patients need urgent care?

**Algorithms**:
1. **Multi-factor risk scoring (0-100)**
   - Age: +1.5 per year over 65
   - BMI: +2% per point over 30
   - Chronic diseases: +15 per disease
   - Abnormal labs: +10-15 per abnormality
   - Medications: +2 per drug (interactions)

2. **Treatment recommendation**
   - Compare options by effectiveness & safety
   - Check contraindications
   - Reduce score if contraindicated
   - Penalize side effects for high-risk patients

3. **Risk levels**
   - Critical (>75): Immediate intervention
   - High (50-75): Urgent specialist referral
   - Medium (25-50): Close monitoring
   - Low (<25): Standard care

**Example**:
```
Patient: 72yo with diabetes
Age: +20 points
BMI 32: +4 points
Chronic disease (diabetes): +15 points
Abnormal glucose: +12 points
3 medications: +6 points
Total: 57 → HIGH RISK

Recommendation: "Monthly checkups + specialist referral"
Suggested treatment: Metformin (safe, proven effective)
Warnings: "Monitor for kidney function"
```

**Business Impact**:
- Preventive intervention (reduce ER visits)
- Identify high-risk patients early
- Personalized treatment plans

---

### MÉTIER 7: FRAUD DETECTION - MULTI-DIMENSIONAL ANOMALY
**File**: `07-metier-examples-advanced-2.ts`

**Problem**: How to catch fraud without blocking legitimate transactions?

**Algorithms**:
1. **Behavioral anomaly scoring**
   - Amount Z-score: deviation from user's average
   - Velocity check: Impossible travel (500 km/h limit)
   - Category anomaly: New merchant category
   - Time anomaly: Transactions outside normal hours
   - Device anomaly: New device/phone

2. **Structured fraud detection**
   - Detects "structuring" (multiple $9,999 transactions to avoid $10k threshold)
   - Pattern matching for known fraud schemes

3. **Action determination**
   - Approve (score <50): Low risk
   - Challenge (50-75): Request 2FA/biometric/security questions
   - Block (>75): Hold for manual review

**Example**:
```
Normal user:
- Typical transaction: $50-200
- Typical merchant: Coffee, groceries
- Typical location: New York
- Typical time: 8am-8pm
- Typical device: iPhone

Fraudulent transaction:
- $3,500 at jewelry store (3.2σ above mean)
- In Miami (impossible travel from NY in 10 mins)
- New device (Android)
- 2am transaction
- First jewelry purchase ever

Anomaly score: 92/100 → BLOCK
Action: "Hold for verification + contact customer"
```

**Business Impact**:
- 95%+ fraud detection rate
- Low false positive rate (real customers not blocked)
- Real-time decision making

---

### MÉTIER 8: RECOMMENDATION ENGINE - COLLABORATIVE FILTERING
**File**: `07-metier-examples-advanced-2.ts`

**Problem**: What products should we recommend?

**Algorithms**:
1. **User similarity (Cosine similarity)**
   - Compares ratings of two users on common items
   - Similarity = 0 (completely different) to 1 (identical taste)
   - Based on math: dot product / (magnitude1 × magnitude2)

2. **Collaborative filtering**
   - Find similar users
   - See what they rated highly
   - Recommend those items

3. **Predicted rating**
   - Average rating from similar users
   - Weighted by similarity score

**Example**:
```
User Alice's ratings:
- Movie A: 5 stars
- Movie B: 4 stars
- Movie C: 3 stars

User Bob's ratings:
- Movie A: 5 stars
- Movie B: 4 stars
(Haven't rated Movie C)

Similarity: 0.98 (very similar taste)

Alice hasn't seen Movie X (rated 5 stars by Bob)
Recommendation: "Movie X (predicted 4.9 stars)"
Reason: "Similar users rated highly"
```

**Business Impact**:
- 30% increase in click-through rate
- 25% increase in conversion
- Personalized user experience

---

## 🎨 Common Métier Patterns

### Pattern 1: Multi-Factor Scoring
Combine multiple signals into single score:
```typescript
score = factor1 * weight1 + factor2 * weight2 + factor3 * weight3
```
**Use cases**: Risk scoring, pricing, recommendations

### Pattern 2: Statistical Analysis
```typescript
Mean, StdDev, Z-score = (value - mean) / stdDev
```
**Use cases**: Anomaly detection, outlier identification

### Pattern 3: Optimization Algorithms
```typescript
- Greedy heuristics: Fast, good enough (routing)
- Linear programming: Optimal (resource allocation)
- Machine learning: Predictive (forecasting)
```

### Pattern 4: Time-Series Forecasting
```typescript
Exponential smoothing: F(t+1) = α × actual(t) + (1-α) × F(t)
```
**Use cases**: Demand forecasting, sales prediction

---

## 📈 Complexity Levels

| Métier | Complexity | Key Skills |
|--------|-----------|-----------|
| Dynamic Pricing | 3/5 | Math, demand curves |
| Route Optimization | 4/5 | Algorithms, optimization |
| Inventory Management | 3/5 | Statistics, forecasting |
| Compensation Engine | 2/5 | HR knowledge |
| Property Valuation | 3/5 | Finance, regression |
| Patient Risk Scoring | 4/5 | Healthcare domain |
| Fraud Detection | 5/5 | Statistical analysis |
| Recommendations | 4/5 | Linear algebra, ML |

---

## 🚀 Implementation Tips

1. **Start simple**: MVP with basic logic, add sophistication
2. **Test with real data**: Backtest on historical data first
3. **Monitor performance**: Track predictions vs actual
4. **Iterate**: Collect feedback, refine algorithms
5. **Document assumptions**: Why did you choose those formulas?
6. **Version control**: Track algorithm changes

---

## 📚 Learn More

- **Statistics**: Z-scores, standard deviation, correlation
- **Algorithms**: Greedy heuristics, dynamic programming, TSP
- **Linear Algebra**: Matrix operations, eigenvalues
- **Machine Learning**: Time series, clustering, classification
- **Domain knowledge**: Industry-specific best practices


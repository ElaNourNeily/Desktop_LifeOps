# 🔗 EXTERNAL APIS INTEGRATION GUIDE
## Mailer (Email) + Stripe (Payment)

---

## 📋 Why These 2 APIs?

| API | Purpose | Integration |
|-----|---------|-------------|
| **Mailer** | Send emails | Budget alerts, approvals, confirmations |
| **Stripe** | Process payments | Track expenses, create charges, refunds |

### Perfect for Budget Management:
✅ Email notifications when budget is nearly full  
✅ Alert managers of high-risk expenses  
✅ Confirm expense submissions  
✅ Track payment transactions  
✅ Create audit trail for compliance  

---

## 🚀 QUICK SETUP

### Step 1: Install Dependencies
```bash
npm install nodemailer stripe dotenv
npm install --save-dev @types/nodemailer
```

### Step 2: Setup Gmail (Free)
1. Go to https://myaccount.google.com/
2. Search "App passwords"
3. Select "Mail" and "Windows Computer"
4. Get your 16-character password

### Step 3: Setup Stripe (Free Test Account)
1. Go to https://stripe.com/
2. Click "Sign Up"
3. Create account
4. Go to Dashboard → Developers → API Keys
5. Copy Secret Key (starts with `sk_test_`)

### Step 4: Create .env File
```
# Email Configuration
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=xxxx xxxx xxxx xxxx

# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_51ABcDefGhI...
STRIPE_PUBLISHABLE_KEY=pk_test_51ABcDefGhI...

# Server
NODE_ENV=development
PORT=3000
```

### Step 5: Install Dependencies
```bash
npm install dotenv
```

### Step 6: Load Environment Variables
```typescript
import dotenv from 'dotenv';
dotenv.config();

const emailUser = process.env.EMAIL_USER;
const stripeKey = process.env.STRIPE_SECRET_KEY;
```

---

## 📧 EMAIL API - NODEMAILER

### What It Does
Sends transactional emails for budget notifications and expense approvals.

### Endpoints

#### 1. Send Budget Warning
```bash
POST /api/v1/notifications/budget-warning

{
  "email": "john@company.com",
  "categoryName": "Travel",
  "spent": 8000,
  "limit": 10000
}

Response:
{
  "success": true,
  "messageId": "<CAK+e5s...@mail.gmail.com>"
}
```

**When to Send**: Budget reaches 80%

#### 2. Send Critical Alert
```bash
POST /api/v1/notifications/budget-critical

{
  "email": "manager@company.com",
  "categoryName": "Software",
  "overageAmount": 5000,
  "limit": 20000
}

Response:
{
  "success": true,
  "messageId": "<CAK+e5s...@mail.gmail.com>"
}
```

**When to Send**: Budget exceeded by >25%

#### 3. Send Approval Request
```bash
POST /api/v1/notifications/approval-request

{
  "managerEmail": "manager@company.com",
  "managerName": "Alice Johnson",
  "expenseDescription": "Software license",
  "expenseAmount": 8500,
  "submitterName": "Bob Smith",
  "riskScore": 62,
  "approvalLink": "http://localhost:3000/approvals/exp_123"
}

Response:
{
  "success": true,
  "messageId": "<CAK+e5s...@mail.gmail.com>"
}
```

**When to Send**: Risk score > 40

#### 4. Send Confirmation
```bash
POST /api/v1/notifications/expense-confirmation

{
  "submitterEmail": "bob@company.com",
  "submitterName": "Bob Smith",
  "expenseId": "exp_123",
  "description": "Flight ticket",
  "amount": 450,
  "category": "Travel",
  "status": "approved"
}

Response:
{
  "success": true,
  "messageId": "<CAK+e5s...@mail.gmail.com>"
}
```

**When to Send**: Immediately after expense submission

---

## 💳 STRIPE API - PAYMENT PROCESSING

### What It Does
Processes payments, tracks transactions, and creates audit trail.

### Endpoints

#### 1. Create Charge
```bash
POST /api/v1/payments/charge

{
  "budgetId": "budget_123",
  "categoryId": "cat_software",
  "description": "Premium license",
  "amountCents": 850000,        # $8,500 in cents
  "token": "tok_visa",          # From Stripe.js
  "email": "bob@company.com"
}

Response:
{
  "success": true,
  "chargeId": "ch_1LwcP9ABcDeFghIjKlMn9opQ",
  "amount": 8500,
  "status": "succeeded"
}
```

**When to Use**: Approved expense ready for payment

#### 2. Get Transactions
```bash
GET /api/v1/payments/transactions?limit=10

Response:
{
  "success": true,
  "charges": [
    {
      "id": "ch_1LwcP9...",
      "amount": 8500,
      "status": "succeeded",
      "description": "Software license",
      "date": "2026-04-25T15:30:00Z"
    }
  ]
}
```

**Use for**: Reconciliation, audits, reports

#### 3. Refund Charge
```bash
POST /api/v1/payments/refund

{
  "chargeId": "ch_1LwcP9ABcDeFghIjKlMn9opQ"
}

Response:
{
  "success": true,
  "refundId": "re_1LwcP9ABcDeFghIjKlMn9opQ"
}
```

**When to Use**: Rejected expense, disputed charge

#### 4. Get Statistics
```bash
GET /api/v1/payments/stats

Response:
{
  "success": true,
  "totalCharges": 42,
  "totalAmount": 23456.75,
  "successfulCharges": 40,
  "failedCharges": 2,
  "successRate": "95.2%"
}
```

**Use for**: Dashboard, KPIs, financial reports

---

## 🔄 COMPLETE WORKFLOW

### Scenario: Approving $8,500 Software License

```
STEP 1: Employee submits expense
├─ POST /api/v1/expenses
├─ Amount: $8,500
└─ Risk Score: 62 (HIGH)

STEP 2: Send approval request email
├─ POST /api/v1/notifications/approval-request
├─ To: manager@company.com
└─ ✉️ Manager gets email with [Approve/Reject] link

STEP 3: Manager approves via email link
├─ Manager clicks "Approve" button
├─ Backend: POST /api/v1/expenses/exp_123/approve
└─ ✅ Approved

STEP 4: Create Stripe charge
├─ POST /api/v1/payments/charge
├─ Amount: 850000 (cents)
└─ Status: "succeeded" (charge created)

STEP 5: Send confirmation emails
├─ POST /api/v1/notifications/expense-confirmation
├─ To: bob@company.com (submitter)
└─ ✉️ Receipt email with transaction ID

STEP 6: Check budget impact
├─ Métier: Calculate new budget utilization
├─ Result: Software category now at 94%
└─ Send warning email

STEP 7: Complete
├─ Expense recorded in system
├─ Payment tracked in Stripe
├─ Audit trail created
└─ ✅ Done!
```

---

## 🧪 TESTING WITH CURL

### Test Email
```bash
# Send budget warning
curl -X POST http://localhost:3000/api/v1/notifications/budget-warning \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@gmail.com",
    "categoryName": "Travel",
    "spent": 8000,
    "limit": 10000
  }'

# Send approval request
curl -X POST http://localhost:3000/api/v1/notifications/approval-request \
  -H "Content-Type: application/json" \
  -d '{
    "managerEmail": "manager@gmail.com",
    "managerName": "John",
    "expenseDescription": "License",
    "expenseAmount": 500,
    "submitterName": "Bob",
    "riskScore": 50,
    "approvalLink": "http://localhost:3000/approve/123"
  }'

# Send confirmation
curl -X POST http://localhost:3000/api/v1/notifications/expense-confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "submitterEmail": "bob@gmail.com",
    "submitterName": "Bob",
    "expenseId": "exp_123",
    "description": "Flight",
    "amount": 450,
    "category": "Travel",
    "status": "approved"
  }'
```

### Test Payment
```bash
# Get transaction history
curl http://localhost:3000/api/v1/payments/transactions?limit=10

# Get payment stats
curl http://localhost:3000/api/v1/payments/stats

# Create charge (Stripe test token)
curl -X POST http://localhost:3000/api/v1/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "budgetId": "budget_123",
    "categoryId": "cat_software",
    "description": "License",
    "amountCents": 50000,
    "token": "tok_visa",
    "email": "bob@gmail.com"
  }'

# Refund charge
curl -X POST http://localhost:3000/api/v1/payments/refund \
  -H "Content-Type: application/json" \
  -d '{"chargeId": "ch_1LwcP9..."}'
```

---

## 📝 INTEGRATION CODE EXAMPLE

```typescript
// In your expense approval endpoint

import { BudgetMailerService, StripeExpenseService } from './11-external-apis-mailer-stripe';

const mailerService = new BudgetMailerService();
const stripeService = new StripeExpenseService();

app.post('/api/v1/expenses/:expenseId/approve', async (req, res) => {
  const expense = getExpense(req.params.expenseId);
  
  // 1. Create payment charge
  const charge = await stripeService.createChargeForExpense(
    expense.budgetId,
    expense.categoryId,
    expense.description,
    expense.amount * 100,  // Convert to cents
    req.body.stripeToken,
    expense.submitterEmail
  );
  
  if (!charge.success) {
    return res.status(400).json({ error: 'Payment failed' });
  }
  
  // 2. Send confirmation email
  await mailerService.sendExpenseConfirmation(
    expense.submitterEmail,
    expense.submitterName,
    expense.id,
    expense.description,
    expense.amount,
    expense.category,
    'approved'
  );
  
  // 3. Check budget and send alert if needed
  const budget = getBudget(expense.budgetId);
  const utilization = calculateUtilization(budget);
  
  if (utilization > 80) {
    await mailerService.sendBudgetWarning(
      budget.ownerEmail,
      expense.category,
      calculateSpent(budget),
      budget.totalLimit,
      utilization
    );
  }
  
  res.json({ success: true, chargeId: charge.chargeId });
});
```

---

## 🔒 SECURITY CHECKLIST

- [ ] Never commit `.env` file
- [ ] Never log Stripe keys
- [ ] Always use HTTPS in production
- [ ] Validate email addresses
- [ ] Rate limit notification endpoints
- [ ] Add CSRF protection
- [ ] Use Stripe webhooks for security
- [ ] Audit all payment transactions
- [ ] Monitor failed payments
- [ ] Implement 2FA for admin access

---

## 📊 COSTS

| Service | Free Tier | Cost |
|---------|-----------|------|
| **Gmail/Nodemailer** | Unlimited | Free |
| **Stripe** | Test mode | 2.9% + $0.30 per transaction |

Example: $8,500 expense
- Stripe fee: $247 + $0.30 = $247.30

---

## 🐛 TROUBLESHOOTING

### Email not sending
```
❌ Error: "Invalid login"
✅ Solution: 
- Use Gmail App Password (16 characters)
- Enable 2FA in Gmail
- Check EMAIL_USER matches Gmail address
```

### Stripe charge fails
```
❌ Error: "Invalid token"
✅ Solution:
- Use test token: tok_visa
- Check amountCents is in cents (not dollars)
- Verify STRIPE_SECRET_KEY is correct
```

### Rate limiting
```
❌ Too many emails sent
✅ Solution:
- Implement email queue
- Add rate limiting: 10 emails/min per user
- Use Stripe webhooks instead of polling
```

---

## 📚 FILES & STRUCTURE

```
11-external-apis-mailer-stripe.ts     ← Implementation
12-external-apis-examples.ts          ← Examples
HOW_TO_USE_EXTERNAL_APIS.md           ← This file

Integration Points:
├─ 03-api-expense-approval.ts         ← Call Mailer + Stripe
├─ 04-server-setup.ts                 ← Register external routers
└─ 10-complete-working-server.ts      ← Already includes setup
```

---

## ✅ PRODUCTION CHECKLIST

- [ ] Move API keys to Secrets Manager
- [ ] Set up email templates in database
- [ ] Implement email queue (Bull/Redis)
- [ ] Add webhook verification for Stripe
- [ ] Set up monitoring and alerts
- [ ] Create backup email service
- [ ] Test failover scenarios
- [ ] Document API response codes
- [ ] Add unit tests for both services
- [ ] Set up CI/CD pipeline

---

## 🎓 NEXT STEPS

1. ✅ Install dependencies
2. ✅ Setup Gmail & Stripe
3. ✅ Create .env file
4. ✅ Test endpoints with curl
5. ✅ Integrate with approval workflow
6. ✅ Test end-to-end flow
7. ✅ Deploy to production
8. ✅ Monitor and optimize


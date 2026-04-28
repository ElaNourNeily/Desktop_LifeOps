/**
 * EXTERNAL API EXAMPLES - MAILER & STRIPE
 * Real request/response examples
 */

// ============================================
// EXAMPLE 1: SEND BUDGET WARNING EMAIL
// ============================================

const budgetWarningExample = {
  title: 'Send Budget Warning Email',
  endpoint: 'POST /api/v1/notifications/budget-warning',
  complexity: 2,

  request: {
    email: 'john@company.com',
    categoryName: 'Travel',
    spent: 8000,
    limit: 10000,
  },

  response: {
    message: 'Budget warning email sent',
    success: true,
    messageId: '<CAK+e5s...@mail.gmail.com>',
  },

  emailContent: {
    subject: 'Budget Alert: Travel at 80%',
    body: `
      ⚠️ Budget Warning
      
      Your Travel category has reached 80% of its limit.
      
      - Spent: $8,000
      - Limit: $10,000
      - Remaining: $2,000
      
      Please review your spending to avoid overage.
    `,
  },
};

// ============================================
// EXAMPLE 2: SEND CRITICAL BUDGET ALERT
// ============================================

const budgetCriticalExample = {
  title: 'Send Critical Budget Alert',
  endpoint: 'POST /api/v1/notifications/budget-critical',
  complexity: 2,

  request: {
    email: 'manager@company.com',
    categoryName: 'Software Licenses',
    overageAmount: 5000,
    limit: 20000,
  },

  response: {
    message: 'Critical budget alert sent',
    success: true,
    messageId: '<CAK+e5s...@mail.gmail.com>',
  },

  emailContent: {
    subject: '🚨 CRITICAL: Budget Exceeded - Software Licenses',
    body: `
      🚨 CRITICAL BUDGET ALERT
      
      Your Software Licenses category has EXCEEDED its budget limit!
      
      - Overage: $5,000
      - Over limit by: 25%
      
      ⚠️ Immediate action required!
      Please reduce spending immediately or request budget reallocation.
      
      Contact your finance manager for assistance.
    `,
  },
};

// ============================================
// EXAMPLE 3: SEND APPROVAL REQUEST EMAIL
// ============================================

const approvalRequestExample = {
  title: 'Send Expense Approval Request',
  endpoint: 'POST /api/v1/notifications/approval-request',
  complexity: 2,

  request: {
    managerEmail: 'manager@company.com',
    managerName: 'Alice Johnson',
    expenseDescription: 'Premium software license renewal',
    expenseAmount: 8500,
    submitterName: 'Bob Smith',
    riskScore: 62,
    approvalLink: 'http://localhost:3000/approvals/exp_123456789',
  },

  response: {
    message: 'Approval request email sent',
    success: true,
    messageId: '<CAK+e5s...@mail.gmail.com>',
  },

  emailContent: {
    subject: 'Expense Approval Needed: $8,500 (🔴 HIGH)',
    body: `
      Expense Approval Required
      
      Hi Alice Johnson,
      
      Bob Smith has submitted an expense for approval:
      
      | Description | Premium software license renewal |
      | Amount | $8,500 |
      | Risk Level | 🔴 HIGH (62/100) |
      
      [Review & Approve]
      
      This is an automatic notification.
    `,
  },
};

// ============================================
// EXAMPLE 4: SEND EXPENSE CONFIRMATION
// ============================================

const expenseConfirmationExample = {
  title: 'Send Expense Confirmation',
  endpoint: 'POST /api/v1/notifications/expense-confirmation',
  complexity: 2,

  request: {
    submitterEmail: 'bob@company.com',
    submitterName: 'Bob Smith',
    expenseId: 'exp_1682514200',
    description: 'Flight NYC to SF for client meeting',
    amount: 450,
    category: 'Travel',
    status: 'approved',
  },

  response: {
    message: 'Expense confirmation email sent',
    success: true,
    messageId: '<CAK+e5s...@mail.gmail.com>',
  },

  emailContent: {
    subject: '✅ Expense Confirmed: exp_1682514200',
    body: `
      Expense Confirmation
      
      Hi Bob Smith,
      
      Your expense has been recorded in the system:
      
      | Expense ID | exp_1682514200 |
      | Description | Flight NYC to SF for client meeting |
      | Amount | $450 |
      | Category | Travel |
      | Status | ✅ APPROVED |
      
      Thank you for your submission!
    `,
  },
};

// ============================================
// EXAMPLE 5: CREATE STRIPE CHARGE
// ============================================

const stripeChargeExample = {
  title: 'Create Stripe Payment Charge',
  endpoint: 'POST /api/v1/payments/charge',
  complexity: 4,

  request: {
    budgetId: 'budget_1682514000',
    categoryId: 'cat_software',
    description: 'Premium software license',
    amountCents: 850000, // $8,500 in cents
    token: 'tok_visa', // Stripe test token
    email: 'bob@company.com',
  },

  response: {
    message: 'Payment processed',
    success: true,
    chargeId: 'ch_1LwcP9ABcDeFghIjKlMn9opQ',
    amount: 8500,
    status: 'succeeded',
  },

  workflow: `
    1. Expense submitted with risk score 62 (HIGH)
    2. Manager approves via email link
    3. Payment token provided by frontend
    4. Charge created via Stripe API
    5. Confirmation email sent to submitter
    6. Receipt generated and sent
  `,
};

// ============================================
// EXAMPLE 6: GET TRANSACTION HISTORY
// ============================================

const transactionHistoryExample = {
  title: 'Get Transaction History',
  endpoint: 'GET /api/v1/payments/transactions?limit=10',
  complexity: 2,

  response: {
    message: 'Transaction history retrieved',
    success: true,
    charges: [
      {
        id: 'ch_1LwcP9ABcDeFghIjKlMn9opQ',
        amount: 8500,
        currency: 'usd',
        status: 'succeeded',
        description: 'Premium software license',
        email: 'bob@company.com',
        date: '2026-04-25T15:30:00.000Z',
        metadata: {
          budgetId: 'budget_1682514000',
          categoryId: 'cat_software',
          submitterEmail: 'bob@company.com',
        },
      },
      {
        id: 'ch_1LwcP8ABcDeFghIjKlMn9opR',
        amount: 450,
        currency: 'usd',
        status: 'succeeded',
        description: 'Flight NYC to SF',
        email: 'alice@company.com',
        date: '2026-04-24T10:15:00.000Z',
        metadata: {
          budgetId: 'budget_1682514000',
          categoryId: 'cat_travel',
          submitterEmail: 'alice@company.com',
        },
      },
      // ... more transactions
    ],
  },

  useCases: [
    'Reconciliation with accounting',
    'Audit trail for expenses',
    'Monthly reporting',
    'Fraud detection',
  ],
};

// ============================================
// EXAMPLE 7: REFUND A CHARGE
// ============================================

const refundChargeExample = {
  title: 'Refund a Charge',
  endpoint: 'POST /api/v1/payments/refund',
  complexity: 3,

  request: {
    chargeId: 'ch_1LwcP9ABcDeFghIjKlMn9opQ',
  },

  response: {
    message: 'Refund processed',
    success: true,
    refundId: 're_1LwcP9ABcDeFghIjKlMn9opQ',
  },

  scenario: `
    1. Expense rejected by manager
    2. Refund initiated automatically
    3. Stripe processes refund (5-10 business days)
    4. Email notification sent to submitter
    5. Expense marked as "refunded" in system
  `,
};

// ============================================
// EXAMPLE 8: GET PAYMENT STATISTICS
// ============================================

const paymentStatsExample = {
  title: 'Get Payment Statistics',
  endpoint: 'GET /api/v1/payments/stats',
  complexity: 2,

  response: {
    message: 'Payment statistics',
    success: true,
    totalCharges: 42,
    totalAmount: 23456.75,
    successfulCharges: 40,
    failedCharges: 2,

    insights: {
      successRate: '95.2%',
      avgTransaction: 561.35,
      largestTransaction: 8500,
      smallestTransaction: 45,
      mostCommonAmount: 450,
    },
  },

  reportingUse: [
    'Monthly financial reports',
    'Dashboard KPIs',
    'CFO presentations',
    'Budget forecasting',
  ],
};

// ============================================
// COMPLETE WORKFLOW EXAMPLE
// ============================================

const completeWorkflowExample = {
  title: 'Complete Workflow: Expense to Payment',

  steps: [
    {
      step: 1,
      action: 'Employee submits expense',
      endpoint: 'POST /api/v1/expenses',
      data: {
        budgetId: 'budget_123',
        description: 'Software license',
        amount: 8500,
        categoryId: 'cat_software',
      },
    },

    {
      step: 2,
      action: 'Risk scoring calculated',
      riskScore: 62,
      requires: 'Manager approval',
    },

    {
      step: 3,
      action: 'Send approval request email',
      endpoint: 'POST /api/v1/notifications/approval-request',
      recipient: 'manager@company.com',
      status: '✉️ Email sent',
    },

    {
      step: 4,
      action: 'Manager reviews & approves',
      link: 'http://localhost:3000/approvals/exp_123',
      status: '✅ Approved',
    },

    {
      step: 5,
      action: 'Create payment charge',
      endpoint: 'POST /api/v1/payments/charge',
      data: {
        chargeId: 'ch_123...',
        status: 'succeeded',
        amount: 8500,
      },
    },

    {
      step: 6,
      action: 'Send confirmation email',
      endpoint: 'POST /api/v1/notifications/expense-confirmation',
      recipient: 'bob@company.com',
      status: '✉️ Confirmation sent',
    },

    {
      step: 7,
      action: 'Check budget warning',
      endpoint: 'POST /api/v1/notifications/budget-warning',
      percentage: '94%',
      status: '⚠️ Warning sent to finance team',
    },

    {
      step: 8,
      action: 'Complete',
      finalStatus: 'Expense paid and tracked',
    },
  ],

  timeline: {
    '00:00': 'Expense submitted',
    '00:05': 'Risk score: 62 (high)',
    '00:06': 'Approval email sent',
    '02:15': 'Manager approves via email',
    '02:20': 'Stripe charge created',
    '02:21': 'Confirmation email sent',
    '02:25': 'Budget warning email sent (94% utilized)',
    '24:00': 'Payment settled in bank',
  },
};

// ============================================
// ENV VARIABLES NEEDED
// ============================================

const environmentSetup = {
  title: 'Environment Setup',

  requiredEnvVars: {
    email: {
      EMAIL_USER: 'your-email@gmail.com',
      EMAIL_PASSWORD: 'your-app-password', // Not actual password!
      note: 'Use Google App Password (2FA required)',
    },

    stripe: {
      STRIPE_SECRET_KEY: 'sk_test_4eC39HqLyjWDarhtT657',
      STRIPE_PUBLISHABLE_KEY: 'pk_test_51ABcDefGhI...',
      note: 'Get from Stripe Dashboard',
    },
  },

  setup: `
    # 1. Create .env file
    EMAIL_USER=your-email@gmail.com
    EMAIL_PASSWORD=your-app-password
    STRIPE_SECRET_KEY=sk_test_...
    STRIPE_PUBLISHABLE_KEY=pk_test_...

    # 2. Install packages
    npm install nodemailer stripe

    # 3. Get credentials
    - Gmail: https://myaccount.google.com/apppasswords
    - Stripe: https://dashboard.stripe.com/apikeys
  `,
};

// ============================================
// INTEGRATION WITH EXISTING SYSTEM
// ============================================

const systemIntegration = {
  title: 'How External APIs Fit Into the System',

  architecture: `
    User submits expense
         ↓
    Métier: Risk scoring (06)
         ↓
    API: Create expense (03)
         ↓
    EXTERNAL: Send approval email (Mailer) ← NEW
         ↓
    Manager reviews & approves
         ↓
    EXTERNAL: Create Stripe charge (Stripe) ← NEW
         ↓
    EXTERNAL: Send confirmation email (Mailer) ← NEW
         ↓
    Métier: Check budget warning (01)
         ↓
    EXTERNAL: Send budget alert (Mailer) ← NEW
         ↓
    Complete
  `,

  files: [
    '01-budget-domain-model.ts ← Budget logic',
    '03-api-expense-approval.ts ← Expense API',
    '11-external-apis-mailer-stripe.ts ← NEW: Email + Payment',
  ],

  integration: `
    // In 03-api-expense-approval.ts, when approving expense:
    
    1. Save approval
    2. Create Stripe charge
       → POST /api/v1/payments/charge
    3. Send confirmation
       → POST /api/v1/notifications/expense-confirmation
    4. Check budget
    5. If warning level, send alert
       → POST /api/v1/notifications/budget-warning
  `,
};

export {
  budgetWarningExample,
  budgetCriticalExample,
  approvalRequestExample,
  expenseConfirmationExample,
  stripeChargeExample,
  transactionHistoryExample,
  refundChargeExample,
  paymentStatsExample,
  completeWorkflowExample,
  environmentSetup,
  systemIntegration,
};

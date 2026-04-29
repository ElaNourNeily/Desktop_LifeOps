/**
 * PRACTICAL EXAMPLES & TEST SCENARIOS
 * Real-world usage patterns and data examples
 */

// ============================================
// EXAMPLE 1: Creating and Managing a Budget
// ============================================

const budgetExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/budgets',
    body: {
      name: 'Q1 2026 Operating Budget',
      totalLimit: 50000,
      categories: [
        {
          categoryId: 'cat_travel',
          name: 'Travel',
          limit: 10000,
          priority: 'high',
        },
        {
          categoryId: 'cat_office',
          name: 'Office Supplies',
          limit: 5000,
          priority: 'medium',
        },
        {
          categoryId: 'cat_software',
          name: 'Software Licenses',
          limit: 20000,
          priority: 'critical',
        },
        {
          categoryId: 'cat_events',
          name: 'Team Events',
          limit: 15000,
          priority: 'low',
        },
      ],
      period: {
        startDate: '2026-01-01',
        endDate: '2026-03-31',
      },
    },
  },

  response: {
    id: 'budget_1682514000',
    name: 'Q1 2026 Operating Budget',
    totalLimit: 50000,
    categories: [
      {
        categoryId: 'cat_travel',
        name: 'Travel',
        limit: 10000,
        spent: 0,
        percentage: 20,
        priority: 'high',
      },
      // ... other categories
    ],
    status: 'active',
  },
};

// ============================================
// EXAMPLE 2: Submitting Expense (Normal)
// ============================================

const normalExpenseExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/expenses',
    body: {
      budgetId: 'budget_1682514000',
      description: 'McDonald\'s lunch',
      amount: 15.99,
      // categoryId not provided - will be auto-categorized
      date: '2026-02-15',
      tags: ['lunch', 'fast-food'],
      receipt: 'https://receipts.example.com/exp_123.pdf',
    },
  },

  response: {
    expense: {
      id: 'exp_1682514100',
      description: 'McDonald\'s lunch',
      amount: 15.99,
      categoryId: 'cat_food', // Auto-categorized
      budgetId: 'budget_1682514000',
      date: '2026-02-15T00:00:00.000Z',
      status: 'approved',
      tags: ['lunch', 'fast-food'],
    },
    approval: {
      approvalPath: 'auto',
      requiredApprovals: 0,
      reasoning: ['Low risk, auto-approved'],
      riskScore: 15,
    },
    alert: {
      alertLevel: 'info',
      title: 'Expense Logged',
      message: 'McDonald\'s lunch - 15.99',
      suggestedAction: 'No action needed',
    },
    autoApproved: true,
    autoCategorization: {
      categoryName: 'Food',
      confidence: 50,
      matchedKeywords: ['mcdonald'],
    },
  },
};

// ============================================
// EXAMPLE 2.5: Submitting Expense with Auto-Categorization (Transport)
// ============================================

const transportExpenseExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/expenses',
    body: {
      budgetId: 'budget_1682514000',
      description: 'Uber ride to airport',
      amount: 25.50,
      // categoryId not provided - will be auto-categorized
      date: '2026-02-16',
      tags: ['transport', 'airport'],
    },
  },

  response: {
    expense: {
      id: 'exp_1682514150',
      description: 'Uber ride to airport',
      amount: 25.50,
      categoryId: 'cat_transport', // Auto-categorized
      budgetId: 'budget_1682514000',
      date: '2026-02-16T00:00:00.000Z',
      status: 'approved',
      tags: ['transport', 'airport'],
    },
    approval: {
      approvalPath: 'auto',
      requiredApprovals: 0,
      reasoning: ['Low risk, auto-approved'],
      riskScore: 10,
    },
    alert: {
      alertLevel: 'info',
      title: 'Expense Logged',
      message: 'Uber ride to airport - 25.50',
      suggestedAction: 'No action needed',
    },
    autoApproved: true,
    autoCategorization: {
      categoryName: 'Transport',
      confidence: 50,
      matchedKeywords: ['uber'],
    },
  },
};

// ============================================
// EXAMPLE 3: Submitting Expense (High Risk)
// ============================================

const highRiskExpenseExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/expenses',
    body: {
      budgetId: 'budget_1682514000',
      description: 'Premium software license',
      amount: 8500,
      categoryId: 'cat_software',
      date: '2026-02-20',
      tags: ['enterprise', 'annual'],
      // Note: No receipt provided
    },
  },

  response: {
    expense: {
      id: 'exp_1682514200',
      description: 'Premium software license',
      amount: 8500,
      categoryId: 'cat_software',
      status: 'pending',
      date: '2026-02-20T00:00:00.000Z',
    },
    approval: {
      approvalPath: 'director',
      requiredApprovals: 2,
      reasoning: [
        'High risk score: 62',
        'Large amount: 17% of total budget',
        'Missing documentation (receipt)',
      ],
      riskScore: 62,
    },
    alert: {
      alertLevel: 'critical',
      title: 'High Risk Expense Detected',
      message:
        'Risk score: 62. Amount: 8500, Missing receipt: true. Deviates significantly from typical expenses.',
      suggestedAction: 'Request additional documentation before approval',
    },
    autoApproved: false,
  },
};

// ============================================
// EXAMPLE 4: Get Budget Forecast
// ============================================

const forecastExample = {
  request: {
    method: 'GET',
    endpoint: '/api/v1/budgets/budget_1682514000/forecast',
  },

  response: {
    budgetId: 'budget_1682514000',
    forecasts: [
      {
        category: 'Travel',
        currentSpent: 1250,
        predictedMonthlySpending: 1875,
        confidence: '78%',
        trend: 'increasing',
        projectedUtilization: '18%',
        willOverspend: false,
        recommendation: 'On track',
      },
      {
        category: 'Office Supplies',
        currentSpent: 420,
        predictedMonthlySpending: 525,
        confidence: '92%',
        trend: 'stable',
        projectedUtilization: '10%',
        willOverspend: false,
        recommendation: 'On track',
      },
      {
        category: 'Software Licenses',
        currentSpent: 8500,
        predictedMonthlySpending: 12000,
        confidence: '65%',
        trend: 'increasing',
        projectedUtilization: '60%',
        willOverspend: false,
        recommendation: 'On track',
      },
      {
        category: 'Team Events',
        currentSpent: 3200,
        predictedMonthlySpending: 5800,
        confidence: '71%',
        trend: 'increasing',
        projectedUtilization: '38%',
        willOverspend: false,
        recommendation: 'On track',
      },
    ],
    timestamp: '2026-02-20T14:30:00.000Z',
  },
};

// ============================================
// EXAMPLE 5: Detect Anomalies
// ============================================

const anomaliesExample = {
  request: {
    method: 'GET',
    endpoint: '/api/v1/budgets/budget_1682514000/anomalies',
  },

  response: {
    budgetId: 'budget_1682514000',
    anomalies: [
      {
        category: 'Travel',
        anomaliesCount: 1,
        anomalies: [
          {
            expenseId: 'exp_1682514500',
            anomalyScore: 3.2,
            reason:
              'Amount (3500) deviates 3.2σ from mean (850). Likely international flight vs domestic.',
          },
        ],
      },
      {
        category: 'Office Supplies',
        anomaliesCount: 0,
        anomalies: [],
      },
    ],
    totalAnomalies: 1,
  },
};

// ============================================
// EXAMPLE 6: Optimize Budget Allocation
// ============================================

const optimizationExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/budgets/budget_1682514000/optimize',
  },

  response: {
    budgetId: 'budget_1682514000',
    optimizationDate: '2026-02-20T14:30:00.000Z',
    changes: [
      {
        categoryName: 'Travel',
        currentLimit: 10000,
        recommendedLimit: 11250,
        change: 1250,
        changePercentage: '12.5',
      },
      {
        categoryName: 'Office Supplies',
        currentLimit: 5000,
        recommendedLimit: 4200,
        change: -800,
        changePercentage: '-16.0',
      },
      {
        categoryName: 'Software Licenses',
        currentLimit: 20000,
        recommendedLimit: 24500,
        change: 4500,
        changePercentage: '22.5',
      },
      {
        categoryName: 'Team Events',
        currentLimit: 15000,
        recommendedLimit: 13050,
        change: -1950,
        changePercentage: '-13.0',
      },
    ],
    newTotalLimit: 53000,
  },
};

// ============================================
// EXAMPLE 7: Pending Approvals (Manager View)
// ============================================

const pendingApprovalsExample = {
  request: {
    method: 'GET',
    endpoint: '/api/v1/expenses/pending-approvals?priority=risk',
  },

  response: {
    totalPending: 5,
    criticalCount: 2,
    mediumCount: 2,
    expenses: [
      {
        expenseId: 'exp_1682514200',
        description: 'Premium software license',
        amount: 8500,
        riskScore: 62,
        riskLevel: 'critical',
        alerts: [
          {
            alertLevel: 'critical',
            title: 'High Risk Expense Detected',
            message: 'Risk score: 62. Amount: 8500, Missing receipt: true',
            suggestedAction: 'Request additional documentation',
          },
        ],
        submittedDate: '2026-02-20T10:00:00.000Z',
      },
      {
        expenseId: 'exp_1682514300',
        description: 'Team lunch event supplies',
        amount: 3200,
        riskScore: 45,
        riskLevel: 'medium',
        alerts: [
          {
            alertLevel: 'warning',
            title: 'Category Budget Warning',
            message: 'Team Events will exceed limit by 18%',
            suggestedAction: 'Consider adjustment or defer expense',
          },
        ],
        submittedDate: '2026-02-19T15:30:00.000Z',
      },
      // ... more expenses
    ],
  },
};

// ============================================
// EXAMPLE 8: Approve Expense
// ============================================

const approveExpenseExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/expenses/exp_1682514200/approve',
    body: {
      approver: 'manager_john@company.com',
      decision: 'approved_with_conditions',
    },
  },

  response: {
    message: 'Expense approved',
    approvalCount: 1,
    expense: {
      id: 'exp_1682514200',
      description: 'Premium software license',
      amount: 8500,
      status: 'approved',
    },
    approvalHistory: [
      {
        approver: 'manager_john@company.com',
        timestamp: '2026-02-20T15:45:00.000Z',
        decision: 'approved_with_conditions',
      },
    ],
  },
};

// ============================================
// EXAMPLE 9: Bulk Review Expenses
// ============================================

const bulkReviewExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/expenses/bulk-review',
    body: {
      expenseIds: [
        'exp_1682514100',
        'exp_1682514150',
        'exp_1682514175',
      ],
      action: 'approve',
      approver: 'admin_system@company.com',
    },
  },

  response: {
    processedCount: 3,
    approvedCount: 3,
    rejectedCount: 0,
    totalAmount: 2850.5,
    results: [
      {
        expenseId: 'exp_1682514100',
        status: 'approved',
        amount: 450,
      },
      {
        expenseId: 'exp_1682514150',
        status: 'approved',
        amount: 1200,
      },
      {
        expenseId: 'exp_1682514175',
        status: 'approved',
        amount: 1200.5,
      },
    ],
  },
};

// ============================================
// EXAMPLE 10: Expense Statistics
// ============================================

const statisticsExample = {
  request: {
    method: 'GET',
    endpoint:
      '/api/v1/expenses/statistics?budgetId=budget_1682514000&timeframe=month',
  },

  response: {
    timeframe: 'month',
    totalExpenses: 42,
    totalAmount: '23456.75',
    averageAmount: '558.49',
    byStatus: {
      approved: 38,
      pending: 3,
      rejected: 1,
    },
    byCategory: {
      cat_travel: {
        count: 8,
        amount: 5400,
      },
      cat_office: {
        count: 12,
        amount: 2150,
      },
      cat_software: {
        count: 15,
        amount: 12500,
      },
      cat_events: {
        count: 7,
        amount: 3406.75,
      },
    },
  },
};

// ============================================
// EXAMPLE 11: Get User Alerts
// ============================================

const getAlertsExample = {
  request: {
    method: 'GET',
    endpoint: '/api/v1/alerts/1',
  },

  response: {
    alerts: [
      {
        id: 1,
        type: 'budget_threshold',
        title: 'Seuil de budget dépassé',
        message: 'Vous avez utilisé 85% de votre budget Mars 2026 ⚠️',
        severity: 'warning',
        userId: 1,
        budgetId: 1,
        expenseId: null,
        dateCreated: '2026-04-28T10:00:00.000Z',
        isRead: false,
        dueDate: null,
      },
      {
        id: 2,
        type: 'reminder',
        title: 'Rappel: Facture STEG',
        message: 'Facture STEG aujourd\'hui - Montant: 45.50 DT',
        severity: 'info',
        userId: 1,
        budgetId: null,
        expenseId: null,
        dateCreated: '2026-04-28T09:00:00.000Z',
        isRead: false,
        dueDate: '2026-04-28T00:00:00.000Z',
      },
    ],
    total: 2,
    unread: 2,
  },
};

// ============================================
// EXAMPLE 12: Create Reminder
// ============================================

const createReminderExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/reminders',
    body: {
      title: 'Facture STEG',
      description: 'Paiement mensuel électricité',
      dueDate: '2026-05-01',
      frequency: 'monthly',
      category: 'facture',
      amount: 45.50,
      userId: 1,
    },
  },

  response: {
    id: 1682678400000,
    title: 'Facture STEG',
    description: 'Paiement mensuel électricité',
    dueDate: '2026-05-01T00:00:00.000Z',
    frequency: 'monthly',
    category: 'facture',
    amount: 45.50,
    userId: 1,
    isActive: true,
    dateCreated: '2026-04-28T12:00:00.000Z',
    lastNotified: null,
  },
};

// ============================================
// EXAMPLE 13: Check Alerts
// ============================================

const checkAlertsExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/alerts/check/1',
  },

  response: {
    message: 'Alert checks completed',
    newAlertsGenerated: 1,
    alerts: [
      {
        id: 1682678500000,
        type: 'budget_threshold',
        title: 'Seuil de budget dépassé',
        message: 'Vous avez utilisé 85% de votre budget Mars 2026 ⚠️',
        severity: 'warning',
        userId: 1,
        budgetId: 1,
        expenseId: null,
        dateCreated: '2026-04-28T12:05:00.000Z',
        isRead: false,
        dueDate: null,
      },
    ],
  },
};

// ============================================
// EXAMPLE 14: Check Budget Alerts
// ============================================

const checkBudgetAlertsExample = {
  request: {
    method: 'POST',
    endpoint: '/api/v1/budgets/check-alerts/budget_123',
    body: {
      totalSpent: 850,
      budgetLimit: 1000
    },
  },

  response: {
    alertTriggered: true,
    severity: 'warning',
    message: 'Vous avez utilisé 85.0% de votre budget ⚠️',
    utilizationPercentage: '85.0',
    recommendation: 'Surveillez vos dépenses de près'
  },
};

export {
  budgetExample,
  normalExpenseExample,
  transportExpenseExample,
  highRiskExpenseExample,
  forecastExample,
  anomaliesExample,
  optimizationExample,
  pendingApprovalsExample,
  approveExpenseExample,
  bulkReviewExample,
  statisticsExample,
  getAlertsExample,
  createReminderExample,
  checkAlertsExample,
  checkBudgetAlertsExample,
};
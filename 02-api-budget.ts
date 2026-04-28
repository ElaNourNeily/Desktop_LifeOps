/**
 * API 1: Budget Management API
 * RESTful endpoints for budget CRUD and analysis
 */

import express, { Router, Request, Response } from 'express';
import {
  Budget,
  Expense,
  BudgetForecastingEngine,
  ExpenseApprovalEngine,
} from './01-budget-domain-model';

const router = Router();

// Mock database
const budgets: Map<string, Budget> = new Map();
const expenses: Map<string, Expense> = new Map();

const forecastEngine = new BudgetForecastingEngine();
const approvalEngine = new ExpenseApprovalEngine();

// ============================================
// API 1: BUDGET ENDPOINTS
// ============================================

/**
 * GET /api/v1/budgets/:budgetId
 * Retrieve budget with advanced analytics
 */
router.get('/api/v1/budgets/:budgetId', (req: Request, res: Response) => {
  const { budgetId } = req.params;
  const budget = budgets.get(budgetId);

  if (!budget) {
    return res.status(404).json({ error: 'Budget not found' });
  }

  // Get all expenses for this budget
  const budgetExpenses = Array.from(expenses.values()).filter(
    (e) => e.budgetId === budgetId
  );

  // Calculate category spending
  const categoriesWithSpending = budget.categories.map((category) => {
    const categoryExpenses = budgetExpenses.filter(
      (e) => e.categoryId === category.categoryId
    );
    const spent = categoryExpenses.reduce((sum, e) => sum + e.amount, 0);
    const remaining = category.limit - spent;
    const utilization = (spent / category.limit) * 100;

    return {
      ...category,
      spent,
      remaining,
      utilization: utilization.toFixed(2),
      status:
        utilization > 100
          ? 'overspent'
          : utilization > 80
            ? 'warning'
            : 'healthy',
    };
  });

  res.json({
    budget: { ...budget, categories: categoriesWithSpending },
    summary: {
      totalSpent: budgetExpenses.reduce((sum, e) => sum + e.amount, 0),
      totalRemaining:
        budget.totalLimit -
        budgetExpenses.reduce((sum, e) => sum + e.amount, 0),
      utilization: (
        (budgetExpenses.reduce((sum, e) => sum + e.amount, 0) /
          budget.totalLimit) *
        100
      ).toFixed(2),
    },
  });
});

/**
 * POST /api/v1/budgets
 * Create new budget
 */
router.post('/api/v1/budgets', (req: Request, res: Response) => {
  const { name, totalLimit, categories, period } = req.body;

  if (!name || !totalLimit || !categories) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  const budget: Budget = {
    id: `budget_${Date.now()}`,
    name,
    totalLimit,
    currency: 'USD',
    categories: categories.map((cat: any) => ({
      categoryId: cat.categoryId,
      name: cat.name,
      limit: cat.limit,
      spent: 0,
      percentage: (cat.limit / totalLimit) * 100,
      priority: cat.priority || 'medium',
    })),
    period: {
      startDate: new Date(period.startDate),
      endDate: new Date(period.endDate),
    },
    alerts: [],
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  budgets.set(budget.id, budget);
  res.status(201).json(budget);
});

/**
 * GET /api/v1/budgets/:budgetId/forecast
 * Get spending forecast with ML predictions
 */
router.get('/api/v1/budgets/:budgetId/forecast', (req: Request, res: Response) => {
  const { budgetId } = req.params;
  const budget = budgets.get(budgetId);

  if (!budget) {
    return res.status(404).json({ error: 'Budget not found' });
  }

  const budgetExpenses = Array.from(expenses.values()).filter(
    (e) => e.budgetId === budgetId
  );

  const forecasts = budget.categories.map((category) => {
    const categoryExpenses = budgetExpenses.filter(
      (e) => e.categoryId === category.categoryId
    );

    const forecast = forecastEngine.predictMonthlySpending(
      category.categoryId,
      categoryExpenses
    );

    const projectedUtilization = (forecast.predictedAmount / category.limit) * 100;
    const willOverspend = projectedUtilization > 100;

    return {
      category: category.name,
      currentSpent: categoryExpenses.reduce((sum, e) => sum + e.amount, 0),
      predictedMonthlySpending: forecast.predictedAmount,
      confidence: `${(forecast.confidence * 100).toFixed(0)}%`,
      trend: forecast.trend,
      projectedUtilization: `${projectedUtilization.toFixed(0)}%`,
      willOverspend,
      recommendation: willOverspend
        ? 'Consider reallocating budget or reducing spending'
        : 'On track',
    };
  });

  res.json({ budgetId, forecasts, timestamp: new Date().toISOString() });
});

/**
 * GET /api/v1/budgets/:budgetId/anomalies
 * Detect unusual spending patterns
 */
router.get('/api/v1/budgets/:budgetId/anomalies', (req: Request, res: Response) => {
  const { budgetId } = req.params;
  const budget = budgets.get(budgetId);

  if (!budget) {
    return res.status(404).json({ error: 'Budget not found' });
  }

  const budgetExpenses = Array.from(expenses.values()).filter(
    (e) => e.budgetId === budgetId
  );

  const anomalies = budget.categories.map((category) => {
    const categoryExpenses = budgetExpenses.filter(
      (e) => e.categoryId === category.categoryId
    );

    const detected = forecastEngine.detectAnomalies(categoryExpenses);

    return {
      category: category.name,
      anomaliesCount: detected.length,
      anomalies: detected,
    };
  });

  res.json({
    budgetId,
    anomalies: anomalies.filter((a) => a.anomaliesCount > 0),
    totalAnomalies: anomalies.reduce((sum, a) => sum + a.anomaliesCount, 0),
  });
});

/**
 * POST /api/v1/budgets/:budgetId/optimize
 * Get AI-driven budget optimization recommendations
 */
router.post('/api/v1/budgets/:budgetId/optimize', (req: Request, res: Response) => {
  const { budgetId } = req.params;
  const budget = budgets.get(budgetId);

  if (!budget) {
    return res.status(404).json({ error: 'Budget not found' });
  }

  const budgetExpenses = Array.from(expenses.values()).filter(
    (e) => e.budgetId === budgetId
  );

  const optimized = forecastEngine.optimizeBudgetAllocation(budget, budgetExpenses);

  const changes = optimized.map((optimizedCat, idx) => {
    const original = budget.categories[idx];
    return {
      categoryName: optimizedCat.name,
      currentLimit: original.limit,
      recommendedLimit: optimizedCat.limit,
      change: optimizedCat.limit - original.limit,
      changePercentage: (
        ((optimizedCat.limit - original.limit) / original.limit) *
        100
      ).toFixed(1),
    };
  });

  res.json({
    budgetId,
    optimizationDate: new Date().toISOString(),
    changes,
    newTotalLimit: optimized.reduce((sum, c) => sum + c.limit, 0),
  });
});

/**
 * POST /api/v1/budgets/check-alerts/:budgetId
 * Check budget alerts for a specific budget after expense addition
 */
router.post('/api/v1/budgets/check-alerts/:budgetId', (req: Request, res: Response) => {
  const { budgetId } = req.params;
  const { totalSpent, budgetLimit } = req.body;

  const utilizationPercentage = (totalSpent / budgetLimit) * 100;

  if (utilizationPercentage >= 80) {
    const severity = utilizationPercentage >= 100 ? 'critical' : 'warning';
    const message = `Vous avez utilisé ${utilizationPercentage.toFixed(1)}% de votre budget ⚠️`;

    // In real implementation, this would create an alert in database
    res.json({
      alertTriggered: true,
      severity,
      message,
      utilizationPercentage: utilizationPercentage.toFixed(1),
      recommendation: severity === 'critical' ?
        'Considérez réallouer le budget ou réduire les dépenses' :
        'Surveillez vos dépenses de près'
    });
  } else {
    res.json({
      alertTriggered: false,
      utilizationPercentage: utilizationPercentage.toFixed(1),
      status: 'normal'
    });
  }
});

// ============================================
// API 2: ALERTS AND REMINDERS ENDPOINTS
// ============================================

/**
 * GET /api/v1/alerts/:userId
 * Get all alerts for a user
 */
router.get('/api/v1/alerts/:userId', (req: Request, res: Response) => {
  const { userId } = req.params;
  // Mock alerts data - in real implementation, this would query the database
  const alerts = [
    {
      id: 1,
      type: 'budget_threshold',
      title: 'Seuil de budget dépassé',
      message: 'Vous avez utilisé 85% de votre budget Mars 2026 ⚠️',
      severity: 'warning',
      userId: parseInt(userId),
      budgetId: 1,
      expenseId: null,
      dateCreated: new Date().toISOString(),
      isRead: false,
      dueDate: null,
    },
    {
      id: 2,
      type: 'reminder',
      title: 'Rappel: Facture STEG',
      message: 'Facture STEG aujourd\'hui - Montant: 45.50 DT',
      severity: 'info',
      userId: parseInt(userId),
      budgetId: null,
      expenseId: null,
      dateCreated: new Date().toISOString(),
      isRead: false,
      dueDate: new Date().toISOString(),
    },
  ];

  res.json({ alerts, total: alerts.length, unread: alerts.filter(a => !a.isRead).length });
});

/**
 * POST /api/v1/alerts/:alertId/read
 * Mark alert as read
 */
router.post('/api/v1/alerts/:alertId/read', (req: Request, res: Response) => {
  const { alertId } = req.params;
  // In real implementation, update database
  res.json({ message: 'Alert marked as read', alertId });
});

/**
 * GET /api/v1/reminders/:userId
 * Get all reminders for a user
 */
router.get('/api/v1/reminders/:userId', (req: Request, res: Response) => {
  const { userId } = req.params;
  // Mock reminders data
  const reminders = [
    {
      id: 1,
      title: 'Facture STEG',
      description: 'Paiement mensuel électricité',
      dueDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(), // Tomorrow
      frequency: 'monthly',
      category: 'facture',
      amount: 45.50,
      userId: parseInt(userId),
      isActive: true,
      dateCreated: new Date().toISOString(),
      lastNotified: null,
    },
    {
      id: 2,
      title: 'Abonnement Netflix',
      description: 'Renouvellement mensuel',
      dueDate: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString(), // 5 days from now
      frequency: 'monthly',
      category: 'abonnement',
      amount: 15.99,
      userId: parseInt(userId),
      isActive: true,
      dateCreated: new Date().toISOString(),
      lastNotified: null,
    },
  ];

  res.json({ reminders });
});

/**
 * POST /api/v1/reminders
 * Create a new reminder
 */
router.post('/api/v1/reminders', (req: Request, res: Response) => {
  const { title, description, dueDate, frequency, category, amount, userId } = req.body;

  if (!title || !dueDate || !amount || !userId) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  const reminder = {
    id: Date.now(),
    title,
    description: description || '',
    dueDate,
    frequency: frequency || 'once',
    category: category || 'autre',
    amount,
    userId,
    isActive: true,
    dateCreated: new Date().toISOString(),
    lastNotified: null,
  };

  res.status(201).json(reminder);
});

/**
 * POST /api/v1/alerts/check/:userId
 * Run alert checks for a user (budget thresholds, due reminders)
 */
router.post('/api/v1/alerts/check/:userId', (req: Request, res: Response) => {
  const { userId } = req.params;

  // Simulate running alert checks
  const newAlerts = [
    {
      id: Date.now(),
      type: 'budget_threshold',
      title: 'Seuil de budget dépassé',
      message: 'Vous avez utilisé 85% de votre budget Mars 2026 ⚠️',
      severity: 'warning',
      userId: parseInt(userId),
      budgetId: 1,
      expenseId: null,
      dateCreated: new Date().toISOString(),
      isRead: false,
      dueDate: null,
    }
  ];

  res.json({
    message: 'Alert checks completed',
    newAlertsGenerated: newAlerts.length,
    alerts: newAlerts
  });
});

export default router;

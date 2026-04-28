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

export default router;

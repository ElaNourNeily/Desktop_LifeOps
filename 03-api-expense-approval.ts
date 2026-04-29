/**
 * API 2: Expense Management & Approval API
 * Smart expense submission, approval workflows, and real-time alerts
 */

import express, { Router, Request, Response } from 'express';
import {
  Expense,
  Budget,
  ExpenseApprovalEngine,
  ExpenseCategorizationEngine,
} from './01-budget-domain-model';

const router = Router();

// Mock database
const budgets: Map<string, Budget> = new Map();
const expenses: Map<string, Expense> = new Map();
const approvals: Map<string, ApprovalRecord> = new Map();

const approvalEngine = new ExpenseApprovalEngine();
const categorizationEngine = new ExpenseCategorizationEngine();

interface ApprovalRecord {
  expenseId: string;
  status: 'pending' | 'approved' | 'rejected';
  approvals: { approver: string; timestamp: Date; decision: string }[];
  riskScore: number;
  alerts: any[];
}

// ============================================
// API 2: EXPENSE MANAGEMENT ENDPOINTS
// ============================================

/**
 * POST /api/v1/expenses
 * Submit expense with intelligent risk assessment & approval routing
 * MÉTIER: Advanced approval workflow with ML-based risk scoring
 */
router.post('/api/v1/expenses', (req: Request, res: Response) => {
  const { budgetId, description, amount, categoryId, date, tags, receipt } = req.body;

  const budget = budgets.get(budgetId);
  if (!budget) {
    return res.status(404).json({ error: 'Budget not found' });
  }

  // Auto-categorize if categoryId not provided
  let finalCategoryId = categoryId;
  let categorizationResult = null;

  if (!categoryId) {
    categorizationResult = categorizationEngine.categorizeExpense(description);
    finalCategoryId = categorizationResult.categoryId;

    // Check if category exists in budget, if not, add it
    const existingCategory = budget.categories.find(c => c.categoryId === finalCategoryId);
    if (!existingCategory) {
      budget.categories.push({
        categoryId: finalCategoryId,
        name: categorizationResult.categoryName,
        limit: 0, // Default limit
        spent: 0,
        percentage: 0,
        priority: 'medium',
      });
    }
  }

  // Create expense
  const expense: Expense = {
    id: `exp_${Date.now()}`,
    description,
    amount,
    categoryId: finalCategoryId,
    budgetId,
    date: new Date(date),
    tags: tags || [],
    receipt,
    status: 'pending',
  };

  // Calculate risk score
  const riskScore = approvalEngine.calculateRiskScore(expense, budget);

  // Route for approval
  const approvalPath = approvalEngine.routeForApproval(expense, budget, riskScore);

  // Generate alert
  const alert = approvalEngine.generateAlert(expense, budget, riskScore);

  // Auto-approve if low risk and no required approvals
  if (approvalPath.requiredApprovals === 0) {
    expense.status = 'approved';
  }

  // Store expense and approval record
  expenses.set(expense.id, expense);
  approvals.set(expense.id, {
    expenseId: expense.id,
    status: expense.status as any,
    approvals: [],
    riskScore,
    alerts: [alert],
  });

  res.status(201).json({
    expense,
    approval: {
      approvalPath: approvalPath.approvalPath,
      requiredApprovals: approvalPath.requiredApprovals,
      reasoning: approvalPath.reasoning,
      riskScore,
    },
    alert,
    autoApproved: expense.status === 'approved',
    ...(categorizationResult && {
      autoCategorization: {
        categoryName: categorizationResult.categoryName,
        confidence: categorizationResult.confidence,
        matchedKeywords: categorizationResult.matchedKeywords,
      }
    }),
  });
});

/**
 * GET /api/v1/expenses/:expenseId/approval-status
 * Get approval status and history
 */
router.get(
  '/api/v1/expenses/:expenseId/approval-status',
  (req: Request, res: Response) => {
    const { expenseId } = req.params;
    const approval = approvals.get(expenseId);

    if (!approval) {
      return res.status(404).json({ error: 'Approval record not found' });
    }

    const expense = expenses.get(expenseId);

    res.json({
      expenseId,
      expense: {
        description: expense?.description,
        amount: expense?.amount,
        category: expense?.categoryId,
      },
      status: approval.status,
      riskScore: approval.riskScore,
      riskLevel:
        approval.riskScore > 60
          ? 'critical'
          : approval.riskScore > 40
            ? 'medium'
            : 'low',
      approvals: approval.approvals,
      alerts: approval.alerts,
    });
  }
);

/**
 * POST /api/v1/expenses/:expenseId/approve
 * Approve expense
 */
router.post(
  '/api/v1/expenses/:expenseId/approve',
  (req: Request, res: Response) => {
    const { expenseId } = req.params;
    const { approver, decision } = req.body;

    const approval = approvals.get(expenseId);
    if (!approval) {
      return res.status(404).json({ error: 'Approval record not found' });
    }

    approval.approvals.push({
      approver,
      timestamp: new Date(),
      decision: decision || 'approved',
    });

    const expense = expenses.get(expenseId)!;
    if (approval.approvals.length > 0) {
      expense.status = 'approved';
      approval.status = 'approved';
    }

    res.json({
      message: 'Expense approved',
      approvalCount: approval.approvals.length,
      expense,
      approvalHistory: approval.approvals,
    });
  }
);

/**
 * POST /api/v1/expenses/:expenseId/reject
 * Reject expense with reason
 */
router.post(
  '/api/v1/expenses/:expenseId/reject',
  (req: Request, res: Response) => {
    const { expenseId } = req.params;
    const { approver, reason } = req.body;

    const approval = approvals.get(expenseId);
    if (!approval) {
      return res.status(404).json({ error: 'Approval record not found' });
    }

    const expense = expenses.get(expenseId)!;
    expense.status = 'rejected';
    approval.status = 'rejected';

    approval.approvals.push({
      approver,
      timestamp: new Date(),
      decision: `rejected: ${reason}`,
    });

    res.json({
      message: 'Expense rejected',
      reason,
      approver,
      timestamp: new Date().toISOString(),
    });
  }
);

/**
 * GET /api/v1/expenses/pending-approvals
 * List all expenses pending approval (manager view)
 * MÉTIER: Smart prioritization based on risk & business rules
 */
router.get('/api/v1/expenses/pending-approvals', (req: Request, res: Response) => {
  const { priority } = req.query;

  const pendingApprovals = Array.from(approvals.values())
    .filter((a) => a.status === 'pending')
    .map((approval) => {
      const expense = expenses.get(approval.expenseId)!;
      return {
        expenseId: approval.expenseId,
        description: expense.description,
        amount: expense.amount,
        riskScore: approval.riskScore,
        riskLevel:
          approval.riskScore > 60
            ? 'critical'
            : approval.riskScore > 40
              ? 'medium'
              : 'low',
        alerts: approval.alerts,
        submittedDate: expense.date,
      };
    })
    .sort((a, b) => {
      // Sort by risk score descending (critical first)
      if (priority === 'risk') {
        return b.riskScore - a.riskScore;
      }
      // Sort by amount descending (largest first)
      if (priority === 'amount') {
        return b.amount - a.amount;
      }
      // Default: by submission date
      return (
        new Date(b.submittedDate).getTime() - new Date(a.submittedDate).getTime()
      );
    });

  res.json({
    totalPending: pendingApprovals.length,
    criticalCount: pendingApprovals.filter((a) => a.riskLevel === 'critical').length,
    mediumCount: pendingApprovals.filter((a) => a.riskLevel === 'medium').length,
    expenses: pendingApprovals,
  });
});

/**
 * POST /api/v1/expenses/bulk-review
 * Bulk review multiple expenses with compliance check
 * MÉTIER: Advanced batch processing with policy compliance
 */
router.post('/api/v1/expenses/bulk-review', (req: Request, res: Response) => {
  const { expenseIds, action, approver } = req.body;

  if (!expenseIds || !Array.isArray(expenseIds)) {
    return res.status(400).json({ error: 'expenseIds must be an array' });
  }

  const results = expenseIds.map((expenseId: string) => {
    const approval = approvals.get(expenseId);
    if (!approval) {
      return { expenseId, status: 'not_found' };
    }

    const expense = expenses.get(expenseId)!;

    if (action === 'approve') {
      approval.approvals.push({
        approver,
        timestamp: new Date(),
        decision: 'bulk_approved',
      });
      expense.status = 'approved';
      approval.status = 'approved';
      return { expenseId, status: 'approved', amount: expense.amount };
    }

    if (action === 'reject') {
      approval.approvals.push({
        approver,
        timestamp: new Date(),
        decision: 'bulk_rejected',
      });
      expense.status = 'rejected';
      approval.status = 'rejected';
      return { expenseId, status: 'rejected', amount: expense.amount };
    }

    return { expenseId, status: 'invalid_action' };
  });

  const approved = results.filter((r) => r.status === 'approved');
  const rejected = results.filter((r) => r.status === 'rejected');
  const totalAmount = [
    ...approved,
    ...rejected,
  ].reduce((sum, r) => sum + (r.amount || 0), 0);

  res.json({
    processedCount: results.length,
    approvedCount: approved.length,
    rejectedCount: rejected.length,
    totalAmount,
    results,
  });
});

/**
 * GET /api/v1/expenses/statistics
 * Get spending statistics and trends
 */
router.get('/api/v1/expenses/statistics', (req: Request, res: Response) => {
  const { budgetId, timeframe } = req.query;

  let filterExpenses = Array.from(expenses.values());

  if (budgetId) {
    filterExpenses = filterExpenses.filter((e) => e.budgetId === budgetId);
  }

  if (timeframe === 'week') {
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    filterExpenses = filterExpenses.filter((e) => e.date >= weekAgo);
  }

  if (timeframe === 'month') {
    const monthAgo = new Date();
    monthAgo.setMonth(monthAgo.getMonth() - 1);
    filterExpenses = filterExpenses.filter((e) => e.date >= monthAgo);
  }

  const totalExpenses = filterExpenses.length;
  const totalAmount = filterExpenses.reduce((sum, e) => sum + e.amount, 0);
  const averageAmount = totalAmount / (totalExpenses || 1);

  const byStatus = {
    approved: filterExpenses.filter((e) => e.status === 'approved').length,
    pending: filterExpenses.filter((e) => e.status === 'pending').length,
    rejected: filterExpenses.filter((e) => e.status === 'rejected').length,
  };

  const byCategory: Record<string, { count: number; amount: number }> = {};
  filterExpenses.forEach((e) => {
    if (!byCategory[e.categoryId]) {
      byCategory[e.categoryId] = { count: 0, amount: 0 };
    }
    byCategory[e.categoryId].count++;
    byCategory[e.categoryId].amount += e.amount;
  });

  res.json({
    timeframe: timeframe || 'all',
    totalExpenses,
    totalAmount: totalAmount.toFixed(2),
    averageAmount: averageAmount.toFixed(2),
    byStatus,
    byCategory,
  });
});

/**
 * POST /api/v1/categorization/rules
 * Add custom categorization rule
 */
router.post('/api/v1/categorization/rules', (req: Request, res: Response) => {
  const { categoryName, keywords } = req.body;

  if (!categoryName || !Array.isArray(keywords)) {
    return res.status(400).json({ error: 'categoryName and keywords array required' });
  }

  categorizationEngine.addCategoryRule(categoryName, keywords);
  res.json({ message: `Added rule for category: ${categoryName}`, keywords });
});

/**
 * GET /api/v1/categorization/categories
 * Get all available categories
 */
router.get('/api/v1/categorization/categories', (req: Request, res: Response) => {
  const categories = categorizationEngine.getAvailableCategories();
  res.json({ categories });
});

/**
 * POST /api/v1/categorization/preview
 * Preview categorization for a description
 */
router.post('/api/v1/categorization/preview', (req: Request, res: Response) => {
  const { description } = req.body;

  if (!description) {
    return res.status(400).json({ error: 'description required' });
  }

  const result = categorizationEngine.categorizeExpense(description);
  res.json(result);
});

export default router;

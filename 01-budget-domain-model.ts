/**
 * ADVANCED DOMAIN MODEL - Budget & Expense Management
 * Métier Avancée: Business logic layer for budget constraints and expense categorization
 */

// ============================================
// DOMAIN ENTITIES
// ============================================

interface Budget {
  id: string;
  name: string;
  totalLimit: number;
  currency: string;
  categories: BudgetCategory[];
  period: { startDate: Date; endDate: Date };
  alerts: AlertRule[];
  createdAt: Date;
  updatedAt: Date;
}

interface BudgetCategory {
  categoryId: string;
  name: string;
  limit: number;
  spent: number;
  percentage: number; // % of total budget
  priority: 'critical' | 'high' | 'medium' | 'low';
}

interface Expense {
  id: string;
  description: string;
  amount: number;
  categoryId: string;
  budgetId: string;
  date: Date;
  tags: string[];
  receipt?: string;
  recurring?: RecurringConfig;
  status: 'pending' | 'approved' | 'rejected';
}

interface RecurringConfig {
  frequency: 'daily' | 'weekly' | 'monthly' | 'yearly';
  endDate?: Date;
  nextOccurrence: Date;
}

interface AlertRule {
  id: string;
  threshold: number; // percentage
  type: 'threshold' | 'unusual' | 'forecast';
  recipients: string[];
  enabled: boolean;
}

// ============================================
// ADVANCED BUSINESS LOGIC - MÉTIER 1
// ============================================

/**
 * MÉTIER AVANCÉE 1: Budget Constraint Analysis & Forecasting
 * Analyzes spending patterns, predicts overruns, and optimizes budget allocation
 */

class BudgetForecastingEngine {
  // Predict spending based on historical patterns using exponential smoothing
  predictMonthlySpending(
    categoryId: string,
    historicalExpenses: Expense[],
    alpha: number = 0.3
  ): {
    predictedAmount: number;
    confidence: number;
    trend: 'increasing' | 'decreasing' | 'stable';
  } {
    if (historicalExpenses.length === 0) {
      return { predictedAmount: 0, confidence: 0, trend: 'stable' };
    }

    const sorted = historicalExpenses.sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );
    let forecast = sorted[0].amount;

    for (let i = 1; i < sorted.length; i++) {
      forecast = alpha * sorted[i].amount + (1 - alpha) * forecast;
    }

    // Calculate trend using linear regression
    const n = sorted.length;
    const xMean = (n + 1) / 2;
    const yMean = sorted.reduce((sum, e) => sum + e.amount, 0) / n;

    let numerator = 0;
    let denominator = 0;

    sorted.forEach((expense, i) => {
      numerator += (i + 1 - xMean) * (expense.amount - yMean);
      denominator += Math.pow(i + 1 - xMean, 2);
    });

    const slope = denominator === 0 ? 0 : numerator / denominator;
    const trend = slope > 5 ? 'increasing' : slope < -5 ? 'decreasing' : 'stable';

    // Confidence based on variance
    const variance =
      sorted.reduce((sum, e) => sum + Math.pow(e.amount - yMean, 2), 0) / n;
    const confidence = Math.max(0, 1 - Math.sqrt(variance) / forecast);

    return {
      predictedAmount: Math.round(forecast * 100) / 100,
      confidence: Math.round(confidence * 100) / 100,
      trend,
    };
  }

  // Detect unusual spending patterns
  detectAnomalies(
    expenses: Expense[],
    sensitivityFactor: number = 2
  ): { expenseId: string; anomalyScore: number; reason: string }[] {
    if (expenses.length < 3) return [];

    const anomalies: { expenseId: string; anomalyScore: number; reason: string }[] =
      [];
    const amounts = expenses.map((e) => e.amount);
    const mean = amounts.reduce((a, b) => a + b, 0) / amounts.length;
    const stdDev = Math.sqrt(
      amounts.reduce((sum, a) => sum + Math.pow(a - mean, 2), 0) / amounts.length
    );

    expenses.forEach((expense) => {
      const zScore = Math.abs((expense.amount - mean) / (stdDev || 1));

      if (zScore > sensitivityFactor) {
        anomalies.push({
          expenseId: expense.id,
          anomalyScore: zScore,
          reason: `Amount (${expense.amount}) deviates ${zScore.toFixed(1)}σ from mean (${mean.toFixed(2)})`,
        });
      }
    });

    return anomalies.sort((a, b) => b.anomalyScore - a.anomalyScore);
  }

  // Optimize budget allocation across categories
  optimizeBudgetAllocation(
    budget: Budget,
    historicalExpenses: Expense[]
  ): BudgetCategory[] {
    const categoryExpenses: Map<string, Expense[]> = new Map();

    historicalExpenses.forEach((exp) => {
      if (!categoryExpenses.has(exp.categoryId)) {
        categoryExpenses.set(exp.categoryId, []);
      }
      categoryExpenses.get(exp.categoryId)!.push(exp);
    });

    return budget.categories.map((category) => {
      const categoryExpnses = categoryExpenses.get(category.categoryId) || [];
      const avgSpending =
        categoryExpnses.reduce((sum, e) => sum + e.amount, 0) /
        (categoryExpnses.length || 1);

      // Allocate based on actual usage and priority
      const priorityMultiplier = {
        critical: 1.5,
        high: 1.2,
        medium: 1.0,
        low: 0.8,
      }[category.priority];

      const recommendedLimit = Math.ceil(avgSpending * 1.2 * priorityMultiplier);

      return {
        ...category,
        limit: recommendedLimit,
        percentage: (recommendedLimit / budget.totalLimit) * 100,
      };
    });
  }
}

// ============================================
// ADVANCED BUSINESS LOGIC - MÉTIER 2
// ============================================

/**
 * MÉTIER AVANCÉE 2: Smart Alert & Approval Workflow
 * Intelligent expense validation, approval routing, and real-time alerts
 */

class ExpenseApprovalEngine {
  private riskScores: Map<string, number> = new Map();

  // Calculate risk score for expense (fraud/policy detection)
  calculateRiskScore(expense: Expense, budget: Budget): number {
    let risk = 0;

    const category = budget.categories.find(
      (c) => c.categoryId === expense.categoryId
    );
    if (!category) return 100; // Unknown category = high risk

    // Risk 1: Exceeds category limit
    const categoryUtilization = (category.spent + expense.amount) / category.limit;
    if (categoryUtilization > 1.5) risk += 35;
    else if (categoryUtilization > 1.0) risk += 15;

    // Risk 2: Unusual amount
    const avgCategoryAmount = category.spent / 10; // Assume ~10 expenses
    const amountRatio = expense.amount / (avgCategoryAmount || 1);
    if (amountRatio > 3) risk += 25;

    // Risk 3: Missing receipt/documentation
    if (!expense.receipt) risk += 20;

    // Risk 4: Suspicious timing
    const dayOfWeek = expense.date.getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) risk += 10; // Weekend spending

    // Risk 5: High-frequency transactions (potential fraud)
    const riskKey = `${expense.categoryId}:${expense.date.toDateString()}`;
    const dailyCount = this.riskScores.get(riskKey) || 0;
    if (dailyCount > 3) risk += 20;

    this.riskScores.set(riskKey, dailyCount + 1);

    return Math.min(risk, 100);
  }

  // Determine approval workflow based on risk & budget rules
  routeForApproval(
    expense: Expense,
    budget: Budget,
    riskScore: number
  ): {
    approvalPath: 'auto' | 'manager' | 'director' | 'board';
    reasoning: string[];
    requiredApprovals: number;
  } {
    const reasoning: string[] = [];
    let approvalPath: 'auto' | 'manager' | 'director' | 'board' = 'auto';
    let requiredApprovals = 0;

    const amountRatio = (expense.amount / budget.totalLimit) * 100;

    if (riskScore > 60) {
      approvalPath = 'director';
      requiredApprovals = 2;
      reasoning.push(`High risk score: ${riskScore}`);
    } else if (riskScore > 40) {
      approvalPath = 'manager';
      requiredApprovals = 1;
      reasoning.push(`Medium risk score: ${riskScore}`);
    } else if (amountRatio > 25) {
      approvalPath = 'manager';
      requiredApprovals = 1;
      reasoning.push(`Large amount: ${amountRatio.toFixed(1)}% of budget`);
    } else if (amountRatio > 50) {
      approvalPath = 'board';
      requiredApprovals = 3;
      reasoning.push(`Very large amount: ${amountRatio.toFixed(1)}% of budget`);
    } else {
      requiredApprovals = 0;
      reasoning.push('Low risk, auto-approved');
    }

    return { approvalPath, reasoning, requiredApprovals };
  }

  // Generate intelligent alert for stakeholders
  generateAlert(
    expense: Expense,
    budget: Budget,
    riskScore: number
  ): {
    alertLevel: 'info' | 'warning' | 'critical';
    title: string;
    message: string;
    suggestedAction: string;
  } {
    const category = budget.categories.find(
      (c) => c.categoryId === expense.categoryId
    )!;
    const categoryUtilization = ((category.spent + expense.amount) / category.limit) * 100;

    if (categoryUtilization > 150) {
      return {
        alertLevel: 'critical',
        title: 'Category Budget Critical Overspend',
        message: `${category.name} will exceed limit by ${(categoryUtilization - 100).toFixed(0)}%`,
        suggestedAction: 'Reject expense or reallocate budget',
      };
    }

    if (riskScore > 60) {
      return {
        alertLevel: 'critical',
        title: 'High Risk Expense Detected',
        message: `Risk score: ${riskScore}. Amount: ${expense.amount}, Missing receipt: ${!expense.receipt}`,
        suggestedAction: 'Request additional documentation before approval',
      };
    }

    if (categoryUtilization > 100) {
      return {
        alertLevel: 'warning',
        title: 'Category Budget Exceeded',
        message: `${category.name} will exceed limit by ${(categoryUtilization - 100).toFixed(0)}%`,
        suggestedAction: 'Consider adjustment or defer expense',
      };
    }

    return {
      alertLevel: 'info',
      title: 'Expense Logged',
      message: `${expense.description} - ${expense.amount}`,
      suggestedAction: 'No action needed',
    };
  }
}

export {
  Budget,
  BudgetCategory,
  Expense,
  AlertRule,
  RecurringConfig,
  BudgetForecastingEngine,
  ExpenseApprovalEngine,
};

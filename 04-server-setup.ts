/**
 * COMPLETE SERVER SETUP & INTEGRATION EXAMPLE
 * How to wire everything together with advanced middleware & services
 */

import express, { Express, Request, Response, NextFunction } from 'express';
import budgetRouter from './02-api-budget';
import expenseRouter from './03-api-expense-approval';
import { BudgetForecastingEngine, ExpenseApprovalEngine, ExpenseCategorizationEngine } from './01-budget-domain-model';

// ============================================
// MIDDLEWARE & CONFIGURATION
// ============================================

class APIServer {
  app: Express;
  forecastEngine: BudgetForecastingEngine;
  approvalEngine: ExpenseApprovalEngine;
  categorizationEngine: ExpenseCategorizationEngine;

  constructor() {
    this.app = express();
    this.forecastEngine = new BudgetForecastingEngine();
    this.approvalEngine = new ExpenseApprovalEngine();
    this.categorizationEngine = new ExpenseCategorizationEngine();

    this.setupMiddleware();
    this.setupRoutes();
    this.setupErrorHandling();
  }

  private setupMiddleware() {
    // JSON parsing
    this.app.use(express.json());

    // Request logging
    this.app.use((req: Request, res: Response, next: NextFunction) => {
      console.log(`${req.method} ${req.path} - ${new Date().toISOString()}`);
      next();
    });

    // CORS
    this.app.use((req: Request, res: Response, next: NextFunction) => {
      res.header('Access-Control-Allow-Origin', '*');
      res.header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE');
      res.header('Access-Control-Allow-Headers', 'Content-Type');
      next();
    });

    // Authentication mock
    this.app.use((req: Request, res: Response, next: NextFunction) => {
      // In production, verify JWT token
      req.user = { id: 'user_123', role: 'manager' };
      next();
    });
  }

  private setupRoutes() {
    // Health check
    this.app.get('/health', (req: Request, res: Response) => {
      res.json({ status: 'ok', timestamp: new Date().toISOString() });
    });

    // API routes
    this.app.use(budgetRouter);
    this.app.use(expenseRouter);

    // Advanced analytics endpoint
    this.app.get('/api/v1/analytics/dashboard', (req: Request, res: Response) => {
      res.json({
        title: 'Budget Dashboard',
        metrics: {
          totalBudgets: 5,
          totalExpenses: 142,
          approvalPending: 8,
          anomaliesDetected: 3,
        },
        lastUpdated: new Date().toISOString(),
      });
    });
  }

  private setupErrorHandling() {
    // 404 handler
    this.app.use((req: Request, res: Response) => {
      res.status(404).json({ error: 'Not found', path: req.path });
    });

    // Error handler
    this.app.use(
      (err: Error, req: Request, res: Response, next: NextFunction) => {
        console.error(err);
        res.status(500).json({
          error: 'Internal server error',
          message: err.message,
        });
      }
    );
  }

  start(port: number = 3000) {
    this.app.listen(port, () => {
      console.log(`🚀 Server running on http://localhost:${port}`);
      console.log('📊 Available endpoints:');
      console.log('  - GET  /health');
      console.log('  - GET  /api/v1/budgets/:budgetId');
      console.log('  - POST /api/v1/budgets');
      console.log('  - GET  /api/v1/budgets/:budgetId/forecast');
      console.log('  - GET  /api/v1/budgets/:budgetId/anomalies');
      console.log('  - POST /api/v1/budgets/:budgetId/optimize');
      console.log('  - POST /api/v1/budgets/check-alerts/:budgetId');
      console.log('  - POST /api/v1/expenses');
      console.log('  - GET  /api/v1/expenses/:expenseId/approval-status');
      console.log('  - POST /api/v1/expenses/:expenseId/approve');
      console.log('  - POST /api/v1/expenses/:expenseId/reject');
      console.log('  - GET  /api/v1/expenses/pending-approvals');
      console.log('  - POST /api/v1/expenses/bulk-review');
      console.log('  - GET  /api/v1/expenses/statistics');
      console.log('  - POST /api/v1/categorization/rules');
      console.log('  - GET  /api/v1/categorization/categories');
      console.log('  - POST /api/v1/categorization/preview');
      console.log('  - GET  /api/v1/alerts/:userId');
      console.log('  - POST /api/v1/alerts/:alertId/read');
      console.log('  - GET  /api/v1/reminders/:userId');
      console.log('  - POST /api/v1/reminders');
      console.log('  - POST /api/v1/alerts/check/:userId');
    });
  }
}

// ============================================
// EXAMPLE USAGE
// ============================================

const server = new APIServer();
server.start(3000);

export default server;
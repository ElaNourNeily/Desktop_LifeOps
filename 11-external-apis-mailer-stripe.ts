/**
 * EXTERNAL API INTEGRATION
 * Mailer + Stripe with Budget/Expense Métier
 */

import express, { Router, Request, Response } from 'express';
import nodemailer from 'nodemailer';
import Stripe from 'stripe';

// ============================================
// CONFIGURATION
// ============================================

// Nodemailer Config (Email)
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER || 'your-email@gmail.com',
    pass: process.env.EMAIL_PASSWORD || 'your-app-password',
  },
});

// Stripe Config (Payment)
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || 'sk_test_dummy', {
  apiVersion: '2023-10-16' as any,
});

// ============================================
// API 1: MAILER - BUDGET ALERTS & NOTIFICATIONS
// ============================================

interface AlertNotification {
  recipientEmail: string;
  recipientName: string;
  subject: string;
  alertType: 'budget_warning' | 'budget_critical' | 'approval_needed' | 'expense_approved';
  data: any;
}

class BudgetMailerService {
  /**
   * Send budget warning email when approaching limit
   */
  async sendBudgetWarning(
    email: string,
    categoryName: string,
    spent: number,
    limit: number,
    percentage: number
  ): Promise<{ success: boolean; messageId?: string; error?: string }> {
    const message = `
      <h2>⚠️ Budget Warning</h2>
      <p>Your <strong>${categoryName}</strong> category has reached <strong>${percentage}%</strong> of its limit.</p>
      <ul>
        <li>Spent: $${spent}</li>
        <li>Limit: $${limit}</li>
        <li>Remaining: $${limit - spent}</li>
      </ul>
      <p>Please review your spending to avoid overage.</p>
      <hr/>
      <p>This is an automated notification from Budget Manager</p>
    `;

    try {
      const info = await transporter.sendMail({
        from: process.env.EMAIL_USER || 'budget@example.com',
        to: email,
        subject: `Budget Alert: ${categoryName} at ${percentage}%`,
        html: message,
      });

      console.log(`✅ Budget warning sent: ${info.messageId}`);
      return { success: true, messageId: info.messageId };
    } catch (error: any) {
      console.error(`❌ Email error: ${error.message}`);
      return { success: false, error: error.message };
    }
  }

  /**
   * Send critical budget overrun alert
   */
  async sendBudgetCritical(
    email: string,
    categoryName: string,
    overageAmount: number,
    overagePercentage: number
  ): Promise<{ success: boolean; messageId?: string; error?: string }> {
    const message = `
      <h2>🚨 CRITICAL BUDGET ALERT</h2>
      <p>Your <strong>${categoryName}</strong> category has <strong>EXCEEDED</strong> its budget limit!</p>
      <ul>
        <li>Overage: $${overageAmount}</li>
        <li>Over limit by: ${overagePercentage}%</li>
      </ul>
      <p><strong>Immediate action required!</strong></p>
      <p>Please reduce spending immediately or request budget reallocation.</p>
      <hr/>
      <p>Contact your finance manager for assistance.</p>
    `;

    try {
      const info = await transporter.sendMail({
        from: process.env.EMAIL_USER || 'budget@example.com',
        to: email,
        subject: `🚨 CRITICAL: Budget Exceeded - ${categoryName}`,
        html: message,
      });

      return { success: true, messageId: info.messageId };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }

  /**
   * Send expense approval request
   */
  async sendApprovalRequest(
    managerEmail: string,
    managerName: string,
    expenseDescription: string,
    expenseAmount: number,
    submitterName: string,
    riskScore: number,
    approvalLink: string
  ): Promise<{ success: boolean; messageId?: string; error?: string }> {
    const riskLevel = riskScore > 60 ? '🔴 HIGH' : riskScore > 40 ? '🟡 MEDIUM' : '🟢 LOW';

    const message = `
      <h2>Expense Approval Required</h2>
      <p>Hi ${managerName},</p>
      <p><strong>${submitterName}</strong> has submitted an expense for approval:</p>
      <table style="border-collapse: collapse; width: 100%;">
        <tr style="background: #f5f5f5;">
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Description</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${expenseDescription}</td>
        </tr>
        <tr>
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Amount</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">$${expenseAmount}</td>
        </tr>
        <tr style="background: #f5f5f5;">
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Risk Level</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${riskLevel} (${riskScore}/100)</td>
        </tr>
      </table>
      <p style="margin-top: 20px;">
        <a href="${approvalLink}" style="background: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
          Review & Approve
        </a>
      </p>
      <hr/>
      <p style="font-size: 12px; color: #666;">Automatic notification - do not reply to this email</p>
    `;

    try {
      const info = await transporter.sendMail({
        from: process.env.EMAIL_USER || 'budget@example.com',
        to: managerEmail,
        subject: `Expense Approval Needed: $${expenseAmount} (${riskLevel})`,
        html: message,
      });

      return { success: true, messageId: info.messageId };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }

  /**
   * Send expense confirmation
   */
  async sendExpenseConfirmation(
    submitterEmail: string,
    submitterName: string,
    expenseId: string,
    description: string,
    amount: number,
    category: string,
    status: string
  ): Promise<{ success: boolean; messageId?: string; error?: string }> {
    const statusColor = status === 'approved' ? '#28a745' : status === 'rejected' ? '#dc3545' : '#ffc107';
    const statusEmoji = status === 'approved' ? '✅' : status === 'rejected' ? '❌' : '⏳';

    const message = `
      <h2>Expense Confirmation</h2>
      <p>Hi ${submitterName},</p>
      <p>Your expense has been recorded in the system:</p>
      <table style="border-collapse: collapse; width: 100%;">
        <tr style="background: #f5f5f5;">
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Expense ID</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${expenseId}</td>
        </tr>
        <tr>
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Description</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${description}</td>
        </tr>
        <tr style="background: #f5f5f5;">
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Amount</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">$${amount}</td>
        </tr>
        <tr>
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Category</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${category}</td>
        </tr>
        <tr style="background: ${statusColor}; color: white;">
          <td style="padding: 10px; border: 1px solid #ddd;"><strong>Status</strong></td>
          <td style="padding: 10px; border: 1px solid #ddd;">${statusEmoji} ${status.toUpperCase()}</td>
        </tr>
      </table>
      <p style="margin-top: 20px;">Thank you for your submission!</p>
    `;

    try {
      const info = await transporter.sendMail({
        from: process.env.EMAIL_USER || 'budget@example.com',
        to: submitterEmail,
        subject: `${statusEmoji} Expense Confirmed: ${expenseId}`,
        html: message,
      });

      return { success: true, messageId: info.messageId };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }
}

// ============================================
// API 2: STRIPE - PAYMENT TRACKING & EXPENSE RECORDING
// ============================================

interface StripeCharge {
  amount: number;
  description: string;
  metadata: {
    budgetId: string;
    categoryId: string;
    submitterEmail: string;
  };
}

class StripeExpenseService {
  /**
   * Create charge when expense is approved
   * Integrates with payment processing
   */
  async createChargeForExpense(
    budgetId: string,
    categoryId: string,
    expenseDescription: string,
    amountCents: number,
    token: string,
    email: string
  ): Promise<{
    success: boolean;
    chargeId?: string;
    amount?: number;
    status?: string;
    error?: string;
  }> {
    try {
      const charge = await stripe.charges.create({
        amount: amountCents, // Stripe uses cents
        currency: 'usd',
        source: token,
        description: expenseDescription,
        receipt_email: email,
        metadata: {
          budgetId,
          categoryId,
          submitterEmail: email,
        },
      });

      console.log(`✅ Stripe charge created: ${charge.id}`);
      return {
        success: true,
        chargeId: charge.id,
        amount: charge.amount / 100,
        status: charge.status,
      };
    } catch (error: any) {
      console.error(`❌ Stripe error: ${error.message}`);
      return { success: false, error: error.message };
    }
  }

  /**
   * Retrieve transaction history for reconciliation
   */
  async getTransactionHistory(limit: number = 10): Promise<{
    success: boolean;
    charges?: any[];
    error?: string;
  }> {
    try {
      const charges = await stripe.charges.list({
        limit,
      });

      return {
        success: true,
        charges: charges.data.map((charge) => ({
          id: charge.id,
          amount: charge.amount / 100,
          currency: charge.currency,
          status: charge.status,
          description: charge.description,
          email: charge.receipt_email,
          date: new Date(charge.created * 1000),
          metadata: charge.metadata,
        })),
      };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }

  /**
   * Refund a charge if expense is rejected
   */
  async refundCharge(chargeId: string): Promise<{
    success: boolean;
    refundId?: string;
    error?: string;
  }> {
    try {
      const refund = await stripe.refunds.create({
        charge: chargeId,
      });

      console.log(`✅ Stripe refund created: ${refund.id}`);
      return { success: true, refundId: refund.id };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }

  /**
   * Get payment statistics
   */
  async getPaymentStats(): Promise<{
    success: boolean;
    totalCharges?: number;
    totalAmount?: number;
    successfulCharges?: number;
    failedCharges?: number;
    error?: string;
  }> {
    try {
      const charges = await stripe.charges.list({ limit: 100 });

      const totalCharges = charges.data.length;
      const totalAmount = charges.data.reduce((sum, c) => sum + c.amount, 0) / 100;
      const successful = charges.data.filter((c) => c.status === 'succeeded').length;
      const failed = charges.data.filter((c) => c.status === 'failed').length;

      return {
        success: true,
        totalCharges,
        totalAmount,
        successfulCharges: successful,
        failedCharges: failed,
      };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  }
}

// ============================================
// COMBINED ROUTER - MAILER + STRIPE ENDPOINTS
// ============================================

const externalApisRouter = Router();
const mailerService = new BudgetMailerService();
const stripeService = new StripeExpenseService();

/**
 * POST /api/v1/notifications/budget-warning
 * Send budget warning email
 */
externalApisRouter.post('/api/v1/notifications/budget-warning', async (req: Request, res: Response) => {
  try {
    const { email, categoryName, spent, limit } = req.body;
    const percentage = (spent / limit) * 100;

    const result = await mailerService.sendBudgetWarning(
      email,
      categoryName,
      spent,
      limit,
      Math.round(percentage)
    );

    res.json({
      message: 'Budget warning email sent',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/v1/notifications/budget-critical
 * Send critical budget alert
 */
externalApisRouter.post('/api/v1/notifications/budget-critical', async (req: Request, res: Response) => {
  try {
    const { email, categoryName, overageAmount, limit } = req.body;
    const overagePercentage = ((overageAmount / limit) * 100).toFixed(1);

    const result = await mailerService.sendBudgetCritical(
      email,
      categoryName,
      overageAmount,
      Number(overagePercentage)
    );

    res.json({
      message: 'Critical budget alert sent',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/v1/notifications/approval-request
 * Send approval request email
 */
externalApisRouter.post('/api/v1/notifications/approval-request', async (req: Request, res: Response) => {
  try {
    const {
      managerEmail,
      managerName,
      expenseDescription,
      expenseAmount,
      submitterName,
      riskScore,
      approvalLink,
    } = req.body;

    const result = await mailerService.sendApprovalRequest(
      managerEmail,
      managerName,
      expenseDescription,
      expenseAmount,
      submitterName,
      riskScore,
      approvalLink
    );

    res.json({
      message: 'Approval request email sent',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/v1/notifications/expense-confirmation
 * Send expense confirmation email
 */
externalApisRouter.post('/api/v1/notifications/expense-confirmation', async (req: Request, res: Response) => {
  try {
    const { submitterEmail, submitterName, expenseId, description, amount, category, status } =
      req.body;

    const result = await mailerService.sendExpenseConfirmation(
      submitterEmail,
      submitterName,
      expenseId,
      description,
      amount,
      category,
      status
    );

    res.json({
      message: 'Expense confirmation email sent',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/v1/payments/charge
 * Create Stripe charge for approved expense
 */
externalApisRouter.post('/api/v1/payments/charge', async (req: Request, res: Response) => {
  try {
    const { budgetId, categoryId, description, amountCents, token, email } = req.body;

    const result = await stripeService.createChargeForExpense(
      budgetId,
      categoryId,
      description,
      amountCents,
      token,
      email
    );

    res.json({
      message: 'Payment processed',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/v1/payments/transactions
 * Get transaction history
 */
externalApisRouter.get('/api/v1/payments/transactions', async (req: Request, res: Response) => {
  try {
    const limit = parseInt(req.query.limit as string) || 10;
    const result = await stripeService.getTransactionHistory(limit);

    res.json({
      message: 'Transaction history retrieved',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/v1/payments/refund
 * Refund a charge
 */
externalApisRouter.post('/api/v1/payments/refund', async (req: Request, res: Response) => {
  try {
    const { chargeId } = req.body;
    const result = await stripeService.refundCharge(chargeId);

    res.json({
      message: 'Refund processed',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/v1/payments/stats
 * Get payment statistics
 */
externalApisRouter.get('/api/v1/payments/stats', async (req: Request, res: Response) => {
  try {
    const result = await stripeService.getPaymentStats();

    res.json({
      message: 'Payment statistics',
      ...result,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export {
  externalApisRouter,
  BudgetMailerService,
  StripeExpenseService,
};

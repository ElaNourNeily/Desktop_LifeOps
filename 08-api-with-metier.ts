/**
 * API INTEGRATION WITH MÉTIER
 * How to wire business logic into REST API endpoints
 */

import express, { Router, Request, Response } from 'express';
import {
  DynamicPricingEngine,
  RouteOptimizationEngine,
  InventoryOptimizationEngine,
  CompensationEngine,
  PropertyValuationEngine,
} from './06-metier-examples-advanced';

// ============================================
// API 1: E-COMMERCE PRICING API
// ============================================

const pricingRouter = Router();
const pricingEngine = new DynamicPricingEngine();

// Mock database
interface Product {
  id: string;
  name: string;
  basePrice: number;
  cost: number;
  inventory: number;
  demandScore: number;
  competitorPrice?: number;
  seasonalMultiplier: number;
}

const products: Map<string, Product> = new Map();
const salesHistory: Map<string, { price: number; quantity: number }[]> = new Map();

/**
 * POST /api/v1/pricing/calculate
 * Calculate optimal price for a product using AI
 */
pricingRouter.post('/api/v1/pricing/calculate', (req: Request, res: Response) => {
  const { productId } = req.body;
  const product = products.get(productId);

  if (!product) {
    return res.status(404).json({ error: 'Product not found' });
  }

  const pricing = pricingEngine.calculateOptimalPrice(product, new Date());

  // Get price elasticity history
  const history = salesHistory.get(productId) || [];
  const elasticity = pricingEngine.calculatePriceElasticity(history);

  // Get recommendation
  const recommendation = pricingEngine.recommendPriceAdjustment(
    product.basePrice,
    elasticity,
    0.3 // 30% target margin
  );

  res.json({
    productId,
    product: {
      name: product.name,
      basePrice: product.basePrice,
      currentCost: product.cost,
      inventory: product.inventory,
    },
    pricing: {
      recommendedPrice: pricing.finalPrice,
      margin: pricing.margin,
      marginPercentage: pricing.marginPercentage,
      factors: {
        demand: `${((pricing.factors.demandFactor - 1) * 100).toFixed(0)}%`,
        inventory: `${((pricing.factors.inventoryFactor - 1) * 100).toFixed(0)}%`,
        competitor: `${((pricing.factors.competitorFactor - 1) * 100).toFixed(0)}%`,
        seasonal: `${((pricing.factors.seasonalFactor - 1) * 100).toFixed(0)}%`,
      },
    },
    elasticity: {
      score: elasticity.toFixed(2),
      type:
        elasticity > -0.5
          ? 'Inelastic (price increase OK)'
          : elasticity < -2
            ? 'Elastic (lower price = volume)'
            : 'Unit elastic',
    },
    recommendation,
    timestamp: new Date().toISOString(),
  });
});

/**
 * GET /api/v1/pricing/products/:productId/elasticity
 * Analyze price sensitivity for a product
 */
pricingRouter.get('/api/v1/pricing/products/:productId/elasticity', (req: Request, res: Response) => {
  const { productId } = req.params;
  const product = products.get(productId);

  if (!product) {
    return res.status(404).json({ error: 'Product not found' });
  }

  const history = salesHistory.get(productId) || [];
  const elasticity = pricingEngine.calculatePriceElasticity(history);

  res.json({
    productId,
    productName: product.name,
    priceHistory: history,
    elasticity: elasticity.toFixed(2),
    interpretation:
      elasticity > -0.5
        ? 'INELASTIC: Customers not price sensitive. Increase price for more margin.'
        : elasticity < -2
          ? 'ELASTIC: Customers very price sensitive. Lower price increases volume.'
          : 'UNIT ELASTIC: Price and quantity balanced. Maintain current strategy.',
    recommendation:
      elasticity > -0.5
        ? { action: 'Increase price by 10%', expectedImpact: 'Quantity down 5%, Revenue up 5%' }
        : elasticity < -2
          ? { action: 'Decrease price by 5%', expectedImpact: 'Quantity up 8%, Revenue up 2%' }
          : { action: 'Hold price', expectedImpact: 'Maintain current revenue' },
  });
});

/**
 * POST /api/v1/pricing/products
 * Create/register product for pricing optimization
 */
pricingRouter.post('/api/v1/pricing/products', (req: Request, res: Response) => {
  const {
    name,
    basePrice,
    cost,
    inventory,
    demandScore,
    competitorPrice,
    seasonalMultiplier = 1.0,
  } = req.body;

  const product: Product = {
    id: `prod_${Date.now()}`,
    name,
    basePrice,
    cost,
    inventory,
    demandScore,
    competitorPrice,
    seasonalMultiplier,
  };

  products.set(product.id, product);
  salesHistory.set(product.id, []);

  const pricing = pricingEngine.calculateOptimalPrice(product, new Date());

  res.status(201).json({
    message: 'Product registered for dynamic pricing',
    product,
    recommendedPrice: pricing.finalPrice,
    margin: pricing.margin,
  });
});

// ============================================
// API 2: LOGISTICS ROUTING API
// ============================================

const routingRouter = Router();
const routingEngine = new RouteOptimizationEngine();

interface Location {
  id: string;
  lat: number;
  lng: number;
  name: string;
}

interface Delivery {
  id: string;
  from: Location;
  to: Location;
  weight: number;
  priority: 'standard' | 'fast' | 'overnight';
  timeWindow?: { start: Date; end: Date };
}

const warehouse: Location = {
  id: 'warehouse_1',
  lat: 40.7128,
  lng: -74.006,
  name: 'Main Warehouse NYC',
};

const pendingDeliveries: Delivery[] = [];

/**
 * POST /api/v1/routing/routes/optimize
 * Generate optimized delivery routes
 */
routingRouter.post('/api/v1/routing/routes/optimize', (req: Request, res: Response) => {
  const { deliveryIds } = req.body;

  if (!deliveryIds || deliveryIds.length === 0) {
    return res.status(400).json({ error: 'No deliveries provided' });
  }

  // Get deliveries
  const selectedDeliveries = pendingDeliveries.filter((d) => deliveryIds.includes(d.id));

  if (selectedDeliveries.length === 0) {
    return res.status(404).json({ error: 'Deliveries not found' });
  }

  // Optimize by priority
  const routes = routingEngine.optimizeByPriority(warehouse, selectedDeliveries);

  res.json({
    routeCount: routes.length,
    totalDeliveries: selectedDeliveries.length,
    routes: routes.map((route, idx) => ({
      routeId: route.routeId,
      routeNumber: idx + 1,
      stops: route.stops.length,
      distance: route.totalDistance,
      distanceUnit: 'km',
      estimatedTime: route.estimatedTime,
      timeUnit: 'minutes',
      weight: route.totalWeight,
      cost: route.cost,
      efficiency: route.efficiency,
      stops: route.stops.map((s) => ({
        id: s.id,
        name: s.name,
        lat: s.lat,
        lng: s.lng,
      })),
    })),
    summary: {
      totalDistance: routes.reduce((sum, r) => sum + r.totalDistance, 0),
      totalCost: routes.reduce((sum, r) => sum + r.cost, 0),
      averageEfficiency:
        routes.reduce((sum, r) => sum + r.efficiency, 0) / routes.length,
      estimatedTotalTime: routes.reduce((sum, r) => sum + r.estimatedTime, 0),
    },
  });
});

/**
 * POST /api/v1/routing/deliveries
 * Add delivery to pending queue
 */
routingRouter.post('/api/v1/routing/deliveries', (req: Request, res: Response) => {
  const { toName, toLat, toLng, weight, priority } = req.body;

  const delivery: Delivery = {
    id: `del_${Date.now()}`,
    from: warehouse,
    to: {
      id: `loc_${Date.now()}`,
      name: toName,
      lat: toLat,
      lng: toLng,
    },
    weight,
    priority: priority || 'standard',
  };

  pendingDeliveries.push(delivery);

  res.status(201).json({
    message: 'Delivery added to queue',
    delivery,
    queueSize: pendingDeliveries.length,
  });
});

/**
 * GET /api/v1/routing/deliveries/pending
 * Get all pending deliveries
 */
routingRouter.get('/api/v1/routing/deliveries/pending', (req: Request, res: Response) => {
  res.json({
    totalPending: pendingDeliveries.length,
    byPriority: {
      overnight: pendingDeliveries.filter((d) => d.priority === 'overnight').length,
      fast: pendingDeliveries.filter((d) => d.priority === 'fast').length,
      standard: pendingDeliveries.filter((d) => d.priority === 'standard').length,
    },
    deliveries: pendingDeliveries.map((d) => ({
      id: d.id,
      to: d.to.name,
      weight: d.weight,
      priority: d.priority,
    })),
  });
});

// ============================================
// API 3: INVENTORY MANAGEMENT API
// ============================================

const inventoryRouter = Router();
const inventoryEngine = new InventoryOptimizationEngine();

interface SKU {
  id: string;
  name: string;
  currentStock: number;
  reorderPoint: number;
  leadTimeDays: number;
  holdingCost: number;
  orderingCost: number;
}

interface HistoricalDemand {
  date: Date;
  quantity: number;
}

const skus: Map<string, SKU> = new Map();
const demandHistory: Map<string, HistoricalDemand[]> = new Map();

/**
 * POST /api/v1/inventory/sku
 * Register SKU for inventory optimization
 */
inventoryRouter.post('/api/v1/inventory/sku', (req: Request, res: Response) => {
  const {
    name,
    currentStock,
    reorderPoint,
    leadTimeDays,
    holdingCost,
    orderingCost,
  } = req.body;

  const sku: SKU = {
    id: `sku_${Date.now()}`,
    name,
    currentStock,
    reorderPoint,
    leadTimeDays,
    holdingCost,
    orderingCost,
  };

  skus.set(sku.id, sku);
  demandHistory.set(sku.id, []);

  res.status(201).json({
    message: 'SKU registered',
    sku,
  });
});

/**
 * POST /api/v1/inventory/sku/:skuId/demand
 * Record demand/sales for a SKU
 */
inventoryRouter.post('/api/v1/inventory/sku/:skuId/demand', (req: Request, res: Response) => {
  const { skuId } = req.params;
  const { quantity, date } = req.body;

  if (!demandHistory.has(skuId)) {
    return res.status(404).json({ error: 'SKU not found' });
  }

  demandHistory.get(skuId)!.push({
    date: new Date(date),
    quantity,
  });

  res.json({ message: 'Demand recorded', skuId });
});

/**
 * GET /api/v1/inventory/sku/:skuId/reorder-analysis
 * Analyze if reorder is needed
 */
inventoryRouter.get('/api/v1/inventory/sku/:skuId/reorder-analysis', (req: Request, res: Response) => {
  const { skuId } = req.params;
  const sku = skus.get(skuId);
  const history = demandHistory.get(skuId) || [];

  if (!sku) {
    return res.status(404).json({ error: 'SKU not found' });
  }

  const avgDemand = inventoryEngine.forecastDemand(history);
  const eoq = inventoryEngine.calculateEOQ(
    avgDemand * 365,
    sku.orderingCost,
    sku.holdingCost
  );

  const analysis = inventoryEngine.shouldReorder(sku, avgDemand, sku.leadTimeDays);

  res.json({
    skuId,
    sku: { name: sku.name, currentStock: sku.currentStock },
    demandAnalysis: {
      avgDailyDemand: avgDemand.toFixed(2),
      forecastedAnnual: (avgDemand * 365).toFixed(0),
    },
    economicOrderQuantity: {
      recommendedQty: Math.round(eoq),
      explanation: `Balances ordering cost ($${sku.orderingCost}) with holding cost ($${sku.holdingCost}/unit)`,
    },
    reorderPoint: {
      current: sku.reorderPoint,
      recommended: inventoryEngine.calculateReorderPoint(avgDemand, sku.leadTimeDays),
    },
    decision: {
      shouldOrder: analysis.shouldOrder,
      recommendedQty: analysis.recommendedQty,
      daysUntilStockout: analysis.daysUntilStockout,
      reason: analysis.reason,
    },
    timestamp: new Date().toISOString(),
  });
});

/**
 * GET /api/v1/inventory/abc-analysis
 * Perform ABC inventory analysis
 */
inventoryRouter.get('/api/v1/inventory/abc-analysis', (req: Request, res: Response) => {
  const skuArray = Array.from(skus.values());

  const analysis = inventoryEngine.abcAnalysis(skuArray, demandHistory);

  res.json({
    timestamp: new Date().toISOString(),
    categories: {
      A: {
        count: analysis.A.length,
        items: analysis.A.map((item) => ({
          skuId: item.sku.id,
          name: item.sku.name,
          stock: item.sku.currentStock,
          annualValue: Math.round(item.annualValue),
          priority: 'HIGH - Tight control, frequent reviews',
        })),
      },
      B: {
        count: analysis.B.length,
        items: analysis.B.map((item) => ({
          skuId: item.sku.id,
          name: item.sku.name,
          stock: item.sku.currentStock,
          annualValue: Math.round(item.annualValue),
          priority: 'MEDIUM - Normal controls',
        })),
      },
      C: {
        count: analysis.C.length,
        items: analysis.C.map((item) => ({
          skuId: item.sku.id,
          name: item.sku.name,
          stock: item.sku.currentStock,
          annualValue: Math.round(item.annualValue),
          priority: 'LOW - Minimal control, simple reviews',
        })),
      },
    },
    summary: {
      totalItems: skuArray.length,
      aPercentage: ((analysis.A.length / skuArray.length) * 100).toFixed(1),
      bPercentage: ((analysis.B.length / skuArray.length) * 100).toFixed(1),
      cPercentage: ((analysis.C.length / skuArray.length) * 100).toFixed(1),
    },
  });
});

// ============================================
// API 4: HR COMPENSATION API
// ============================================

const hrRouter = Router();
const compensationEngine = new CompensationEngine();

interface Employee {
  id: string;
  name: string;
  baseSalary: number;
  department: string;
  yearsExperience: number;
  performanceRating: number;
  skillsCount: number;
  certificationsCount: number;
}

const employees: Map<string, Employee> = new Map();
const performanceMetrics: Map<string, any> = new Map();

/**
 * POST /api/v1/hr/compensation/calculate
 * Calculate total compensation for employee
 */
hrRouter.post('/api/v1/hr/compensation/calculate', (req: Request, res: Response) => {
  const { employeeId } = req.body;
  const employee = employees.get(employeeId);
  const metrics = performanceMetrics.get(employeeId);

  if (!employee || !metrics) {
    return res.status(404).json({ error: 'Employee or metrics not found' });
  }

  const compensation = compensationEngine.calculateTotalCompensationScore(employee, metrics);

  res.json({
    employeeId,
    employee: {
      name: employee.name,
      department: employee.department,
      yearsExperience: employee.yearsExperience,
    },
    compensation,
    breakdown: {
      baseSalary: employee.baseSalary,
      performanceBonus: compensation.performanceBonus,
      skillBonus: compensation.skillBonus,
      total: compensation.totalCompensation,
    },
    recommendation: {
      totalIncrease:
        compensation.totalCompensation - employee.baseSalary,
      percentageIncrease: compensation.percentageIncrease,
      message: `Employee should earn $${compensation.totalCompensation.toLocaleString()}/year (${compensation.percentageIncrease}% above base)`,
    },
  });
});

/**
 * GET /api/v1/hr/nine-box-analysis
 * 9-box matrix for all employees
 */
hrRouter.get('/api/v1/hr/nine-box-analysis', (req: Request, res: Response) => {
  const employeeArray = Array.from(employees.values());

  const analysis = compensationEngine.nineBoxAnalysis(employeeArray, performanceMetrics);

  const categories = {
    stars: analysis.filter((e) => e.category === 'Star - Promote'),
    corePlayers: analysis.filter((e) => e.category === 'Core Player - Retain'),
    highPotential: analysis.filter((e) => e.category === 'High Potential - Develop'),
    developing: analysis.filter((e) => e.category === 'Developing - Coach'),
    transition: analysis.filter((e) => e.category === 'Transition/Exit'),
  };

  res.json({
    timestamp: new Date().toISOString(),
    nineBox: {
      stars: {
        count: categories.stars.length,
        description: 'High Performance, High Potential - PROMOTE',
        employees: categories.stars.map((e) => e.employee),
        action: 'Fast-track for leadership roles',
      },
      corePlayers: {
        count: categories.corePlayers.length,
        description: 'High Performance, Medium Potential - RETAIN',
        employees: categories.corePlayers.map((e) => e.employee),
        action: 'Reward and retain with competitive compensation',
      },
      highPotential: {
        count: categories.highPotential.length,
        description: 'Medium Performance, High Potential - DEVELOP',
        employees: categories.highPotential.map((e) => e.employee),
        action: 'Mentor, training, stretch assignments',
      },
      developing: {
        count: categories.developing.length,
        description: 'Low Performance, High Potential - COACH',
        employees: categories.developing.map((e) => e.employee),
        action: 'Performance improvement plan',
      },
      transition: {
        count: categories.transition.length,
        description: 'Low Performance, Low Potential',
        employees: categories.transition.map((e) => e.employee),
        action: 'Consider transition or exit',
      },
    },
  });
});

// ============================================
// MAIN SERVER SETUP
// ============================================

export function setupMetierAPIs(app: express.Express) {
  app.use(pricingRouter);
  app.use(routingRouter);
  app.use(inventoryRouter);
  app.use(hrRouter);

  console.log('✅ Métier APIs registered');
  return app;
}

export { pricingRouter, routingRouter, inventoryRouter, hrRouter };

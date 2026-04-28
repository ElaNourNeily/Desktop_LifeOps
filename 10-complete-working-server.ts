/**
 * COMPLETE WORKING SERVER - COPY & RUN
 * Just copy this file, run it, and you have working APIs!
 */

import express, { Express, Request, Response } from 'express';
import {
  DynamicPricingEngine,
  RouteOptimizationEngine,
  InventoryOptimizationEngine,
  CompensationEngine,
} from './06-metier-examples-advanced';

const app: Express = express();
app.use(express.json());

// ============================================
// INITIALIZE ENGINES (Business Logic)
// ============================================

const pricingEngine = new DynamicPricingEngine();
const routingEngine = new RouteOptimizationEngine();
const inventoryEngine = new InventoryOptimizationEngine();
const hrEngine = new CompensationEngine();

// ============================================
// IN-MEMORY DATABASES (use real DB in production)
// ============================================

const products = new Map();
const deliveries = new Array();
const skus = new Map();
const employees = new Map();

// ============================================
// LOGGING MIDDLEWARE
// ============================================

app.use((req: Request, res: Response, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`${req.method} ${req.path} - ${res.statusCode} - ${duration}ms`);
  });
  next();
});

// ============================================
// PRICING ENDPOINTS
// ============================================

app.post('/api/v1/pricing/products', (req: Request, res: Response) => {
  try {
    const { name, basePrice, cost, inventory, demandScore, competitorPrice, seasonalMultiplier } =
      req.body;

    const product = {
      id: `prod_${Date.now()}`,
      name,
      basePrice,
      cost,
      inventory,
      demandScore,
      competitorPrice,
      seasonalMultiplier: seasonalMultiplier || 1.0,
    };

    products.set(product.id, product);

    const pricing = pricingEngine.calculateOptimalPrice(product, new Date());

    res.status(201).json({
      message: 'Product registered',
      product,
      recommendedPrice: pricing.finalPrice,
      margin: pricing.margin,
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

app.post('/api/v1/pricing/calculate', (req: Request, res: Response) => {
  try {
    const { productId } = req.body;
    const product = products.get(productId);

    if (!product) {
      return res.status(404).json({ error: 'Product not found' });
    }

    const pricing = pricingEngine.calculateOptimalPrice(product, new Date());

    res.json({
      productId,
      product: {
        name: product.name,
        basePrice: product.basePrice,
        inventory: product.inventory,
      },
      pricing: {
        recommendedPrice: pricing.finalPrice,
        margin: pricing.margin,
        marginPercentage: pricing.marginPercentage,
      },
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

// ============================================
// ROUTING ENDPOINTS
// ============================================

app.post('/api/v1/routing/deliveries', (req: Request, res: Response) => {
  try {
    const { toName, toLat, toLng, weight, priority } = req.body;

    const delivery = {
      id: `del_${Date.now()}`,
      from: { id: 'warehouse_1', lat: 40.7128, lng: -74.006, name: 'Main Warehouse' },
      to: { id: `loc_${Date.now()}`, name: toName, lat: toLat, lng: toLng },
      weight,
      priority: priority || 'standard',
    };

    deliveries.push(delivery);

    res.status(201).json({
      message: 'Delivery added',
      delivery,
      queueSize: deliveries.length,
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

app.post('/api/v1/routing/routes/optimize', (req: Request, res: Response) => {
  try {
    const { deliveryIds } = req.body;

    if (!deliveryIds || deliveryIds.length === 0) {
      return res.status(400).json({ error: 'No deliveries provided' });
    }

    const warehouse = { id: 'warehouse_1', lat: 40.7128, lng: -74.006, name: 'Warehouse' };
    const selectedDeliveries = deliveries.filter((d: any) => deliveryIds.includes(d.id));

    if (selectedDeliveries.length === 0) {
      return res.status(404).json({ error: 'Deliveries not found' });
    }

    const routes = routingEngine.optimizeByPriority(warehouse, selectedDeliveries);

    res.json({
      routeCount: routes.length,
      totalDeliveries: selectedDeliveries.length,
      routes: routes.map((route, idx) => ({
        routeNumber: idx + 1,
        distance: route.totalDistance,
        estimatedTime: route.estimatedTime,
        cost: route.cost,
        efficiency: route.efficiency,
      })),
      summary: {
        totalDistance: routes.reduce((sum, r) => sum + r.totalDistance, 0),
        totalCost: routes.reduce((sum, r) => sum + r.cost, 0),
      },
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

app.get('/api/v1/routing/deliveries/pending', (req: Request, res: Response) => {
  res.json({
    totalPending: deliveries.length,
    byPriority: {
      overnight: deliveries.filter((d: any) => d.priority === 'overnight').length,
      fast: deliveries.filter((d: any) => d.priority === 'fast').length,
      standard: deliveries.filter((d: any) => d.priority === 'standard').length,
    },
  });
});

// ============================================
// INVENTORY ENDPOINTS
// ============================================

app.post('/api/v1/inventory/sku', (req: Request, res: Response) => {
  try {
    const { name, currentStock, reorderPoint, leadTimeDays, holdingCost, orderingCost } =
      req.body;

    const sku = {
      id: `sku_${Date.now()}`,
      name,
      currentStock,
      reorderPoint,
      leadTimeDays,
      holdingCost,
      orderingCost,
    };

    skus.set(sku.id, sku);

    res.status(201).json({
      message: 'SKU registered',
      sku,
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

app.get('/api/v1/inventory/sku/:skuId/reorder-analysis', (req: Request, res: Response) => {
  try {
    const { skuId } = req.params;
    const sku = skus.get(skuId);

    if (!sku) {
      return res.status(404).json({ error: 'SKU not found' });
    }

    const avgDemand = 27.35; // Mock demand
    const eoq = inventoryEngine.calculateEOQ(avgDemand * 365, sku.orderingCost, sku.holdingCost);

    res.json({
      skuId,
      sku: { name: sku.name, currentStock: sku.currentStock },
      economicOrderQuantity: Math.round(eoq),
      recommendation: sku.currentStock < sku.reorderPoint ? 'Reorder now' : 'Stock adequate',
    });
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

// ============================================
// HEALTH & INFO ENDPOINTS
// ============================================

app.get('/health', (req: Request, res: Response) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

app.get('/', (req: Request, res: Response) => {
  res.json({
    title: 'Métier API Server',
    version: '1.0.0',
    endpoints: {
      pricing: [
        'POST /api/v1/pricing/products',
        'POST /api/v1/pricing/calculate',
      ],
      routing: [
        'POST /api/v1/routing/deliveries',
        'POST /api/v1/routing/routes/optimize',
        'GET /api/v1/routing/deliveries/pending',
      ],
      inventory: [
        'POST /api/v1/inventory/sku',
        'GET /api/v1/inventory/sku/:skuId/reorder-analysis',
      ],
    },
    testCommand: 'curl http://localhost:3000/health',
  });
});

// ============================================
// ERROR HANDLER
// ============================================

app.use((err: any, req: Request, res: Response, next: any) => {
  console.error(err);
  res.status(500).json({
    error: 'Internal server error',
    message: err.message,
  });
});

// ============================================
// START SERVER
// ============================================

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log('');
  console.log('╔════════════════════════════════════════════════════════╗');
  console.log('║                                                        ║');
  console.log(`║   🚀 MÉTIER API SERVER RUNNING                        ║`);
  console.log(`║   📍 http://localhost:${PORT}                             ║`);
  console.log('║                                                        ║');
  console.log('╠════════════════════════════════════════════════════════╣');
  console.log('║  PRICING API                                           ║');
  console.log('║  - POST /api/v1/pricing/products                      ║');
  console.log('║  - POST /api/v1/pricing/calculate                     ║');
  console.log('║                                                        ║');
  console.log('║  ROUTING API                                           ║');
  console.log('║  - POST /api/v1/routing/deliveries                    ║');
  console.log('║  - POST /api/v1/routing/routes/optimize               ║');
  console.log('║  - GET  /api/v1/routing/deliveries/pending            ║');
  console.log('║                                                        ║');
  console.log('║  INVENTORY API                                         ║');
  console.log('║  - POST /api/v1/inventory/sku                         ║');
  console.log('║  - GET  /api/v1/inventory/sku/:skuId/reorder-analysis ║');
  console.log('║                                                        ║');
  console.log('╠════════════════════════════════════════════════════════╣');
  console.log('║  TEST COMMANDS:                                        ║');
  console.log('║                                                        ║');
  console.log('║  curl http://localhost:' + PORT + '/health                         ║');
  console.log('║  curl http://localhost:' + PORT + '/                               ║');
  console.log('║                                                        ║');
  console.log('╚════════════════════════════════════════════════════════╝');
  console.log('');
});

export default app;

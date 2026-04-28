/**
 * COMPLETE API EXAMPLES WITH REQUESTS & RESPONSES
 * Real-world scenarios showing métier in action
 */

// ============================================
// EXAMPLE 1: DYNAMIC PRICING API
// ============================================

const pricingExample1 = {
  title: 'Calculate Optimal Price',
  endpoint: 'POST /api/v1/pricing/calculate',
  
  request: {
    productId: 'prod_1234567890',
  },

  response: {
    productId: 'prod_1234567890',
    product: {
      name: 'Wireless Headphones',
      basePrice: 100,
      currentCost: 35,
      inventory: 250,
    },
    pricing: {
      recommendedPrice: 127.8,
      margin: 92.8,
      marginPercentage: 72.65,
      factors: {
        demand: '+16%', // HIGH demand
        inventory: '+10%', // LOW stock
        competitor: '-2%', // 5% undercut
        seasonal: '+0%', // Not holiday season
      },
    },
    elasticity: {
      score: '-1.25',
      type: 'Unit elastic (balanced)',
    },
    recommendation: {
      recommendedPrice: 127.8,
      expectedImpact: 'Quantity down 5%, Revenue up 5%',
      reason: 'Unit elastic demand - current price is optimal',
    },
    timestamp: '2026-04-25T18:30:00.000Z',
  },
};

const pricingExample2 = {
  title: 'Analyze Price Sensitivity',
  endpoint: 'GET /api/v1/pricing/products/prod_1234567890/elasticity',

  response: {
    productId: 'prod_1234567890',
    productName: 'Wireless Headphones',
    priceHistory: [
      { price: 100, quantity: 150 },
      { price: 110, quantity: 140 },
      { price: 120, quantity: 125 },
      { price: 130, quantity: 115 },
    ],
    elasticity: '-0.42',
    interpretation:
      'INELASTIC: Customers not price sensitive. Increase price for more margin.',
    recommendation: {
      action: 'Increase price by 10%',
      expectedImpact: 'Quantity down 5%, Revenue up 5%',
    },
  },
};

const pricingExample3 = {
  title: 'Register Product',
  endpoint: 'POST /api/v1/pricing/products',

  request: {
    name: 'Premium Wireless Headphones',
    basePrice: 100,
    cost: 35,
    inventory: 250,
    demandScore: 80, // HIGH demand (0-100)
    competitorPrice: 95,
    seasonalMultiplier: 1.2, // Holiday season +20%
  },

  response: {
    message: 'Product registered for dynamic pricing',
    product: {
      id: 'prod_1234567890',
      name: 'Premium Wireless Headphones',
      basePrice: 100,
      cost: 35,
      inventory: 250,
      demandScore: 80,
      competitorPrice: 95,
      seasonalMultiplier: 1.2,
    },
    recommendedPrice: 137.28,
    margin: 102.28,
  },
};

// ============================================
// EXAMPLE 2: LOGISTICS ROUTING API
// ============================================

const routingExample1 = {
  title: 'Generate Optimized Routes',
  endpoint: 'POST /api/v1/routing/routes/optimize',

  request: {
    deliveryIds: ['del_001', 'del_002', 'del_003', 'del_004', 'del_005'],
  },

  response: {
    routeCount: 2,
    totalDeliveries: 5,
    routes: [
      {
        routeId: 'route_1682514000',
        routeNumber: 1,
        stops: 4,
        distance: 23.5,
        distanceUnit: 'km',
        estimatedTime: 45,
        timeUnit: 'minutes',
        weight: 45,
        cost: 97.0,
        efficiency: 87.3,
        stops: [
          {
            id: 'warehouse_1',
            name: 'Main Warehouse NYC',
            lat: 40.7128,
            lng: -74.006,
          },
          {
            id: 'loc_001',
            name: 'Manhattan Office',
            lat: 40.758,
            lng: -73.9855,
          },
          {
            id: 'loc_002',
            name: 'Brooklyn Store',
            lat: 40.6501,
            lng: -73.9496,
          },
          {
            id: 'loc_003',
            name: 'Queens Distribution',
            lat: 40.7282,
            lng: -73.7949,
          },
          {
            id: 'warehouse_1',
            name: 'Main Warehouse NYC',
            lat: 40.7128,
            lng: -74.006,
          },
        ],
      },
      {
        routeId: 'route_1682514100',
        routeNumber: 2,
        stops: 2,
        distance: 18.2,
        distanceUnit: 'km',
        estimatedTime: 32,
        timeUnit: 'minutes',
        weight: 32,
        cost: 86.4,
        efficiency: 91.5,
        stops: [
          {
            id: 'warehouse_1',
            name: 'Main Warehouse NYC',
            lat: 40.7128,
            lng: -74.006,
          },
          {
            id: 'loc_004',
            name: 'Bronx Hub',
            lat: 40.8448,
            lng: -73.8648,
          },
          {
            id: 'warehouse_1',
            name: 'Main Warehouse NYC',
            lat: 40.7128,
            lng: -74.006,
          },
        ],
      },
    ],
    summary: {
      totalDistance: 41.7,
      totalCost: 183.4,
      averageEfficiency: 89.4,
      estimatedTotalTime: 77,
    },
  },
};

const routingExample2 = {
  title: 'Add Delivery to Queue',
  endpoint: 'POST /api/v1/routing/deliveries',

  request: {
    toName: 'Manhattan Office - 5th Ave',
    toLat: 40.758,
    toLng: -73.9855,
    weight: 8,
    priority: 'fast',
  },

  response: {
    message: 'Delivery added to queue',
    delivery: {
      id: 'del_1682514200',
      from: {
        id: 'warehouse_1',
        lat: 40.7128,
        lng: -74.006,
        name: 'Main Warehouse NYC',
      },
      to: {
        id: 'loc_1682514200',
        name: 'Manhattan Office - 5th Ave',
        lat: 40.758,
        lng: -73.9855,
      },
      weight: 8,
      priority: 'fast',
    },
    queueSize: 12,
  },
};

const routingExample3 = {
  title: 'Check Pending Deliveries',
  endpoint: 'GET /api/v1/routing/deliveries/pending',

  response: {
    totalPending: 12,
    byPriority: {
      overnight: 2,
      fast: 5,
      standard: 5,
    },
    deliveries: [
      {
        id: 'del_001',
        to: 'Manhattan Office',
        weight: 10,
        priority: 'overnight',
      },
      {
        id: 'del_002',
        to: 'Brooklyn Store',
        weight: 8,
        priority: 'fast',
      },
      {
        id: 'del_003',
        to: 'Queens Distribution',
        weight: 12,
        priority: 'standard',
      },
      // ... more deliveries
    ],
  },
};

// ============================================
// EXAMPLE 3: INVENTORY MANAGEMENT API
// ============================================

const inventoryExample1 = {
  title: 'Analyze Reorder Need',
  endpoint: 'GET /api/v1/inventory/sku/sku_1234567890/reorder-analysis',

  response: {
    skuId: 'sku_1234567890',
    sku: { name: 'Phone Case', currentStock: 150 },
    demandAnalysis: {
      avgDailyDemand: '27.35',
      forecastedAnnual: '9983',
    },
    economicOrderQuantity: {
      recommendedQty: 1000,
      explanation:
        'Balances ordering cost ($100) with holding cost ($2/unit). Order 1000 units 10 times per year.',
    },
    reorderPoint: {
      current: 180,
      recommended: 185,
    },
    decision: {
      shouldOrder: false,
      recommendedQty: 1000,
      daysUntilStockout: 5,
      reason: 'Current stock 150 > reorder point 180, but stock decreasing. Monitor closely.',
    },
    timestamp: '2026-04-25T18:30:00.000Z',
  },
};

const inventoryExample2 = {
  title: 'ABC Inventory Analysis',
  endpoint: 'GET /api/v1/inventory/abc-analysis',

  response: {
    timestamp: '2026-04-25T18:30:00.000Z',
    categories: {
      A: {
        count: 12,
        items: [
          {
            skuId: 'sku_001',
            name: 'Server Rack',
            stock: 5,
            annualValue: 500000,
            priority: 'HIGH - Tight control, frequent reviews',
          },
          {
            skuId: 'sku_002',
            name: 'Enterprise Software License',
            stock: 25,
            annualValue: 450000,
            priority: 'HIGH - Tight control, frequent reviews',
          },
        ],
      },
      B: {
        count: 45,
        items: [
          {
            skuId: 'sku_050',
            name: 'Office Chair',
            stock: 120,
            annualValue: 95000,
            priority: 'MEDIUM - Normal controls',
          },
        ],
      },
      C: {
        count: 243,
        items: [
          {
            skuId: 'sku_300',
            name: 'Pen Set',
            stock: 500,
            annualValue: 5000,
            priority: 'LOW - Minimal control, simple reviews',
          },
        ],
      },
    },
    summary: {
      totalItems: 300,
      aPercentage: '4.0',
      bPercentage: '15.0',
      cPercentage: '81.0',
    },
  },
};

const inventoryExample3 = {
  title: 'Record Demand',
  endpoint: 'POST /api/v1/inventory/sku/sku_1234567890/demand',

  request: {
    quantity: 45,
    date: '2026-04-25',
  },

  response: {
    message: 'Demand recorded',
    skuId: 'sku_1234567890',
  },
};

// ============================================
// EXAMPLE 4: HR COMPENSATION API
// ============================================

const hrExample1 = {
  title: 'Calculate Total Compensation',
  endpoint: 'POST /api/v1/hr/compensation/calculate',

  request: {
    employeeId: 'emp_1234567890',
  },

  response: {
    employeeId: 'emp_1234567890',
    employee: {
      name: 'Alice Johnson',
      department: 'Engineering',
      yearsExperience: 6,
    },
    compensation: {
      baseSalary: 120000,
      performanceBonus: 24000, // 20% performance
      skillBonus: 18000, // 10 skills × 2% + 3 certs × 3%
      totalCompensation: 162000,
      percentageIncrease: 35.0,
    },
    breakdown: {
      baseSalary: 120000,
      performanceBonus: 24000,
      skillBonus: 18000,
      total: 162000,
    },
    recommendation: {
      totalIncrease: 42000,
      percentageIncrease: 35.0,
      message:
        'Employee should earn $162,000/year (35% above base salary). This reflects high performance and valuable skills.',
    },
  },
};

const hrExample2 = {
  title: '9-Box Matrix Analysis',
  endpoint: 'GET /api/v1/hr/nine-box-analysis',

  response: {
    timestamp: '2026-04-25T18:30:00.000Z',
    nineBox: {
      stars: {
        count: 5,
        description: 'High Performance, High Potential - PROMOTE',
        employees: ['Alice Johnson', 'Bob Smith', 'Carol Davis', 'David Lee', 'Emma Wilson'],
        action: 'Fast-track for leadership roles',
      },
      corePlayers: {
        count: 18,
        description: 'High Performance, Medium Potential - RETAIN',
        employees: [
          'Frank Miller',
          'Grace Chen',
          // ... more employees
        ],
        action: 'Reward and retain with competitive compensation',
      },
      highPotential: {
        count: 8,
        description: 'Medium Performance, High Potential - DEVELOP',
        employees: ['Henry Brown', 'Iris Martinez', // ... more employees
        ],
        action: 'Mentor, training, stretch assignments',
      },
      developing: {
        count: 4,
        description: 'Low Performance, High Potential - COACH',
        employees: ['Jack Taylor', 'Karen White'],
        action: 'Performance improvement plan with mentoring',
      },
      transition: {
        count: 2,
        description: 'Low Performance, Low Potential',
        employees: ['Larry Green', 'Michelle Orange'],
        action: 'Consider transition or exit strategy',
      },
    },
  },
};

// ============================================
// COMPLETE SERVER EXAMPLE
// ============================================

const completeServerExample = {
  title: 'Complete Server Setup',
  code: `
import express from 'express';
import { setupMetierAPIs } from './08-api-with-metier';

const app = express();
app.use(express.json());

// Setup all métier APIs
setupMetierAPIs(app);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', server: 'Métier API Server' });
});

// Start server
app.listen(3000, () => {
  console.log('🚀 Métier API Server running on http://localhost:3000');
  console.log('');
  console.log('📊 AVAILABLE ENDPOINTS:');
  console.log('');
  console.log('PRICING API:');
  console.log('  POST /api/v1/pricing/calculate');
  console.log('  GET  /api/v1/pricing/products/:productId/elasticity');
  console.log('  POST /api/v1/pricing/products');
  console.log('');
  console.log('ROUTING API:');
  console.log('  POST /api/v1/routing/routes/optimize');
  console.log('  POST /api/v1/routing/deliveries');
  console.log('  GET  /api/v1/routing/deliveries/pending');
  console.log('');
  console.log('INVENTORY API:');
  console.log('  POST /api/v1/inventory/sku');
  console.log('  POST /api/v1/inventory/sku/:skuId/demand');
  console.log('  GET  /api/v1/inventory/sku/:skuId/reorder-analysis');
  console.log('  GET  /api/v1/inventory/abc-analysis');
  console.log('');
  console.log('HR COMPENSATION API:');
  console.log('  POST /api/v1/hr/compensation/calculate');
  console.log('  GET  /api/v1/hr/nine-box-analysis');
});
  `,
};

// ============================================
// CURL EXAMPLES FOR TESTING
// ============================================

const curlExamples = {
  pricing: {
    title: 'Test Dynamic Pricing',
    commands: [
      `# Register a product
curl -X POST http://localhost:3000/api/v1/pricing/products \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Wireless Headphones",
    "basePrice": 100,
    "cost": 35,
    "inventory": 250,
    "demandScore": 80,
    "competitorPrice": 95,
    "seasonalMultiplier": 1.2
  }'`,

      `# Calculate optimal price
curl -X POST http://localhost:3000/api/v1/pricing/calculate \\
  -H "Content-Type: application/json" \\
  -d '{"productId": "prod_1234567890"}'`,

      `# Analyze price sensitivity
curl http://localhost:3000/api/v1/pricing/products/prod_1234567890/elasticity`,
    ],
  },

  routing: {
    title: 'Test Logistics Routing',
    commands: [
      `# Add delivery
curl -X POST http://localhost:3000/api/v1/routing/deliveries \\
  -H "Content-Type: application/json" \\
  -d '{
    "toName": "Manhattan Office",
    "toLat": 40.758,
    "toLng": -73.9855,
    "weight": 8,
    "priority": "fast"
  }'`,

      `# Get pending deliveries
curl http://localhost:3000/api/v1/routing/deliveries/pending`,

      `# Optimize routes
curl -X POST http://localhost:3000/api/v1/routing/routes/optimize \\
  -H "Content-Type: application/json" \\
  -d '{"deliveryIds": ["del_001", "del_002", "del_003"]}'`,
    ],
  },

  inventory: {
    title: 'Test Inventory Management',
    commands: [
      `# Register SKU
curl -X POST http://localhost:3000/api/v1/inventory/sku \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Phone Case",
    "currentStock": 250,
    "reorderPoint": 100,
    "leadTimeDays": 5,
    "holdingCost": 2,
    "orderingCost": 100
  }'`,

      `# Record demand
curl -X POST http://localhost:3000/api/v1/inventory/sku/sku_1234567890/demand \\
  -H "Content-Type: application/json" \\
  -d '{"quantity": 25, "date": "2026-04-25"}'`,

      `# Check reorder analysis
curl http://localhost:3000/api/v1/inventory/sku/sku_1234567890/reorder-analysis`,

      `# ABC analysis
curl http://localhost:3000/api/v1/inventory/abc-analysis`,
    ],
  },

  hr: {
    title: 'Test HR Compensation',
    commands: [
      `# Calculate compensation
curl -X POST http://localhost:3000/api/v1/hr/compensation/calculate \\
  -H "Content-Type: application/json" \\
  -d '{"employeeId": "emp_1234567890"}'`,

      `# 9-box analysis
curl http://localhost:3000/api/v1/hr/nine-box-analysis`,
    ],
  },
};

export {
  pricingExample1,
  pricingExample2,
  pricingExample3,
  routingExample1,
  routingExample2,
  routingExample3,
  inventoryExample1,
  inventoryExample2,
  inventoryExample3,
  hrExample1,
  hrExample2,
  completeServerExample,
  curlExamples,
};

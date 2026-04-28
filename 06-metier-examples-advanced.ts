/**
 * ADVANCED MÉTIER (BUSINESS LOGIC) EXAMPLES
 * Internal domain logic - NOT external API integrations
 */

// ============================================
// MÉTIER 1: E-COMMERCE - DYNAMIC PRICING ENGINE
// ============================================

interface Product {
  id: string;
  name: string;
  basePrice: number;
  cost: number;
  inventory: number;
  demandScore: number; // 0-100
  competitorPrice?: number;
  seasonalMultiplier: number;
}

interface PricingStrategy {
  finalPrice: number;
  margin: number;
  marginPercentage: number;
  factors: {
    demandFactor: number;
    inventoryFactor: number;
    competitorFactor: number;
    seasonalFactor: number;
  };
}

class DynamicPricingEngine {
  // AI-based pricing that maximizes revenue based on supply & demand
  calculateOptimalPrice(product: Product, timestamp: Date): PricingStrategy {
    let price = product.basePrice;

    // Factor 1: Demand elasticity (high demand = higher price)
    const demandFactor = 0.8 + (product.demandScore / 100) * 0.4; // 0.8-1.2x
    price *= demandFactor;

    // Factor 2: Inventory pressure (low stock = higher price to reduce demand)
    const inventoryLevel = product.inventory / 100; // Assume 100 is normal
    const inventoryFactor = Math.max(0.7, Math.min(1.3, 1 / (inventoryLevel + 0.5)));
    price *= inventoryFactor;

    // Factor 3: Competitor pricing (undercut by 5% if possible while maintaining margin)
    if (product.competitorPrice) {
      const competitorFactor = Math.min(
        1.0,
        (product.competitorPrice / product.basePrice) * 0.95
      ); // Max 5% undercut
      price *= competitorFactor;
    } else {
      const competitorFactor = 1.0;
    }

    // Factor 4: Seasonal multiplier (holiday season, back-to-school, etc)
    price *= product.seasonalMultiplier;

    // Ensure margin >= 30%
    const minPrice = product.cost * 1.3;
    price = Math.max(price, minPrice);

    const margin = price - product.cost;
    const marginPercentage = (margin / price) * 100;

    return {
      finalPrice: Math.round(price * 100) / 100,
      margin: Math.round(margin * 100) / 100,
      marginPercentage: Math.round(marginPercentage * 100) / 100,
      factors: {
        demandFactor,
        inventoryFactor,
        competitorFactor: product.competitorPrice
          ? Math.min(1.0, (product.competitorPrice / product.basePrice) * 0.95)
          : 1.0,
        seasonalFactor: product.seasonalMultiplier,
      },
    };
  }

  // Detect price elasticity (how sensitive customers are to price changes)
  calculatePriceElasticity(
    salesHistory: { price: number; quantity: number }[]
  ): number {
    if (salesHistory.length < 2) return -1;

    const sorted = salesHistory.sort((a, b) => a.price - b.price);
    const priceChange = (sorted[1].price - sorted[0].price) / sorted[0].price;
    const quantityChange = (sorted[1].quantity - sorted[0].quantity) / sorted[0].quantity;

    // Elasticity = % change in quantity / % change in price
    return quantityChange / (priceChange || 1);
  }

  // Recommend price change based on elasticity
  recommendPriceAdjustment(
    currentPrice: number,
    elasticity: number,
    targetMargin: number
  ): {
    recommendedPrice: number;
    expectedImpact: string;
    reason: string;
  } {
    if (elasticity > -0.5) {
      // Inelastic: price increase won't hurt much
      return {
        recommendedPrice: currentPrice * 1.1,
        expectedImpact: 'Quantity down 5%, Revenue up 5%',
        reason: 'Inelastic demand - customers insensitive to price',
      };
    } else if (elasticity < -2) {
      // Elastic: price sensitive
      return {
        recommendedPrice: currentPrice * 0.95,
        expectedImpact: 'Quantity up 8%, Revenue up 2%',
        reason: 'Elastic demand - lower price increases volume',
      };
    } else {
      // Unitary elasticity
      return {
        recommendedPrice: currentPrice,
        expectedImpact: 'Price and quantity balanced',
        reason: 'Unit elastic - maintain current price',
      };
    }
  }
}

// ============================================
// MÉTIER 2: LOGISTICS - ROUTE OPTIMIZATION
// ============================================

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

interface Route {
  routeId: string;
  stops: Location[];
  totalDistance: number;
  estimatedTime: number; // minutes
  totalWeight: number;
  cost: number;
  efficiency: number; // 0-100
}

class RouteOptimizationEngine {
  // Haversine formula: distance between two coordinates
  private calculateDistance(loc1: Location, loc2: Location): number {
    const R = 6371; // Earth radius in km
    const dLat = ((loc2.lat - loc1.lat) * Math.PI) / 180;
    const dLng = ((loc2.lng - loc1.lng) * Math.PI) / 180;

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((loc1.lat * Math.PI) / 180) *
        Math.cos((loc2.lat * Math.PI) / 180) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  // Nearest neighbor heuristic for TSP (Traveling Salesman Problem)
  optimizeRoute(
    warehouse: Location,
    deliveries: Delivery[],
    vehicleCapacity: number = 1000
  ): Route {
    const unvisited = [...deliveries];
    const route = [warehouse];
    let totalDistance = 0;
    let totalWeight = 0;
    let currentLocation = warehouse;

    while (unvisited.length > 0) {
      // Find nearest unvisited destination
      let nearest = 0;
      let minDistance = Infinity;

      unvisited.forEach((delivery, idx) => {
        const dist = this.calculateDistance(currentLocation, delivery.to);
        if (dist < minDistance && totalWeight + delivery.weight <= vehicleCapacity) {
          minDistance = dist;
          nearest = idx;
        }
      });

      const nextDelivery = unvisited.splice(nearest, 1)[0];
      totalDistance += this.calculateDistance(currentLocation, nextDelivery.to);
      totalWeight += nextDelivery.weight;
      route.push(nextDelivery.to);
      currentLocation = nextDelivery.to;

      // Return to warehouse if capacity reached or last delivery
      if (totalWeight >= vehicleCapacity * 0.9 || unvisited.length === 0) {
        totalDistance += this.calculateDistance(currentLocation, warehouse);
        route.push(warehouse);
        break;
      }
    }

    const estimatedTime = (totalDistance / 50) * 60; // Assume 50 km/h average
    const costPerKm = 2; // $2 per km
    const baseCost = 50; // Fixed cost per route
    const totalCost = baseCost + totalDistance * costPerKm;

    // Efficiency = (ideal route distance / actual route distance)
    // Ideal is straight line, so use lower bound estimate
    const directDistance = deliveries.reduce((sum, d) => {
      return sum + this.calculateDistance(warehouse, d.to);
    }, 0);
    const efficiency =
      (directDistance / (totalDistance || 1)) * 100 <= 100
        ? (directDistance / (totalDistance || 1)) * 100
        : 100;

    return {
      routeId: `route_${Date.now()}`,
      stops: route,
      totalDistance: Math.round(totalDistance * 10) / 10,
      estimatedTime: Math.round(estimatedTime),
      totalWeight,
      cost: Math.round(totalCost * 100) / 100,
      efficiency: Math.round(efficiency * 100) / 100,
    };
  }

  // Priority-based routing (fast deliveries first)
  optimizeByPriority(
    warehouse: Location,
    deliveries: Delivery[]
  ): Route[] {
    const grouped = {
      overnight: deliveries.filter((d) => d.priority === 'overnight'),
      fast: deliveries.filter((d) => d.priority === 'fast'),
      standard: deliveries.filter((d) => d.priority === 'standard'),
    };

    const routes: Route[] = [];

    // Process by priority
    [grouped.overnight, grouped.fast, grouped.standard].forEach((group) => {
      if (group.length > 0) {
        routes.push(this.optimizeRoute(warehouse, group));
      }
    });

    return routes;
  }
}

// ============================================
// MÉTIER 3: INVENTORY - DEMAND FORECASTING & STOCK OPTIMIZATION
// ============================================

interface HistoricalDemand {
  date: Date;
  quantity: number;
}

interface SKU {
  id: string;
  name: string;
  currentStock: number;
  reorderPoint: number;
  leadTimeDays: number;
  holdingCost: number; // $ per unit per day
  orderingCost: number; // $ per order
}

class InventoryOptimizationEngine {
  // Moving average forecast
  forecastDemand(history: HistoricalDemand[], periodDays: number = 30): number {
    const recentData = history.filter((d) => {
      const daysAgo = (Date.now() - d.date.getTime()) / (1000 * 60 * 60 * 24);
      return daysAgo <= periodDays;
    });

    if (recentData.length === 0) return 0;

    const totalQty = recentData.reduce((sum, d) => sum + d.quantity, 0);
    return totalQty / recentData.length;
  }

  // Economic Order Quantity (EOQ) formula
  calculateEOQ(
    annualDemand: number,
    orderingCost: number,
    holdingCost: number
  ): number {
    // EOQ = sqrt(2 * D * S / H)
    // D = annual demand, S = ordering cost, H = holding cost
    return Math.sqrt((2 * annualDemand * orderingCost) / holdingCost);
  }

  // Calculate reorder point
  calculateReorderPoint(
    avgDailyDemand: number,
    leadTimeDays: number,
    safetyStock: number = 1.5
  ): number {
    // ROP = (average daily demand × lead time in days) + safety stock
    return avgDailyDemand * leadTimeDays + safetyStock;
  }

  // Determine if reorder needed
  shouldReorder(
    sku: SKU,
    currentDemand: number,
    leadTimeDays: number
  ): {
    shouldOrder: boolean;
    recommendedQty: number;
    daysUntilStockout: number;
    reason: string;
  } {
    const stockoutDate = currentDemand > 0 ? sku.currentStock / currentDemand : Infinity;
    const daysUntilStockout = Math.round(stockoutDate);

    const recommendedQty = this.calculateEOQ(
      currentDemand * 365,
      sku.orderingCost,
      sku.holdingCost
    );

    const shouldOrder =
      sku.currentStock <= sku.reorderPoint ||
      daysUntilStockout < leadTimeDays;

    return {
      shouldOrder,
      recommendedQty: Math.round(recommendedQty),
      daysUntilStockout,
      reason: shouldOrder
        ? `Stock will run out in ${daysUntilStockout} days, lead time is ${leadTimeDays} days`
        : `Current stock ${sku.currentStock} > reorder point ${sku.reorderPoint}`,
    };
  }

  // ABC analysis: classify inventory by value
  abcAnalysis(
    skus: SKU[],
    historicalDemand: Map<string, HistoricalDemand[]>
  ): {
    A: { sku: SKU; annualValue: number }[];
    B: { sku: SKU; annualValue: number }[];
    C: { sku: SKU; annualValue: number }[];
  } {
    const values = skus.map((sku) => {
      const demand = historicalDemand.get(sku.id) || [];
      const avgDemand = this.forecastDemand(demand);
      const annualValue = avgDemand * 365 * sku.currentStock;
      return { sku, annualValue };
    });

    values.sort((a, b) => b.annualValue - a.annualValue);

    const total = values.reduce((sum, v) => sum + v.annualValue, 0);
    let cumulative = 0;

    const result = {
      A: [] as { sku: SKU; annualValue: number }[],
      B: [] as { sku: SKU; annualValue: number }[],
      C: [] as { sku: SKU; annualValue: number }[],
    };

    values.forEach((v) => {
      cumulative += v.annualValue;
      const percentage = (cumulative / total) * 100;

      if (percentage <= 80) {
        result.A.push(v);
      } else if (percentage <= 95) {
        result.B.push(v);
      } else {
        result.C.push(v);
      }
    });

    return result;
  }
}

// ============================================
// MÉTIER 4: HR - EMPLOYEE COMPENSATION & PERFORMANCE SCORING
// ============================================

interface Employee {
  id: string;
  name: string;
  baseSalary: number;
  department: string;
  yearsExperience: number;
  performanceRating: number; // 1-5
  skillsCount: number;
  certificationsCount: number;
}

interface PerformanceMetrics {
  projectsCompleted: number;
  tasksOnTime: number;
  qualityScore: number; // 1-100
  teamCollaboration: number; // 1-5
  knowledgeSharing: number; // 1-5
}

class CompensationEngine {
  // Calculate total compensation score
  calculateTotalCompensationScore(
    employee: Employee,
    metrics: PerformanceMetrics
  ): {
    baseSalary: number;
    performanceBonus: number;
    skillBonus: number;
    totalCompensation: number;
    percentageIncrease: number;
  } {
    // Performance bonus: up to 20% based on rating + metrics
    const performanceScore =
      (employee.performanceRating / 5) * 0.5 +
      (metrics.qualityScore / 100) * 0.3 +
      (metrics.teamCollaboration / 5) * 0.2;

    const performanceBonus = Math.round(
      employee.baseSalary * (performanceScore * 0.2)
    );

    // Skill bonus: 2% per skill, 3% per certification
    const skillBonus = Math.round(
      employee.baseSalary *
        (employee.skillsCount * 0.02 + employee.certificationsCount * 0.03)
    );

    // Experience adjustments
    const experienceBonus = Math.round(
      employee.baseSalary * Math.min(employee.yearsExperience * 0.03, 0.15)
    );

    const totalCompensation =
      employee.baseSalary + performanceBonus + skillBonus + experienceBonus;

    const percentageIncrease =
      ((totalCompensation - employee.baseSalary) / employee.baseSalary) * 100;

    return {
      baseSalary: employee.baseSalary,
      performanceBonus,
      skillBonus,
      totalCompensation,
      percentageIncrease: Math.round(percentageIncrease * 100) / 100,
    };
  }

  // Identify high performers (9-box matrix)
  nineBoxAnalysis(employees: Employee[], metricsMap: Map<string, PerformanceMetrics>) {
    return employees.map((emp) => {
      const metrics = metricsMap.get(emp.id);
      const potential = emp.yearsExperience < 3 ? 'high' : 'medium'; // New employees = high potential
      const performance =
        emp.performanceRating >= 4 ? 'high' : emp.performanceRating >= 3 ? 'medium' : 'low';

      return {
        employee: emp.name,
        performance,
        potential,
        category:
          performance === 'high' && potential === 'high'
            ? 'Star - Promote'
            : performance === 'high' && potential === 'medium'
              ? 'Core Player - Retain'
              : performance === 'medium' && potential === 'high'
                ? 'High Potential - Develop'
                : performance === 'low' && potential === 'high'
                  ? 'Developing - Coach'
                  : 'Transition/Exit',
      };
    });
  }

  // Determine salary bracket based on market data
  calculateMarketAdjustment(
    employee: Employee,
    marketMedian: number,
    marketPercentile: number = 50 // 50th percentile
  ): {
    currentSalary: number;
    recommendedSalary: number;
    adjustment: number;
    adjustmentPercentage: number;
  } {
    const marketBased = marketMedian * (1 + (marketPercentile - 50) / 1000);
    const adjustedForExperience = marketBased * (1 + employee.yearsExperience * 0.02);

    const recommendedSalary = Math.round(adjustedForExperience);
    const adjustment = recommendedSalary - employee.baseSalary;
    const adjustmentPercentage = (adjustment / employee.baseSalary) * 100;

    return {
      currentSalary: employee.baseSalary,
      recommendedSalary,
      adjustment,
      adjustmentPercentage: Math.round(adjustmentPercentage * 100) / 100,
    };
  }
}

// ============================================
// MÉTIER 5: REAL ESTATE - PROPERTY VALUATION & RECOMMENDATION
// ============================================

interface Property {
  id: string;
  address: string;
  squareFeet: number;
  yearBuilt: number;
  bedrooms: number;
  bathrooms: number;
  listingPrice: number;
  neighborhood: string;
  amenities: string[];
}

interface PropertyComparable {
  property: Property;
  soldPrice: number;
  soldDate: Date;
  daysOnMarket: number;
}

class PropertyValuationEngine {
  // Comparable sales method
  valueByCMP(
    property: Property,
    comparables: PropertyComparable[]
  ): {
    estimatedValue: number;
    pricePerSqFt: number;
    confidence: number;
    recommendation: string;
  } {
    if (comparables.length === 0) {
      return {
        estimatedValue: property.listingPrice,
        pricePerSqFt: property.listingPrice / property.squareFeet,
        confidence: 0,
        recommendation: 'Insufficient data',
      };
    }

    // Weight comparables by similarity
    const weights = comparables.map((comp) => {
      let similarity = 100;

      // Adjust for year built (older = less value)
      const ageAdjustment =
        Math.abs(comp.property.yearBuilt - property.yearBuilt) * 0.5;
      similarity -= Math.min(ageAdjustment, 20);

      // Adjust for size difference
      const sizeDiff = Math.abs(
        (comp.property.squareFeet - property.squareFeet) /
          property.squareFeet
      );
      similarity -= Math.min(sizeDiff * 100, 15);

      // Adjust for bedrooms/bathrooms
      const bedDiff = Math.abs(comp.property.bedrooms - property.bedrooms);
      similarity -= Math.min(bedDiff * 10, 10);

      return Math.max(0, similarity);
    });

    const totalWeight = weights.reduce((a, b) => a + b, 0);
    const weightedPrices = comparables.map(
      (comp, idx) => (comp.soldPrice * weights[idx]) / totalWeight
    );

    const estimatedValue = Math.round(
      weightedPrices.reduce((a, b) => a + b, 0)
    );
    const confidence = (totalWeight / (100 * comparables.length)) * 100;

    const listingValue = property.listingPrice;
    const variance = ((estimatedValue - listingValue) / listingValue) * 100;

    let recommendation = 'Fair';
    if (variance > 10) recommendation = 'Overpriced - Negotiate Down';
    if (variance < -10) recommendation = 'Underpriced - Good Deal';

    return {
      estimatedValue,
      pricePerSqFt: Math.round((estimatedValue / property.squareFeet) * 100) / 100,
      confidence: Math.round(confidence),
      recommendation,
    };
  }

  // Investment potential score
  calculateInvestmentScore(
    property: Property,
    expectedRent: number,
    interestRate: number = 0.05 // 5%
  ): {
    cashOnCashReturn: number;
    capRate: number;
    investmentScore: number; // 1-10
    recommendation: string;
  } {
    const annualRent = expectedRent * 12;
    const capRate = (annualRent / property.listingPrice) * 100;

    // Cash-on-cash: annual cash flow / down payment (assume 20% down)
    const downPayment = property.listingPrice * 0.2;
    const monthlyMortgage =
      (property.listingPrice * 0.8 * (interestRate / 12)) /
      (1 - Math.pow(1 + interestRate / 12, -360)); // 30-year mortgage

    const monthlyExpenses = expectedRent * 0.3; // Assume 30% maintenance/taxes
    const monthlyCashFlow = expectedRent - monthlyMortgage - monthlyExpenses;
    const cashOnCashReturn = (monthlyCashFlow * 12) / downPayment;

    const investmentScore = Math.min(10, (capRate * 2 + cashOnCashReturn * 3) / 5);

    let recommendation = 'Consider';
    if (investmentScore > 7) recommendation = 'Strong Buy';
    if (investmentScore > 5) recommendation = 'Buy';
    if (investmentScore < 3) recommendation = 'Avoid';

    return {
      cashOnCashReturn: Math.round(cashOnCashReturn * 100) / 100,
      capRate: Math.round(capRate * 100) / 100,
      investmentScore: Math.round(investmentScore * 10) / 10,
      recommendation,
    };
  }
}

export {
  DynamicPricingEngine,
  RouteOptimizationEngine,
  InventoryOptimizationEngine,
  CompensationEngine,
  PropertyValuationEngine,
};

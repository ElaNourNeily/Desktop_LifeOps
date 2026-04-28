/**
 * MORE ADVANCED MÉTIER EXAMPLES
 * Complex business logic for different domains
 */

// ============================================
// MÉTIER 6: HEALTHCARE - PATIENT RISK SCORING & TREATMENT OPTIMIZATION
// ============================================

interface Patient {
  id: string;
  age: number;
  weight: number;
  height: number;
  bloodType: string;
  chronicDiseases: string[];
  medications: string[];
  labResults: { test: string; value: number; normalRange: [number, number] }[];
}

interface TreatmentOption {
  name: string;
  cost: number;
  effectivenessRate: number; // 0-100
  sideEffects: string[];
  durationDays: number;
  contraindications: string[];
}

class PatientRiskScoringEngine {
  // Comprehensive health risk score (0-100)
  calculateHealthRiskScore(patient: Patient): {
    riskScore: number;
    riskLevel: 'low' | 'medium' | 'high' | 'critical';
    factors: { factor: string; impact: number }[];
    recommendations: string[];
  } {
    let riskScore = 0;
    const factors: { factor: string; impact: number }[] = [];

    // Age risk
    let ageRisk = 0;
    if (patient.age > 65) ageRisk = Math.min((patient.age - 65) * 1.5, 30);
    if (ageRisk > 0) factors.push({ factor: `Age (${patient.age})`, impact: ageRisk });
    riskScore += ageRisk;

    // BMI risk
    const bmi = (patient.weight / (patient.height * patient.height)) * 703;
    let bmiRisk = 0;
    if (bmi > 30) bmiRisk = Math.min((bmi - 30) * 2, 25);
    if (bmiRisk > 0) factors.push({ factor: `BMI (${bmi.toFixed(1)})`, impact: bmiRisk });
    riskScore += bmiRisk;

    // Chronic diseases risk
    const chronicDiseaseRisk = patient.chronicDiseases.length * 15;
    if (chronicDiseaseRisk > 0)
      factors.push({
        factor: `Chronic Diseases (${patient.chronicDiseases.length})`,
        impact: chronicDiseaseRisk,
      });
    riskScore += Math.min(chronicDiseaseRisk, 40);

    // Lab results risk
    let labRisk = 0;
    patient.labResults.forEach((result) => {
      const [min, max] = result.normalRange;
      if (result.value < min || result.value > max) {
        const deviation =
          (Math.abs(result.value - (min + max) / 2) / ((max - min) / 2)) * 10;
        labRisk += Math.min(deviation, 15);
      }
    });
    if (labRisk > 0)
      factors.push({
        factor: `Abnormal Lab Results (${patient.labResults.filter((r) => r.value < r.normalRange[0] || r.value > r.normalRange[1]).length})`,
        impact: labRisk,
      });
    riskScore += labRisk;

    // Medication interactions risk
    const drugInteractionRisk = patient.medications.length * 2;
    if (drugInteractionRisk > 0)
      factors.push({
        factor: `Medications (${patient.medications.length})`,
        impact: drugInteractionRisk,
      });
    riskScore += Math.min(drugInteractionRisk, 20);

    const finalScore = Math.min(riskScore, 100);

    let riskLevel: 'low' | 'medium' | 'high' | 'critical' = 'low';
    if (finalScore > 75) riskLevel = 'critical';
    else if (finalScore > 50) riskLevel = 'high';
    else if (finalScore > 25) riskLevel = 'medium';

    const recommendations: string[] = [];
    if (patient.age > 65) recommendations.push('Schedule regular preventive checkups');
    if (bmi > 30) recommendations.push('Consult nutritionist for weight management');
    if (patient.chronicDiseases.length > 2)
      recommendations.push('Consider specialist referrals');
    if (labRisk > 10) recommendations.push('Follow up on abnormal lab results');

    return {
      riskScore: Math.round(finalScore),
      riskLevel,
      factors: factors.sort((a, b) => b.impact - a.impact),
      recommendations,
    };
  }

  // Recommend optimal treatment
  recommendTreatment(
    patient: Patient,
    options: TreatmentOption[]
  ): {
    recommended: TreatmentOption;
    score: number;
    reasoning: string;
    safetyWarnings: string[];
  } {
    const riskProfile = this.calculateHealthRiskScore(patient);

    const scores = options.map((option) => {
      let score = option.effectivenessRate;

      // Reduce score if contraindications match
      option.contraindications.forEach((contra) => {
        if (patient.chronicDiseases.includes(contra)) {
          score -= 30;
        }
        if (patient.medications.some((med) => med.toLowerCase().includes(contra))) {
          score -= 25;
        }
      });

      // Adjust for patient risk level (high risk patients need safer options)
      if (riskProfile.riskLevel === 'critical') {
        score += option.sideEffects.length * -5; // Penalize side effects
      }

      return { option, score: Math.max(0, score) };
    });

    scores.sort((a, b) => b.score - a.score);
    const recommended = scores[0].option;

    const safetyWarnings: string[] = [];
    recommended.contraindications.forEach((contra) => {
      if (patient.chronicDiseases.includes(contra)) {
        safetyWarnings.push(`⚠️ Contraindicated for ${contra}`);
      }
    });

    return {
      recommended,
      score: Math.round(scores[0].score * 100) / 100,
      reasoning: `Score: ${scores[0].score.toFixed(0)}/100. Effectiveness: ${recommended.effectivenessRate}%, Cost: $${recommended.cost}, Duration: ${recommended.durationDays} days`,
      safetyWarnings,
    };
  }
}

// ============================================
// MÉTIER 7: FRAUD DETECTION - MULTI-DIMENSIONAL ANOMALY ENGINE
// ============================================

interface Transaction {
  id: string;
  userId: string;
  amount: number;
  merchant: string;
  location: { lat: number; lng: number };
  timestamp: Date;
  deviceId: string;
  category: string;
}

class FraudDetectionEngine {
  private userBehavior: Map<string, { transactions: Transaction[]; averages: any }> =
    new Map();

  // Calculate behavioral distance (behavioral analytics)
  calculateBehavioralAnomalyScore(transaction: Transaction, userHistory: Transaction[]): number {
    if (userHistory.length === 0) return 0; // New user

    let anomalyScore = 0;

    // 1. Amount anomaly
    const amounts = userHistory.map((t) => t.amount);
    const avgAmount = amounts.reduce((a, b) => a + b, 0) / amounts.length;
    const stdDevAmount = Math.sqrt(
      amounts.reduce((sum, a) => sum + Math.pow(a - avgAmount, 2), 0) / amounts.length
    );

    if (stdDevAmount > 0) {
      const amountZScore = Math.abs((transaction.amount - avgAmount) / stdDevAmount);
      anomalyScore += Math.min(amountZScore * 10, 30);
    }

    // 2. Location anomaly (velocity check)
    const lastTransaction = userHistory[userHistory.length - 1];
    if (lastTransaction) {
      const timeDiffMinutes =
        (transaction.timestamp.getTime() - lastTransaction.timestamp.getTime()) / (1000 * 60);
      const distance = this.calculateDistance(lastTransaction.location, transaction.location);
      const requiredTimeHours = distance / 500; // Max speed 500 km/h

      if (timeDiffMinutes < requiredTimeHours * 60) {
        anomalyScore += 25; // Impossible travel
      }
    }

    // 3. Category anomaly
    const categoryFrequency = userHistory.filter(
      (t) => t.category === transaction.category
    ).length;
    if (categoryFrequency === 0) {
      anomalyScore += 15; // New category
    }

    // 4. Time anomaly
    const transactionHour = transaction.timestamp.getHours();
    const typicalHours = userHistory
      .map((t) => t.timestamp.getHours())
      .filter((h) => h >= 8 && h <= 20);
    if (typicalHours.length > 0 && (transactionHour < 8 || transactionHour > 20)) {
      anomalyScore += 10; // Outside typical hours
    }

    // 5. Device anomaly
    const deviceFrequency = userHistory.filter((t) => t.deviceId === transaction.deviceId)
      .length;
    if (deviceFrequency === 0 && userHistory.length > 5) {
      anomalyScore += 20; // New device
    }

    return Math.min(anomalyScore, 100);
  }

  private calculateDistance(loc1: { lat: number; lng: number }, loc2: { lat: number; lng: number }): number {
    const R = 6371; // km
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

  // Detect structured fraud patterns
  detectStructuredFraud(transactions: Transaction[]): {
    pattern: string;
    confidence: number;
    transactions: string[];
  }[] {
    const patterns: {
      pattern: string;
      confidence: number;
      transactions: string[];
    }[] = [];

    // Pattern 1: Structuring (multiple small transactions to avoid threshold)
    const userTransactions = new Map<string, Transaction[]>();
    transactions.forEach((t) => {
      if (!userTransactions.has(t.userId)) {
        userTransactions.set(t.userId, []);
      }
      userTransactions.get(t.userId)!.push(t);
    });

    userTransactions.forEach((txns, userId) => {
      const amountsJustBelow10k = txns.filter(
        (t) => t.amount > 5000 && t.amount < 10000
      ).length;

      if (amountsJustBelow10k >= 3) {
        patterns.push({
          pattern: 'Structuring (transactions near $10k threshold)',
          confidence: Math.min(amountsJustBelow10k * 20, 95),
          transactions: txns.filter((t) => t.amount > 5000 && t.amount < 10000).map((t) => t.id),
        });
      }
    });

    return patterns;
  }

  // Determine action (block, approve, challenge)
  determineAction(
    transaction: Transaction,
    anomalyScore: number
  ): {
    action: 'approve' | 'challenge' | 'block';
    reason: string;
    additionalChecks: string[];
  } {
    if (anomalyScore > 75) {
      return {
        action: 'block',
        reason: 'High fraud risk detected',
        additionalChecks: [
          'Manual review required',
          'Contact customer for verification',
        ],
      };
    }

    if (anomalyScore > 50) {
      return {
        action: 'challenge',
        reason: 'Medium fraud risk - challenge customer',
        additionalChecks: [
          'Send 2FA code',
          'Request biometric verification',
          'Ask security questions',
        ],
      };
    }

    return {
      action: 'approve',
      reason: 'Low fraud risk',
      additionalChecks: [],
    };
  }
}

// ============================================
// MÉTIER 8: RECOMMENDATION ENGINE - COLLABORATIVE FILTERING
// ============================================

interface UserRating {
  userId: string;
  itemId: string;
  rating: number; // 1-5
  timestamp: Date;
}

interface Item {
  id: string;
  title: string;
  category: string;
  tags: string[];
  popularity: number;
}

class RecommendationEngine {
  // Calculate similarity between users (cosine similarity)
  calculateUserSimilarity(
    user1Ratings: Map<string, number>,
    user2Ratings: Map<string, number>
  ): number {
    const commonItems: string[] = [];
    user1Ratings.forEach((_, itemId) => {
      if (user2Ratings.has(itemId)) {
        commonItems.push(itemId);
      }
    });

    if (commonItems.length === 0) return 0;

    let dotProduct = 0;
    let magnitude1 = 0;
    let magnitude2 = 0;

    commonItems.forEach((itemId) => {
      const r1 = user1Ratings.get(itemId)!;
      const r2 = user2Ratings.get(itemId)!;
      dotProduct += r1 * r2;
      magnitude1 += r1 * r1;
      magnitude2 += r2 * r2;
    });

    magnitude1 = Math.sqrt(magnitude1);
    magnitude2 = Math.sqrt(magnitude2);

    if (magnitude1 === 0 || magnitude2 === 0) return 0;

    return dotProduct / (magnitude1 * magnitude2);
  }

  // Collaborative filtering recommendations
  recommendItems(
    userId: string,
    allRatings: UserRating[],
    items: Item[],
    topN: number = 5
  ): {
    itemId: string;
    title: string;
    predictedRating: number;
    reason: string;
  }[] {
    // Get user's ratings
    const userRatings = new Map<string, number>();
    allRatings
      .filter((r) => r.userId === userId)
      .forEach((r) => userRatings.set(r.itemId, r.rating));

    // Find similar users
    const otherUsers = new Set(allRatings.map((r) => r.userId));
    otherUsers.delete(userId);

    const userSimilarities: { userId: string; similarity: number }[] = [];

    otherUsers.forEach((otherUserId) => {
      const otherUserRatings = new Map<string, number>();
      allRatings
        .filter((r) => r.userId === otherUserId)
        .forEach((r) => otherUserRatings.set(r.itemId, r.rating));

      const similarity = this.calculateUserSimilarity(userRatings, otherUserRatings);
      if (similarity > 0) {
        userSimilarities.push({ userId: otherUserId, similarity });
      }
    });

    userSimilarities.sort((a, b) => b.similarity - a.similarity);
    const similarUsers = userSimilarities.slice(0, 10).map((u) => u.userId);

    // Get items similar users rated highly that current user hasn't rated
    const recommendations = new Map<string, { predictedRating: number; similarUsers: string[] }>();

    similarUsers.forEach((similarUserId) => {
      allRatings
        .filter((r) => r.userId === similarUserId && r.rating >= 4)
        .forEach((r) => {
          if (!userRatings.has(r.itemId)) {
            if (!recommendations.has(r.itemId)) {
              recommendations.set(r.itemId, { predictedRating: 0, similarUsers: [] });
            }
            const rec = recommendations.get(r.itemId)!;
            rec.predictedRating += r.rating;
            rec.similarUsers.push(similarUserId);
          }
        });
    });

    // Sort by predicted rating
    const sorted = Array.from(recommendations.entries())
      .map(([itemId, data]) => ({
        itemId,
        predictedRating: data.predictedRating / data.similarUsers.length,
        similarUsersCount: data.similarUsers.length,
      }))
      .sort((a, b) => b.predictedRating - a.predictedRating);

    return sorted.slice(0, topN).map((rec) => {
      const item = items.find((i) => i.id === rec.itemId);
      return {
        itemId: rec.itemId,
        title: item?.title || 'Unknown',
        predictedRating: Math.round(rec.predictedRating * 100) / 100,
        reason: `Similar users (${rec.similarUsersCount}) rated highly`,
      };
    });
  }
}

export {
  PatientRiskScoringEngine,
  FraudDetectionEngine,
  RecommendationEngine,
};

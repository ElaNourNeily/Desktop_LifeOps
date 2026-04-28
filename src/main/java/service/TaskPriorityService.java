package service;

import enums.StatutTache;
import model.task.Tache;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intelligent task prioritization service.
 * Generates explainable suggestions without forcing execution.
 */
public class TaskPriorityService {

    public enum Classification {
        CRITICAL, IMPORTANT, NORMAL, LOW_PRIORITY
    }

    public static class Recommendation {
        private final Tache task;
        private final double score;
        private final Classification classification;
        private final String reason;

        public Recommendation(Tache task, double score, Classification classification, String reason) {
            this.task = task;
            this.score = score;
            this.classification = classification;
            this.reason = reason;
        }

        public Tache getTask() { return task; }
        public double getScore() { return score; }
        public Classification getClassification() { return classification; }
        public String getReason() { return reason; }
    }

    public double calculateScore(Tache t) {
        if (t == null) return 0;

        double priorityScore = normalizePriority(t) * 3.0;
        double urgencyScore = computeUrgencyScore(t);
        double overdueBonus = isOverdue(t) ? 8.0 : 0.0;
        double difficultyWeight = Math.max(0, t.getDifficulte()) * 0.4;
        double stagnationBonus = computeStagnationBonus(t);

        return priorityScore + urgencyScore + overdueBonus + stagnationBonus - difficultyWeight;
    }

    public Classification classifyTask(Tache t) {
        double score = calculateScore(t);
        if (score >= 15) return Classification.CRITICAL;
        if (score >= 10) return Classification.IMPORTANT;
        if (score >= 6) return Classification.NORMAL;
        return Classification.LOW_PRIORITY;
    }

    public List<Recommendation> getRecommendedTasks(List<Tache> tasks) {
        if (tasks == null) return List.of();

        return tasks.stream()
                .filter(this::isActionable)
                .map(t -> new Recommendation(
                        t,
                        calculateScore(t),
                        classifyTask(t),
                        buildReason(t)
                ))
                .sorted(Comparator.comparingDouble(Recommendation::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Back-office insights for planning/distribution quality.
     */
    public Map<String, Object> analyzePlanningHealth(List<Tache> tasks) {
        Map<String, Object> out = new HashMap<>();
        if (tasks == null || tasks.isEmpty()) {
            out.put("overdueRate", 0.0);
            out.put("highDifficultyOpen", 0);
            out.put("avgRealTimeHours", 0.0);
            out.put("signals", List.of());
            return out;
        }

        List<Tache> actionable = tasks.stream().filter(this::isActionable).toList();
        long overdue = actionable.stream().filter(this::isOverdue).count();
        long highDifficultyOpen = actionable.stream().filter(t -> t.getDifficulte() >= 4).count();

        double avgRealSeconds = tasks.stream()
                .filter(t -> t.getRealTimeSpentSeconds() != null && t.getRealTimeSpentSeconds() > 0)
                .mapToInt(Tache::getRealTimeSpentSeconds)
                .average()
                .orElse(0);

        List<String> signals = new ArrayList<>();
        double overdueRate = actionable.isEmpty() ? 0 : (double) overdue / actionable.size();
        if (overdueRate > 0.3) signals.add("Poor planning: overdue rate is high.");
        if (highDifficultyOpen >= 3) signals.add("Potential overload: many high difficulty tasks still open.");
        if (avgRealSeconds / 3600.0 > 8) signals.add("Inefficiency signal: average real time per task is high.");

        out.put("overdueRate", overdueRate);
        out.put("highDifficultyOpen", highDifficultyOpen);
        out.put("avgRealTimeHours", avgRealSeconds / 3600.0);
        out.put("signals", signals);
        return out;
    }

    private boolean isActionable(Tache t) {
        return t != null && t.getStatut() != StatutTache.TERMINE;
    }

    private int normalizePriority(Tache t) {
        if (t.getPriorite() == null) return 2;
        return switch (t.getPriorite()) {
            case URGENTE -> 5;
            case HAUTE -> 4;
            case MOYENNE -> 3;
            case BASSE -> 1;
        };
    }

    private boolean isOverdue(Tache t) {
        if (t.getDeadline() == null) return false;
        return t.getDeadline().toInstant().isBefore(Instant.now());
    }

    private double computeUrgencyScore(Tache t) {
        if (t.getDeadline() == null) return 1.5;

        LocalDate due = t.getDeadline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), due);

        if (daysLeft < 0) return 6.0;
        if (daysLeft == 0) return 5.0;
        if (daysLeft <= 1) return 4.0;
        if (daysLeft <= 3) return 3.0;
        if (daysLeft <= 7) return 2.0;
        return 1.0;
    }

    private double computeStagnationBonus(Tache t) {
        if (t.getCreatedAt() == null || t.getUpdatedAt() == null) return 0;
        long daysWithoutUpdate = ChronoUnit.DAYS.between(
                t.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()
        );
        if (daysWithoutUpdate >= 5 && t.getDifficulte() >= 4) return 2.0;
        return 0;
    }

    private String buildReason(Tache t) {
        if (isOverdue(t)) return "Task is overdue";
        if (t.getDeadline() != null) {
            LocalDate due = t.getDeadline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), due);
            if (daysLeft <= 1 && normalizePriority(t) >= 4) return "High priority + deadline very close";
            if (daysLeft <= 3) return "Deadline in " + daysLeft + " day(s)";
        }
        if (t.getDifficulte() >= 4) return "High difficulty task needs early focus";
        return "Balanced priority and urgency";
    }
}


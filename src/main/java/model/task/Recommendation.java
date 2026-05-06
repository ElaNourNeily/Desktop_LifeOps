package model.task;

public class Recommendation {
    private int userId;
    private double score;
    private double predictedTime;
    private String reason;

    public Recommendation() {}

    public Recommendation(int userId, double score, double predictedTime, String reason) {
        this.userId = userId;
        this.score = score;
        this.predictedTime = predictedTime;
        this.reason = reason;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getPredictedTime() { return predictedTime; }
    public void setPredictedTime(double predictedTime) { this.predictedTime = predictedTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

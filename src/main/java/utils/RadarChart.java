package utils;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RadarChart extends Canvas {
    private String[] labels;
    private double[] maxValues;
    private double[] currentValues;
    private double[] targetValues;
    
    private AnimationTimer timer;
    private long startTime;
    private final long ANIMATION_DURATION = 800_000_000L; // 800ms

    public RadarChart(double width, double height) {
        super(width, height);
        // Default init
        labels = new String[]{"Sommeil", "Activité", "Hydratation", "Humeur"};
        maxValues = new double[]{10, 3, 10, 10}; // 10h, 3h (180m)/60, 10verres, 10/10 humeur
        currentValues = new double[]{0, 0, 0, 0};
        targetValues = new double[]{0, 0, 0, 0};
        drawChart();
    }

    public void updateData(double[] newTargetValues) {
        this.targetValues = newTargetValues.clone();
        
        if (timer != null) timer.stop();
        
        final double[] startValues = currentValues.clone();
        
        timer = new AnimationTimer() {
            private long startTime = -1;
            @Override
            public void handle(long now) {
                if (startTime == -1) startTime = now;
                long elapsed = now - startTime;
                if (elapsed >= ANIMATION_DURATION) {
                    currentValues = targetValues.clone();
                    drawChart();
                    stop();
                } else {
                    double fraction = (double) elapsed / ANIMATION_DURATION;
                    double eased = 1 - Math.pow(1 - fraction, 3);
                    for (int i = 0; i < currentValues.length; i++) {
                        currentValues[i] = startValues[i] + (targetValues[i] - startValues[i]) * eased;
                    }
                    drawChart();
                }
            }
        };
        timer.start();
    }

    private void drawChart() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double radius = Math.min(w, h) / 2 - 40; // padding

        gc.clearRect(0, 0, w, h);

        int sides = labels.length;
        if (sides == 0) return;

        // Draw web
        gc.setStroke(Color.rgb(63, 63, 70)); // Dark gray for web
        gc.setLineWidth(1.5);
        for (int level = 1; level <= 5; level++) {
            double r = radius * (level / 5.0);
            gc.beginPath();
            for (int i = 0; i < sides; i++) {
                double angle = i * 2 * Math.PI / sides - Math.PI / 2;
                double x = cx + r * Math.cos(angle);
                double y = cy + r * Math.sin(angle);
                if (i == 0) gc.moveTo(x, y);
                else gc.lineTo(x, y);
            }
            gc.closePath();
            gc.stroke();
        }

        // Draw axes and labels
        gc.setFill(Color.WHITE); // White text!
        gc.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides - Math.PI / 2;
            double px = cx + radius * Math.cos(angle);
            double py = cy + radius * Math.sin(angle);
            
            gc.setStroke(Color.rgb(63, 63, 70));
            gc.strokeLine(cx, cy, px, py);
            
            double tx = cx + (radius + 25) * Math.cos(angle);
            double ty = cy + (radius + 25) * Math.sin(angle);
            
            // Adjust label position slightly based on angle
            double offset = 20;
            if (angle > -Math.PI/4 && angle < Math.PI/4) tx -= offset/2; 
            if (angle > Math.PI/4 && angle < 3*Math.PI/4) { tx -= offset; ty += offset/2; }
            
            gc.fillText(labels[i], tx - 15, ty);
        }

        // Draw data area
        gc.beginPath();
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides - Math.PI / 2;
            // Prevent NaN or Infinity division
            double maxV = maxValues[i] == 0 ? 1 : maxValues[i];
            double ratio = Math.min(1.0, currentValues[i] / maxV);
            double x = cx + radius * ratio * Math.cos(angle);
            double y = cy + radius * ratio * Math.sin(angle);
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.closePath();
        
        gc.setFill(Color.rgb(139, 92, 246, 0.4)); // Indigo #8b5cf6 transparent
        gc.fill();
        gc.setStroke(Color.rgb(139, 92, 246)); // Indigo outline
        gc.setLineWidth(2.5);
        gc.stroke();

        // Draw points
        gc.setFill(Color.rgb(167, 139, 250)); // Lighter indigo points
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides - Math.PI / 2;
            double maxV = maxValues[i] == 0 ? 1 : maxValues[i];
            double ratio = Math.min(1.0, currentValues[i] / maxV);
            double x = cx + radius * ratio * Math.cos(angle);
            double y = cy + radius * ratio * Math.sin(angle);
            gc.fillOval(x - 5, y - 5, 10, 10);
        }
    }
}

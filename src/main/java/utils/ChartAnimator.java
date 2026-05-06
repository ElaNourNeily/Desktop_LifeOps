package utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class ChartAnimator {

    // 1. Compteur animé (AnimationTimer + easing cubic)
    public static void animateCounter(Label label, double targetValue, String suffix) {
        new AnimationTimer() {
            private long startTime = -1;
            private final long duration = 1500_000_000L; // 1.5s
            
            @Override
            public void handle(long now) {
                if (startTime == -1) startTime = now;
                long elapsed = now - startTime;
                double fraction = (double) elapsed / duration;
                if (fraction >= 1.0) {
                    label.setText(String.format("%.1f %s", targetValue, suffix));
                    stop();
                } else {
                    // Cubic ease out
                    double eased = 1 - Math.pow(1 - fraction, 3);
                    label.setText(String.format("%.1f %s", targetValue * eased, suffix));
                }
            }
        }.start();
    }

    // 2. Apparition des points un par un avec rebond
    public static void animateLineChartPoints(LineChart<?, ?> chart) {
        int delay = 0;
        for (XYChart.Series<?, ?> series : chart.getData()) {
            for (XYChart.Data<?, ?> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setScaleX(0);
                    node.setScaleY(0);
                    ScaleTransition st = new ScaleTransition(Duration.millis(500), node);
                    st.setToX(1);
                    st.setToY(1);
                    st.setDelay(Duration.millis(delay));
                    st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.5));
                    st.play();
                    delay += 100;
                }
            }
        }
    }

    // 3. Coloration des barres selon l'objectif + croissance
    public static void animateBarChart(BarChart<?, Number> chart, double threshold) {
        int delay = 0;
        for (XYChart.Series<?, Number> series : chart.getData()) {
            for (XYChart.Data<?, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setScaleY(0);
                    
                    if (data.getYValue().doubleValue() < threshold) {
                        node.setStyle("-fx-bar-fill: #e74c3c;"); // Rouge si sous obj
                    } else {
                        node.setStyle("-fx-bar-fill: #2ecc71;"); // Vert
                    }

                    ScaleTransition st = new ScaleTransition(Duration.millis(600), node);
                    st.setToY(1);
                    st.setDelay(Duration.millis(delay));
                    st.play();
                    delay += 50;
                }
            }
        }
    }

    // 4. Fade-in de l'AreaChart
    public static void animateAreaChart(AreaChart<?, ?> chart) {
        for (Node node : chart.lookupAll(".chart-series-area-fill")) {
            node.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.seconds(1), node);
            ft.setToValue(0.4);
            ft.play();
        }
    }

    // 5. Pulse d'alerte
    public static void alertPulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(400), node);
        st.setByX(0.1);
        st.setByY(0.1);
        st.setAutoReverse(true);
        st.setCycleCount(4);
        st.play();
    }

    // 6. Flash de mise à jour
    public static void flashUpdate(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }
}

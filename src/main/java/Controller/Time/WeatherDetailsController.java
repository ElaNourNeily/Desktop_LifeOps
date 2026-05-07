package Controller.Time;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.Time.external.WeatherService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WeatherDetailsController {

    @FXML private Label lblCity;
    @FXML private Label lblDate;
    @FXML private Label lblBigIcon;
    @FXML private Label lblCurrentTemp;
    @FXML private Label lblCondition;
    @FXML private Label lblHighLow;
    @FXML private Label lblFeelsLike;
    @FXML private HBox hboxHourly;
    @FXML private VBox vboxDaily;

    private final WeatherService weatherService = new WeatherService();
    private static final double TUNIS_LAT = 36.8065;
    private static final double TUNIS_LON = 10.1815;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.FRENCH)));
        loadWeatherData();
    }

    private void loadWeatherData() {
        new Thread(() -> {
            JsonObject report = weatherService.getFullWeatherReport(TUNIS_LAT, TUNIS_LON);
            if (report != null) {
                Platform.runLater(() -> updateUI(report));
            }
        }).start();
    }

    private void updateUI(JsonObject report) {
        JsonObject current = report.getAsJsonObject("current");
        double temp = current.get("temperature_2m").getAsDouble();
        double feels = current.get("apparent_temperature").getAsDouble();
        int code = current.get("weather_code").getAsInt();

        lblCurrentTemp.setText(Math.round(temp) + "°");
        
        String fullDesc = weatherService.interpretWeatherCode(code);
        String icon = extractEmoji(fullDesc);
        String cleanDesc = fullDesc.replace(icon, "").trim();
        
        lblBigIcon.setText(icon);
        lblCondition.setText(cleanDesc);
        lblFeelsLike.setText("|  Ressenti " + Math.round(feels) + "°");

        JsonObject daily = report.getAsJsonObject("daily");
        double max0 = daily.getAsJsonArray("temperature_2m_max").get(0).getAsDouble();
        double min0 = daily.getAsJsonArray("temperature_2m_min").get(0).getAsDouble();
        lblHighLow.setText("H: " + Math.round(max0) + "°  L: " + Math.round(min0) + "°");

        populateHourly(report.getAsJsonObject("hourly"));
        populateDaily(daily);
    }

    private String extractEmoji(String text) {
        if (text.contains("☀️")) return "☀️";
        if (text.contains("🌤️")) return "🌤️";
        if (text.contains("🌫️")) return "🌫️";
        if (text.contains("🌦️")) return "🌦️";
        if (text.contains("🌧️")) return "🌧️";
        if (text.contains("❄️")) return "❄️";
        if (text.contains("⛈️")) return "⛈️";
        if (text.contains("❓")) return "❓";
        return "☁️";
    }

    private void populateHourly(JsonObject hourly) {
        hboxHourly.getChildren().clear();
        JsonArray times = hourly.getAsJsonArray("time");
        JsonArray temps = hourly.getAsJsonArray("temperature_2m");
        JsonArray codes = hourly.getAsJsonArray("weather_code");

        int currentHour = LocalDateTime.now().getHour();
        for (int i = currentHour; i < currentHour + 12 && i < times.size(); i++) {
            VBox item = new VBox(8);
            item.setAlignment(Pos.CENTER);
            
            String hourStr = i + "h";
            if (i == currentHour) hourStr = "Maintenant";
            
            Label lblH = new Label(hourStr);
            lblH.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
            
            Label lblIcon = new Label(getIconForCode(codes.get(i).getAsInt()));
            lblIcon.setStyle("-fx-font-size: 20;");
            
            Label lblT = new Label(Math.round(temps.get(i).getAsDouble()) + "°");
            lblT.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            
            item.getChildren().addAll(lblH, lblIcon, lblT);
            hboxHourly.getChildren().add(item);
        }
    }

    private void populateDaily(JsonObject daily) {
        vboxDaily.getChildren().clear();
        JsonArray times = daily.getAsJsonArray("time");
        JsonArray maxs = daily.getAsJsonArray("temperature_2m_max");
        JsonArray mins = daily.getAsJsonArray("temperature_2m_min");
        JsonArray codes = daily.getAsJsonArray("weather_code");

        for (int i = 0; i < times.size(); i++) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 5 0;");

            String dayName = "Jour " + i;
            if (i == 0) dayName = "Aujourd'hui";
            else if (i == 1) dayName = "Demain";
            else {
                dayName = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH));
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
            }

            Label lblDay = new Label(dayName);
            lblDay.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-min-width: 100;");
            
            Label lblIcon = new Label(getIconForCode(codes.get(i).getAsInt()));
            lblIcon.setStyle("-fx-font-size: 18; -fx-min-width: 40;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label lblTemp = new Label(Math.round(maxs.get(i).getAsDouble()) + "°  " + Math.round(mins.get(i).getAsDouble()) + "°");
            lblTemp.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            row.getChildren().addAll(lblDay, lblIcon, spacer, lblTemp);
            vboxDaily.getChildren().add(row);
        }
    }

    private String getIconForCode(int code) {
        return extractEmoji(weatherService.interpretWeatherCode(code));
    }

    @FXML
    private void handleClose() {
        ((Stage) lblCity.getScene().getWindow()).close();
    }
}

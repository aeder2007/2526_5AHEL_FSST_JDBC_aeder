package com.example.jdbc;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.BubbleChart;
import javafx.scene.control.TextField;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

// TODO: DB sollte als Singleton ausgeführt werden
class DB {
    private static final String URL = "jdbc:postgresql://xserv:5432/world2";
    private static final String USER = "reader";
    private static final String PASSWORD = "reader";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}





public class HelloController {

    @FXML private BubbleChart<Number, Number> bubbleChart;
    @FXML private ComboBox<String> countryCombo;
   // final NumberAxis xAxis = new NumberAxis(1, 53, 4);
    //final NumberAxis yAxis = new NumberAxis(0, 80, 10);
    //final BubbleChart<Number,Number> bubbleChart = new
      //      BubbleChart<Number,Number>(xAxis,yAxis);
    String user="reader";
    String pw="reader";

    private final String url = "jdbc:postgresql://xserv:5432/world2";

    @FXML
    public void initialize() {
        loadCountries();

        countryCombo.setOnAction(e ->
                loadChartData(countryCombo.getValue())
        );
        NumberAxis xAxis = (NumberAxis) bubbleChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) bubbleChart.getYAxis();

       // xAxis.setAutoRanging(true);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(1400);
        yAxis.setTickUnit(100);
        xAxis.setUpperBound(16);
        xAxis.setTickUnit(2);
        xAxis.setLowerBound(0);
        xAxis.setAutoRanging(false);
        yAxis.setLabel("Bevölkerung (in Mio)");
        xAxis.setLabel("Fläche (Mio km²)");
    }

    private void loadCountries() {
        String sql = "SELECT name FROM country ORDER BY name";

        try (Connection con = DriverManager.getConnection(url, user, pw);
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                countryCombo.getItems().add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadChartData(String country) {
        bubbleChart.getData().clear();

        String sql = """
        SELECT surfacearea, population, lifeexpectancy
        FROM country
        WHERE name = ?
        """;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(country);

        try (Connection con = DriverManager.getConnection(url, user, pw);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, country);





            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double x = rs.getDouble("surfacearea")/1000000.0;
                    double y = rs.getDouble("population")/ 1000000.0;
                    double bubble = rs.getDouble("lifeexpectancy");
                    if (rs.wasNull()) bubble = 1;

                   double bubbleSize = Math.max(2, bubble / 10.0);

                    series.getData().add(
                            new XYChart.Data<>(x, y, bubbleSize)
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        bubbleChart.getData().add(series);

    }
}




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


// TODO: DB sollte als Singleton ausgeführt werden
class DB {
    private static final String URL = "jdbc:postgresql://xserv:5432/world2";
    private static final String USER = "reader";
    private static final String PASSWORD = "reader";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
class CountryRow {
    final String name;
    final double area;
    final long population;
    final double lifeExpectancy;

    CountryRow(String name, double area, long population, double lifeExpectancy) {
        this.name = name;
        this.area = area;
        this.population = population;
        this.lifeExpectancy = lifeExpectancy;
    }
}

public class HelloController {

    @FXML
    private NumberAxis xAxis; //CategoryAxis
    @FXML
    private NumberAxis yAxis;


    @FXML
    private BubbleChart<Number, Number> bubbleChart;
    @FXML
    private Label welcomeText;
    @FXML private ComboBox<String> countryCombo;

    @FXML
    private TextField filterField;

    private final List<CountryRow> allCountries = new ArrayList<>();
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }


    public HelloController() throws SQLException {

    }
    public void initialize(){
        bubbleChart.setTitle("X=Fläche, Y=Bevölkerung, Bubble=Lebenserwartung (skaliert)");

        // Filter reagiert live
        filterField.textProperty().addListener((obs, oldV, newV) -> drawChart(newV));

        // Initial laden (in Hintergrundthread, damit UI nicht hängt)
        reloadData();
    }


    @FXML
    private void reloadData() {
        new Thread(() -> {
            try {
                List<CountryRow> loaded = loadCountriesFromDB();
                Platform.runLater(() -> {
                    allCountries.clear();
                    allCountries.addAll(loaded);
                    drawChart(filterField.getText());
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("DB-Fehler", e.getMessage()));
            }
        }).start();
    }

    private List<CountryRow> loadCountriesFromDB() throws SQLException {
        String sql = """
            SELECT name, surfacearea, population, lifeexpectancy
            FROM country
            WHERE surfacearea IS NOT NULL
              AND population IS NOT NULL
              AND lifeexpectancy IS NOT NULL
            ORDER BY name
        """;

        List<CountryRow> list = new ArrayList<>();

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                double area = rs.getDouble("surfacearea");
                long population = rs.getLong("population");
                double life = rs.getDouble("lifeexpectancy");
                list.add(new CountryRow(name, area, population, life));
            }
        }
        return list;
    }

/*
    @FXML
    private void loadCountries(){
        System.out.println("Loadcountries aufgerufen ");
        String sql = "SELECT name FROM country ORDER BY name";
        ObservableList<String> countries = FXCollections.observableArrayList();

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                countries.add(rs.getString("name"));
            }

            countryCombo.setItems(countries);

        } catch (SQLException e) {
            //showError("DB-Fehler beim Laden der Länder", e.getMessage());
            System.out.println("DB-Fehler");
            System.out.println(e.getMessage());
        }



    }
*/


    private void drawChart(String filterText) {
        bubbleChart.getData().clear();

        String f = (filterText == null) ? "" : filterText.trim().toLowerCase();

        // Min/Max Lebenserwartung finden für Skalierung
        double minLife = Double.POSITIVE_INFINITY;
        double maxLife = Double.NEGATIVE_INFINITY;

        for (CountryRow c : allCountries) {
            if (matchesFilter(c.name, f)) {
                minLife = Math.min(minLife, c.lifeExpectancy);
                maxLife = Math.max(maxLife, c.lifeExpectancy);
            }
        }

        if (minLife == Double.POSITIVE_INFINITY) {
            // nix gefunden
            return;
        }

        // Bubble-Radius Grenzen (optisch angenehm)
        double minR = 3;   // kleinste Bubble
        double maxR = 18;  // größte Bubble

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Länder");

        for (CountryRow c : allCountries) {
            if (!matchesFilter(c.name, f)) continue;

            double radius = scale(c.lifeExpectancy, minLife, maxLife, minR, maxR);
//double radius=50;
            // x=Fläche, y=Bevölkerung, extraValue=BubbleRadius
            XYChart.Data<Number, Number> point = new XYChart.Data<>(c.area, c.population, radius);

            // Tooltip-Text als "node property" (Tooltip optional)
            point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-opacity: 0.8;");
                    newNode.setOnMouseEntered(e -> newNode.setStyle("-fx-opacity: 1.0;"));
                    newNode.setOnMouseExited(e -> newNode.setStyle("-fx-opacity: 0.8;"));
                    newNode.setAccessibleText(
                            c.name + "\nFläche: " + c.area +
                                    "\nBevölkerung: " + c.population +
                                    "\nLebenserwartung: " + c.lifeExpectancy
                    );
                }
            });

            series.getData().add(point);
        }

        bubbleChart.getData().add(series);
    }

    private boolean matchesFilter(String name, String filterLower) {
        if (filterLower.isEmpty()) return true;
        return name.toLowerCase().contains(filterLower);
    }

    private double scale(double value, double inMin, double inMax, double outMin, double outMax) {
        if (inMax <= inMin) return (outMin + outMax) / 2.0;
        double t = (value - inMin) / (inMax - inMin);
        return outMin + t * (outMax - outMin);
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }





}




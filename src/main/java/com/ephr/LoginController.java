package com.ephr;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.net.URL;
import org.json.JSONObject;

public class LoginController {
    @FXML
    private WebView webView;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            WebEngine webEngine = webView.getEngine();
            String authURL = "https://" + Auth0Helper.getDomain() + "/authorize" +
                    "?client_id=" + Auth0Helper.getClientId() +
                    "&response_type=code" +
                    "&redirect_uri=http://localhost:8080/callback" +
                    "&scope=openid profile email" +
                    "&prompt=login";  // <-- NEW: Forces login every time
    
            System.out.println("🔒 Redirecting to Auth0 login...");
            webEngine.load(authURL);
    
            // Listen for Auth0 callback
            webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
                if (newLocation.startsWith("http://localhost:8080/callback?code=")) {
                    System.out.println("✅ Auth0 Callback Detected!");
                    handleSuccessfulLogin(newLocation);
                }
            });
        });
    }    

    private void handleSuccessfulLogin(String callbackUrl) {
        try {
            String code = callbackUrl.split("code=")[1];
            String domain = Auth0Helper.getDomain();
            String clientId = Auth0Helper.getClientId();
            String clientSecret = Auth0Helper.getClientSecret();
            String redirectUri = "http://localhost:8080/callback";
    
            String payload = "grant_type=authorization_code"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&code=" + code
                    + "&redirect_uri=" + redirectUri;
    
            HttpURLConnection conn = (HttpURLConnection) new URL("https://" + domain + "/oauth/token").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
            }
    
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) responseBuilder.append(line);
            reader.close();
    
            JSONObject json = new JSONObject(responseBuilder.toString());
            String idToken = json.getString("id_token");
    
            DecodedJWT decodedJWT = JWT.decode(idToken);
            String email = decodedJWT.getClaim("email").asString();
    
            // 🧠 Get role here from DB
            String role = DatabaseHelper.getUserRoleByEmail(email);
            System.out.println("📧 Logged in as: " + email);
            System.out.println("🧾 Role: " + role);
    
            if (role == null) {
                Platform.runLater(() -> errorLabel.setText("❌ User not found in DB"));
                return;
            }
    
            Platform.runLater(() -> {
                try {
                    Main.showEPHRScreen(email, role); // ✅ pass role here
                } catch (IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("❌ Failed to load dashboard.");
                }
            });
    
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> errorLabel.setText("❌ Auth failed."));
        }
    }    

}
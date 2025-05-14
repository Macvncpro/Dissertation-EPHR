package com.ephr.controllers;

import com.ephr.blockchain.Block;
import com.ephr.blockchain.BlockchainLedger;
import com.ephr.helpers.EncryptionHelper;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.util.*;

import org.json.JSONObject;

public class BtGAuditController {

    @FXML private TableView<BlockRow> btgTable;
    @FXML private TableColumn<BlockRow, String> timestampCol;
    @FXML private TableColumn<BlockRow, String> dataCol;
    @FXML private TableColumn<BlockRow, String> tamperedCol;
    @FXML private TableColumn<BlockRow, String> userCol;
    @FXML private TableColumn<BlockRow, String> patientCol;
    @FXML private TableColumn<BlockRow, String> reasonCol;
    @FXML private TableColumn<BlockRow, String> categoryCol;
    @FXML private TableColumn<BlockRow, String> justificationCol;
    @FXML private Label statusLabel;

    private BlockchainLedger ledger;

    public static class BlockRow {
        private final String timestamp;
        private final String user;
        private final String patient;
        private final String reason;
        private final String category;
        private final String justification;
        private final String tampered;

        public BlockRow(String timestamp, String user, String patient, String reason,
                        String category, String justification, String tampered) {
            this.timestamp = timestamp;
            this.user = user;
            this.patient = patient;
            this.reason = reason;
            this.category = category;
            this.justification = justification;
            this.tampered = tampered;
        }

        public String getTimestamp() {
            return timestamp; 
        }

        public String getUser() {
            return user; 
        }

        public String getPatient() {
            return patient; 
        }

        public String getReason() {
            return reason; 
        }

        public String getCategory() {
            return category;
        }

        public String getJustification() {
            return justification; 
        }

        public String getTampered() {
            return tampered; 
        }

    }

    @FXML
    public void initialize() {
        timestampCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTimestamp()));
        userCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUser()));
        patientCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPatient()));
        reasonCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getReason()));
        categoryCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        justificationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getJustification()));
        tamperedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTampered()));

        loadLedger();
    }

    private void loadLedger() {
        ledger = new BlockchainLedger();
        List<Block> blocks = ledger.getChain();
        ObservableList<BlockRow> rows = FXCollections.observableArrayList();

        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String user = "?", patient = "?", reason = "?", category = "?", justification = "?";
            String tampered = "No";

            try {
                String decrypted = EncryptionHelper.decrypt(b.data);
                JSONObject json = new JSONObject(decrypted);
                user = json.optString("user", "?");
                patient = json.optString("patient", "?");
                reason = json.optString("reason", "?");
                category = json.optString("category", "?");
                justification = json.optString("justification", "?");
            } catch (Exception e) {
                justification = "[Decryption Failed]";
                tampered = "YES";
            }

            if (i > 0) {
                String expectedPrev = blocks.get(i - 1).hash;
                if (!b.previousHash.equals(expectedPrev) || !b.hash.equals(b.computeHash())) {
                    tampered = "YES";
                }
            }

            rows.add(new BlockRow(b.timestamp, user, patient, reason, category, justification, tampered));
        }

        btgTable.setItems(rows);
        statusLabel.setText(ledger.isValid() ? "✅ Chain is valid." : "❌ Tampering detected.");
    }


    @FXML
    private void handleValidate() {
        loadLedger();
    }

    @FXML
    private void handleExport() {
        try {
            File dest = new File("btg_chain_export.txt");
            try (InputStream in = new FileInputStream("btg_chain.log");
                 OutputStream out = new FileOutputStream(dest)) {
                in.transferTo(out);
            }
            statusLabel.setText("✅ Exported to " + dest.getAbsolutePath());
        } catch (IOException e) {
            statusLabel.setText("❌ Failed to export.");
            e.printStackTrace();
        }
    }
}
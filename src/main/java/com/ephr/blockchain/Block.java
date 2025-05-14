package com.ephr.blockchain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

public class Block {
    public String timestamp;
    public String previousHash;
    public String data;
    public String hash;

    public Block(String data, String previousHash) {
        this.timestamp = LocalDateTime.now().toString();
        this.data = data;
        this.previousHash = previousHash;
        this.hash = computeHash();
    }

    public String computeHash() {
        try {
            String input = timestamp + previousHash + data;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
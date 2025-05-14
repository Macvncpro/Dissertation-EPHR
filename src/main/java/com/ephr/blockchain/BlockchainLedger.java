package com.ephr.blockchain;

import java.io.*;
import java.util.*;
import com.ephr.helpers.EncryptionHelper;

public class BlockchainLedger {
    private List<Block> chain = new ArrayList<>();
    private final File ledgerFile = new File("btg_chain.log");

    public BlockchainLedger() {
        loadChain();
    }

    public void addRecord(String jsonData) {
        String encryptedData = EncryptionHelper.encrypt(jsonData);
        String previousHash = chain.isEmpty() ? "0" : chain.get(chain.size() - 1).hash;
        Block block = new Block(encryptedData, previousHash);
        chain.add(block);
        appendBlockToDisk(block);
    }

    private void appendBlockToDisk(Block block) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ledgerFile, true))) {
            writer.write(String.format(
                "{\"timestamp\":\"%s\",\"previousHash\":\"%s\",\"data\":\"%s\",\"hash\":\"%s\"}\n",
                block.timestamp, block.previousHash, block.data, block.hash
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadChain() {
        if (!ledgerFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(ledgerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String ts = line.split("\"timestamp\":\"")[1].split("\"")[0];
                String prev = line.split("\"previousHash\":\"")[1].split("\"")[0];
                String hash = line.split("\"hash\":\"")[1].split("\"")[0];
                String data = line.split("\"data\":\"")[1].split("\"")[0];

                Block b = new Block(data, prev);
                b.timestamp = ts;
                b.hash = hash;
                chain.add(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.previousHash.equals(previous.hash)) {
                System.out.println("❌ Tampering detected at block " + i + ": invalid previous hash.");
                return false;
            }

            if (!current.hash.equals(current.computeHash())) {
                System.out.println("❌ Tampering detected at block " + i + ": hash mismatch.");
                return false;
            }
        }
        return true;
    }

    public List<Block> getChain() {
        return Collections.unmodifiableList(chain);
    }

}
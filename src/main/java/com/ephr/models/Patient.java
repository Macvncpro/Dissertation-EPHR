package com.ephr.models;

public class Patient {
    private int id;
    private int userId;
    private String medicalHistory, allergies, insuranceCompany, insuranceNumber;

    public Patient(int id, int userId, String medicalHistory, String allergies, String insuranceCompany, String insuranceNumber) {
        this.id = id;
        this.userId = userId;
        this.medicalHistory = medicalHistory;
        this.allergies = allergies;
        this.insuranceCompany = insuranceCompany;
        this.insuranceNumber = insuranceNumber;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getMedicalHistory() { return medicalHistory; }
    public String getAllergies() { return allergies; }
    public String getInsuranceCompany() { return insuranceCompany; }
    public String getInsuranceNumber() { return insuranceNumber; }
}

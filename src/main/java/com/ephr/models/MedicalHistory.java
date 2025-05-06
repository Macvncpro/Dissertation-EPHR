package com.ephr.models;

public class MedicalHistory {
    private final int id;
    private final int patientId;
    private final String condition;
    private final String diagnosisDate;
    private final String treatment;
    private final String status;
    private final String severity;
    private final String notes;

    public MedicalHistory(int id, int patientId, String condition, String diagnosisDate, String treatment,
                          String status, String severity, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.condition = condition;
        this.diagnosisDate = diagnosisDate;
        this.treatment = treatment;
        this.status = status;
        this.severity = severity;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getCondition() {
        return condition;
    }
    
    public String getDiagnosisDate() {
        return diagnosisDate;
    }

    public String getTreatment() {
        return treatment;
    }

    public String getStatus() {
        return status; 
    }

    public String getSeverity() {
        return severity;
    }

    public String getNotes() {
        return notes;
    }
}
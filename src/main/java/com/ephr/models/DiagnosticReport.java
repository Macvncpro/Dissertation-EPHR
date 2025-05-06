package com.ephr.models;

public class DiagnosticReport {
    private final int id;
    private final int patientId;
    private final int doctorId;
    private final String reportType;
    private final String filePath;
    private final String reportDate;
    private final String comments;

    public DiagnosticReport(int id, int patientId, int doctorId, String reportType, String filePath, String reportDate, String comments) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.reportType = reportType;
        this.filePath = filePath;
        this.reportDate = reportDate;
        this.comments = comments;
    }

    public int getId() {
        return id; 
    }

    public int getPatientId() { 
        return patientId;
    }

    public int getDoctorId() {
        return doctorId; 
    }

    public String getReportType() {
        return reportType; 
    }

    public String getFilePath() {
        return filePath; 
    }

    public String getReportDate() { 
        return reportDate; 
    }

    public String getComments() {
        return comments; 
    }
    
}

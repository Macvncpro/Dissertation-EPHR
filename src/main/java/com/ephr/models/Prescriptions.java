package com.ephr.models;

public class Prescriptions {
    private int id;
    private int patientId;
    private int doctorId;
    private int medicationId;
    private String dosage;
    private String instructions;
    private String startDate;
    private String endDate;
    private String type;

    public Prescriptions(int id, int patientId, int doctorId, int medicationId,
                         String dosage, String instructions, String startDate, String endDate, String type) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.instructions = instructions;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
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

    public int getMedicationId() {
        return medicationId;
    }

    public String getDosage() {
        return dosage; 
    }

    public String getInstructions() {
        return instructions;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate; 
    }

    public String getType() {
        return type;
    }
    
}

package com.ephr.models;

public class PatientRecord {
    private final Users user;
    private final Patient details;

    public PatientRecord(Users user, Patient details) {
        this.user = user;
        this.details = details;
    }

    public String getFirstName() { return user.getFirstName(); }
    public String getLastName() { return user.getLastName(); }
    public String getEmail() { return user.getEmail(); }
    public String getGender() { return user.getGender(); }
    public String getDateOfBirth() { return user.getDateOfBirth(); }

    public String getMedicalHistory() { return details.getMedicalHistory(); }
    public String getAllergies() { return details.getAllergies(); }
    public String getInsuranceCompany() { return details.getInsuranceCompany(); }
    public String getInsuranceNumber() { return details.getInsuranceNumber(); }
}

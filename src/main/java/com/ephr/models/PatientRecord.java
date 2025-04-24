package com.ephr.models;

public class PatientRecord {

    private String firstName;
    private String lastName;    
    private String email;
    private String gender;
    private String dateOfBirth;

    private String nhsNumber;
    private String status;
    private boolean dataSharingConsent;
    private boolean scrConsent;
    private String phoneNumber;
    private String preferredContact;

    public PatientRecord(String firstName, String lastName, String email, String gender, String dateOfBirth,
                         String nhsNumber, String status, boolean dataSharingConsent, boolean scrConsent, String phoneNumber, String preferredContact) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.nhsNumber = nhsNumber;
        this.status = status;
        this.dataSharingConsent = dataSharingConsent;
        this.scrConsent = scrConsent;
        this.phoneNumber = phoneNumber;
        this.preferredContact = preferredContact;
    }

    public String getFirstName() { 
        return firstName; 
    }

    public String getLastName() { 
        return lastName; 
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNhsNumber() {
        return nhsNumber;
    }

    public String getStatus() {
        return status;
    }

    public boolean isDataSharingConsent() {
        return dataSharingConsent;
    }

    public boolean isScrConsent() {
        return scrConsent;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getPreferredContact() {
        return preferredContact;
    }
}
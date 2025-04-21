package com.ephr.models;

public class Users {
    private int id;
    private String firstName, lastName, email, gender, role, dateOfBirth;

    public Users(int id, String firstName, String lastName, String email, String gender, String role, String dateOfBirth) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.gender = gender;
        this.role = role;
        this.dateOfBirth = dateOfBirth;
    }

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public String getRole() { return role; }
    public String getDateOfBirth() { return dateOfBirth; }
}
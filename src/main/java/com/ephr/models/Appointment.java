package com.ephr.models;

public class Appointment {
    private int patientId;
    private String patientName;
    private String doctorName;
    private String date;
    private String time;
    private String status;

    public Appointment(int patientId, String patientName, String doctorName, String date, String time, String status) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public int getPatientId() { 
        return patientId; 
    }

    public String getPatientName() { 
        return patientName; 
    }

    public String getDoctorName() { 
        return doctorName; 
    }

    public String getDate() { 
        return date; 
    }

    public String getTime() { 
        return time; 
    }

    public String getStatus() { 
        return status; 
    }
}

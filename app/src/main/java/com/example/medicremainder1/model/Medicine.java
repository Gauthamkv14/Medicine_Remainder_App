package com.example.medicremainder1.model;

public class Medicine {
    private int id;
    private String name;
    private String time;

    public Medicine(int id, String name, String time) {
        this.id = id;
        this.name = name;
        this.time = time;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getTime() { return time; }
}

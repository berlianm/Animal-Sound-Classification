// DataAnimal.java
package com.example.bandungzoochatbot.assets;

public class DataAnimal {
    private String name;
    private int imageResId;
    private String description;
    private String uniqueFact;

    // Constructor
    public DataAnimal(String name, int imageResId, String uniqueFact, String description) {
        this.name = name;
        this.imageResId = imageResId;
        this.description = description;
        this.uniqueFact = uniqueFact;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getDescription() {
        return description;
    }

    public String getUniqueFact() {
        return uniqueFact;
    }
}

// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\Pet.java
package com.example.workshoppetshelter;

public final class Pet {

    private final long id;
    private final String name;
    private final String species;
    private final int age;
    private final String status;
    private final String requestedBy;

    public Pet(long id, String name, String species, int age, String status, String requestedBy) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.age = age;
        this.status = status;
        this.requestedBy = requestedBy;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public int getAge() {
        return age;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    /**
     * Level 2: ArrayAdapter's built-in Filter matches against toString().
     * Returning name + species lets one search find "Rex" and "Chat" alike.
     */
    @Override
    public String toString() {
        return (getName() == null ? "" : getName())
                + " "
                + (getSpecies() == null ? "" : getSpecies());
    }
}

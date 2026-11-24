// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserProfile.java
package com.example.workshoppetshelter;

public final class UserProfile {

    private final long id;
    private final String fullName;
    private final String email;
    private final Role role;

    public UserProfile(long id, String fullName, String email, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}

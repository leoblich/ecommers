package com.carpetadigital.ecommerce.utils.enums;

public enum RolEnum {
    USER(1, "USER"),
    ADMIN(2, "ADMIN");

    private final int id;
    private final String name;

    RolEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static RolEnum getById(int id) {
        for (RolEnum rol : values()) {
            if (rol.getId() == id) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Role not found");
    }

}

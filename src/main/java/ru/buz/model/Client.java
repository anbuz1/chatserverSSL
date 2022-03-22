package ru.buz.model;

import java.util.Objects;

public class Client {
    String name;
    String personalKey;

    public Client(String name, String personalKey) {
        this.name = name;
        this.personalKey = personalKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonalKey() {
        return personalKey;
    }

    public void setPersonalKey(String personalKey) {
        this.personalKey = personalKey;
    }

    @Override
    public String toString() {
        return "Client{" +
                "name='" + name + '\'' +
                ", personalKey='" + personalKey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(name, client.name) && Objects.equals(personalKey, client.personalKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, personalKey);
    }
}

package com.umpquariversoftware.metronome.elements;

public class Jam {
    Kit kit;
    Pattern pattern;
    int tempo;
    String name;
    int dbID;

    public Jam() {
    }

    public void setKit(Kit kit) {
        this.kit = kit;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public Kit getKit() {
        return kit;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getTempo() {
        return tempo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getInterval() {
        return 60000 / this.tempo;
    }

    public int getDbID() {
        return dbID;
    }

    public void setDbID(int dbID) {
        this.dbID = dbID;
    }
}


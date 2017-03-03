package com.umpquariversoftware.metronome.elements;

/**
 * A Jam is what the user ultimately creates. It's a combination of a kit, pattern, and tempo.
 * **/

public class Jam {
    private Kit kit;
    private Pattern pattern;
    private int tempo;
    private String name;
    private int dbID;

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

    @SuppressWarnings("unused")
    public int getDbID() {
        return dbID;
    }

    @SuppressWarnings("unused")
    public void setDbID(int dbID) {
        this.dbID = dbID;
    }
}


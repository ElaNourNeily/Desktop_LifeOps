package model;

public class Exercise {
    private String name;
    private String type;
    private String muscle;
    private String equipment;
    private String difficulty;
    private String instructions;

    public Exercise(String name, String type, String muscle, String equipment, String difficulty, String instructions) {
        this.name = name;
        this.type = type;
        this.muscle = muscle;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
    }

    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public String getMuscle() { return muscle; }
    public String getEquipment() { return equipment; }
    public String getDifficulty() { return difficulty; }
    public String getInstructions() { return instructions; }
}

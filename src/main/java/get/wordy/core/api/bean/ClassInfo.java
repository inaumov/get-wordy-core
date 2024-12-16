package get.wordy.core.api.bean;

public class ClassInfo {

    private String classId;
    private String name;
    private String format;
    private String level;
    private String material;
    private String notes;

    public ClassInfo(String classId, String name, String format, String level, String material, String notes) {
        this.classId = classId;
        this.name = name;
        this.format = format;
        this.level = level;
        this.material = material;
        this.notes = notes;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}

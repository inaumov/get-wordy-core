package get.wordy.core.api.bean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClassInfo {

    private String classId;
    private String name;
    private String format;
    private String level;
    private String material;
    private String notes;
    private boolean isRepeatable;
    private List<ClassSchedule> schedules = new ArrayList<>();
    private LocalDate endDate;
    private boolean isActive;

    public ClassInfo(String classId, String name, String format, String level, String material, String notes, boolean isRepeatable) {
        this.classId = classId;
        this.name = name;
        this.format = format;
        this.level = level;
        this.material = material;
        this.notes = notes;
        this.isRepeatable = isRepeatable;
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

    public boolean getIsRepeatable() {
        return isRepeatable;
    }

    public void setIsRepeatable(boolean isRepeatable) {
        this.isRepeatable = isRepeatable;
    }

    public List<ClassSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ClassSchedule> schedules) {
        this.schedules = schedules;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

}

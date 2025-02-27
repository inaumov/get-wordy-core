package get.wordy.core.api.bean;

import java.time.LocalDate;
import java.util.List;

public class ClassViewerInfo {

    private String classId;
    private boolean isActive;
    private String name;
    private String material;
    private String notes;
    private boolean isRepeatable;
    private List<ClassSchedule> schedules;
    private LocalDate endDate;

    public ClassViewerInfo(String classId, boolean isActive, String name, String material, String notes, boolean isRepeatable, List<ClassSchedule> schedules) {
        this.classId = classId;
        this.isActive = isActive;
        this.name = name;
        this.material = material;
        this.notes = notes;
        this.isRepeatable = isRepeatable;
        this.schedules = schedules;
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

    public List<ClassSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ClassSchedule> schedules) {
        this.schedules = schedules;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean getIsRepeatable() {
        return isRepeatable;
    }

    public void setRepeatable(boolean repeatable) {
        isRepeatable = repeatable;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

}

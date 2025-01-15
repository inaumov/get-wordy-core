package get.wordy.core.api.bean;

import java.time.LocalTime;
import java.util.List;

public class ClassInfo {

    private String classId;
    private String name;
    private String format;
    private String level;
    private String material;
    private String notes;
    private List<ClassSchedule> schedules;

    public ClassInfo(String classId, String name, String format, String level, String material, String notes, List<ClassSchedule> schedules) {
        this.classId = classId;
        this.name = name;
        this.format = format;
        this.level = level;
        this.material = material;
        this.notes = notes;
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

    public List<ClassSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ClassSchedule> schedules) {
        this.schedules = schedules;
    }

    public static class ClassSchedule {

        private String dayOfWeek; // E.g., "Mon", "Tue"
        private LocalTime startTime;
        private LocalTime endTime;

        public ClassSchedule(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }

}

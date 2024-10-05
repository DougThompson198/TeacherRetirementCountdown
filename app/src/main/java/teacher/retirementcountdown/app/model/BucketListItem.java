package teacher.retirementcountdown.app.model;

public class BucketListItem {
    private String name;
    private boolean isChecked;

    public BucketListItem(String name, boolean isChecked) {
        this.name = name;
        this.isChecked = isChecked;
    }

    public String getName() {
        return name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}

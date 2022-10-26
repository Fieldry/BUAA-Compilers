package backend;

public class Label {
    private final String label;

    public Label(String name) { label = name; }

    public String getLabel() { return label; }

    @Override
    public String toString() {
        return label;
    }
}

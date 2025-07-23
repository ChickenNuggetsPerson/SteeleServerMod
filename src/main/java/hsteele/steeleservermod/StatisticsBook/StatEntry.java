package hsteele.steeleservermod.StatisticsBook;



public class StatEntry {
    private final String name;
    private final int rawValue;
    private final String formattedValue;

    public StatEntry(String name, int rawValue, String formattedValue) {
        this.name = name;
        this.rawValue = rawValue;
        this.formattedValue = formattedValue;
    }

    public String getName() {
        return name;
    }

    public int getRawValue() {
        return rawValue;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    @Override
    public String toString() {
        return "StatEntry{name='" + name + "', rawValue=" + rawValue + ", formattedValue='" + formattedValue + "'}";
    }
}

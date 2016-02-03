package AtomM;

import java.text.SimpleDateFormat;
import java.util.Date;

public class QueryItem {

    public enum ItemType {
        NUMERIC, DATE, TEXT
    };

    private String name;
    private String value;
    private ItemType type;

    public QueryItem(String name, String value) {
        this.name = name;
        this.setValue(value);
    }

    public QueryItem(String name, long value) {
        this.name = name;
        this.setValue(value);
    }

    public QueryItem(String name, Date value) {
        this.name = name;
        this.setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public final void setValue(String value) {
        this.value = value;
        this.type = ItemType.TEXT;
    }

    public final void setValue(long value) {
        this.value = Long.toString(value);
        this.type = ItemType.NUMERIC;
    }

    public final void setValue(Date value) {
        this.value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
        this.type = ItemType.DATE;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }
}

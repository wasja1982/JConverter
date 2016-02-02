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
        this.value = value;
    }

    public QueryItem(String name, String value, ItemType type) {
        this.name = name;
        this.value = value;
        this.type = type;
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

    public void setValue(String value) {
        this.value = value;
        this.type = ItemType.TEXT;
    }

    public void setValue(long value) {
        this.value = Long.toString(value);
        this.type = ItemType.NUMERIC;
    }

    public void setValue(Date value) {
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

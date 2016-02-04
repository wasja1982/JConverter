package AtomM.SQL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Query implements Cloneable {

    private String table;
    private List<QueryItem> items;

    public Query(String table) {
        this.table = table;
        this.items = new ArrayList<QueryItem>();
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public int getItemsCount() {
        return items != null ? items.size() : 0;
    }

    public List getItems() {
        return items;
    }

    public void setItems(List items) {
        this.items = items;
    }

    public boolean addItem(QueryItem item) {
        return this.items.add(item);
    }

    public boolean addItem(String name, String value) {
        return this.items.add(new QueryItem(name, value));
    }

    public boolean addItem(String name, long value) {
        return this.items.add(new QueryItem(name, value));
    }

    public boolean addItem(String name, Date value) {
        return this.items.add(new QueryItem(name, value));
    }

    public QueryItem getItem(int number) {
        return items != null ? items.get(number) : null;
    }

    public void clearItems() {
        this.items.clear();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Query result = (Query) super.clone();
        result.setTable(this.table);
        result.getItems().addAll(this.items);
        return result;
    }
}

package AtomM;

import java.util.ArrayList;
import java.util.List;

public class Query {

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

    public QueryItem getItem(int number) {
        return items != null ? items.get(number) : null;
    }
}

package AtomM;

public class InsertQuery extends Query {

    public InsertQuery(String table) {
        super(table);
    }

    @Override
    public String toString() {
        String fields = "";
        String values = "";

        for (int i = 0; i < getItemsCount(); i++) {
            QueryItem item = getItem(i);
            if (item != null) {
                fields += (i > 0 ? ", " : "") + "`" + item.getName() + "`";
                values += (i > 0 ? ", " : "") + "'" + item.getValue() + "'";
            }
        }

        return "INSERT INTO `" + getTable() + "` (" + fields + ") VALUES (" + values + ");";
    }

}

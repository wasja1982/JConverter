package AtomM;

public class TruncateQuery extends Query {

    public TruncateQuery(String table) {
        super(table);
    }

    @Override
    public String toString() {
        return "TRUNCATE TABLE `" + getTable() + "`;";
    }
}

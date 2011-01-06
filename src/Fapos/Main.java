package Fapos;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // Associate ucoz file => Fapos .table
        String[] uTables = new String[] {"forum", "forump", "fr_fr", "nw_nw", "pu_pu", "news", "publ", "users"};
        ArrayList FpsData = new Converter("H:\\WebServers\\home\\localhost\\www\\PhpProject2\\_bk_2d8acada89").getSQL(uTables);
        try {
            OutputStreamWriter buf = new OutputStreamWriter ( new FileOutputStream ( "fapos.sql" ), "UTF-8" );
            for (int i = 0; i < FpsData.size(); i++) buf.write((String)FpsData.get(i) + "\r\n");
            buf.flush();
            buf.close();
        }
        catch (Exception e) {}
    }
}

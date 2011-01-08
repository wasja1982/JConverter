package Fapos;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        System.out.println( "JConvertor v0.2" );
        // Associate ucoz file => Fapos .table
        String[] uTables = new String[] {"users", "fr_fr", "forum", "forump", "nw_nw", "news", "pu_pu", "publ"};
        Converter conv = null;
//            conv = new Converter("H:\\WebServers\\home\\localhost\\www\\PhpProject2\\_bk_2d8acada89");
        if (args.length == 0) {
            conv = new Converter(".");
        } else if (args.length == 1) {
            conv = new Converter(args[0]);
        } else if (args.length > 1) {
            conv = new Converter(args[0], args[1]);
        }
        ArrayList FpsData = conv.getSQL(uTables);
        if (FpsData != null) {
            System.out.println( "Save \"fapos.sql\"..." );
            try {
                OutputStreamWriter buf = new OutputStreamWriter ( new FileOutputStream ( "fapos.sql" ), "UTF-8" );
                for (int i = 0; i < FpsData.size(); i++) buf.write((String)FpsData.get(i) + "\r\n");
                buf.flush();
                buf.close();
            }
            catch (Exception e) {
                System.out.println( "File \"fapos.sql\" not saved (" + e.getMessage() + ")." );
            }
        }
    }
}

package AtomM;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Main {

    static int CURRENT_VERSION = 10;

    private static void createFile(String filename, ArrayList data) {
        try {
            OutputStreamWriter buf = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
            for (int i = 0; i < data.size(); i++) {
                Object item = data.get(i);
                if (item == null) {
                    continue;
                }
                String str = item instanceof String ? (String) item : item.toString();
                buf.write(str + "\r\n");
            }
            buf.flush();
            buf.close();
        } catch (Exception e) {
            System.out.println("File \"" + filename + "\" not saved (" + e.getMessage() + ").");
        }
    }

    public static void main(String[] args) {
        System.out.println("JConvertor v0.4");
        String path = ".";
        String pref = "";
        String password = null;
        boolean parseAll = true;
        boolean[] parse = new boolean[]{false, false, false, false, false};
        boolean sqlSplit = false;
        boolean useWebAvatars = false;
        boolean noEmpty = false;
        boolean noImage = false;
        boolean parseSmile = false;
        boolean noFix = false;
        int version = CURRENT_VERSION;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-path")) {
                path = (i + 1 < args.length) ? args[i + 1] : ".";
            } else if (args[i].equalsIgnoreCase("-pref")) {
                pref = (i + 1 < args.length) ? args[i + 1] : "";
            } else if (args[i].equalsIgnoreCase("-pass")) {
                password = (i + 1 < args.length) ? args[i + 1] : null;
            } else if (args[i].equalsIgnoreCase("-users")) {
                parse[0] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-forum")) {
                parse[1] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-loads")) {
                parse[2] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-publ")) {
                parse[3] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-news")) {
                parse[4] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-comments")) {
                parse[5] = true;
                parseAll = false;
            } else if (args[i].equalsIgnoreCase("-split")) {
                sqlSplit = true;
            } else if (args[i].equalsIgnoreCase("-wa")) {
                useWebAvatars = true;
            } else if (args[i].equalsIgnoreCase("-noempty")) {
                noEmpty = true;
            } else if (args[i].equalsIgnoreCase("-noimage")) {
                noImage = true;
            } else if (args[i].equalsIgnoreCase("-smile")) {
                parseSmile = true;
            } else if (args[i].equalsIgnoreCase("-nofix")) {
                noFix = true;
            } else if (args[i].toLowerCase().startsWith("-v") && args[i].length() > 2) {
                String ver = args[i].substring(2);
                try {
                    version = Integer.parseInt(ver);
                } catch (NumberFormatException ex) {
                    version = CURRENT_VERSION;
                }
                if (version < 0 || version > CURRENT_VERSION) {
                    version = CURRENT_VERSION;
                }
            }
        }

        String[][] uTables = new String[][]{{"users"},
        {"fr_fr", "forum", "forump"},
        {"ld_ld", "loads"},
        {"pu_pu", "publ"},
        {"nw_nw", "news", "bl_bl", "blog", "fq_fq", "faq"},
        {"comments"}};

        String[] uFiles = new String[]{"users", "forum", "loads", "stat", "news", "comments"};

        Converter conv = new Converter(path, pref);

        conv.PASSWORD = password;
        conv.USE_WEB_AVATARS = useWebAvatars;
        conv.NO_EMPTY = noEmpty;
        conv.NO_IMAGE = noImage;
        conv.PARSE_SMILE = parseSmile;
        conv.NO_FIX = noFix;
        conv.VERSION = version;

        if (conv != null && conv.initUsers()) {
            ArrayList FpsData = null;
            if (!sqlSplit) {
                FpsData = new ArrayList();
            }
            for (int i = 0; i < uFiles.length; i++) {
                if (!parseAll && !parse[i]) {
                    continue;
                }
                ArrayList temp = conv.getSQL(uTables[i]);
                if (temp != null) {
                    if (sqlSplit) {
                        String filename = "atomx_" + uFiles[i] + ".sql";
                        System.out.println("Save \"" + filename + "\"...");
                        createFile(filename, temp);
                    } else {
                        FpsData.addAll(temp);
                    }
                }
            }
            if (FpsData != null) {
                System.out.println("Save \"atomx.sql\"...");
                createFile("atomx.sql", FpsData);
            }
        }
    }
}

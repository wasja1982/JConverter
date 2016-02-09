package AtomM;

import AtomM.GUI.FConverter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Main {

    static int CURRENT_VERSION = 11;

    public static void main(String[] args) {
        String path = ".";
        String pref = "";
        String site_old = null;
        String site_new = null;
        String password = null;
        Integer postOnForum = null;
        boolean parseAll = true;
        boolean[] parse = new boolean[]{false, false, false, false, false, false};
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
            } else if (args[i].equalsIgnoreCase("-pof")) {
                try {
                    postOnForum = (i + 1 < args.length) ? Integer.parseInt(args[i + 1]) : null;
                } catch (Exception e) {
                };
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
            } else if (args[i].equalsIgnoreCase("-so")) {
                site_old = (i + 1 < args.length) ? args[i + 1] : null;
            } else if (args[i].equalsIgnoreCase("-sn")) {
                site_new = (i + 1 < args.length) ? args[i + 1] : null;
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

        FConverter conv = new FConverter();
        conv.setPath(path);
        conv.setPref(pref);
        conv.setPassword(password);
        conv.setWebAvatars(useWebAvatars);
        conv.setNoEmpty(noEmpty);
        conv.setNoImage(noImage);
        conv.setSmile(parseSmile);
        conv.setSiteName(site_old, site_new, postOnForum);
        conv.setParse(parseAll, parse);
        conv.setSplit(sqlSplit);
        conv.setNoFix(noFix);
        conv.setVersion(version);
        conv.setVisible(true);
    }
}

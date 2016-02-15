package AtomM;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Main {

    static int CURRENT_VERSION = 11;

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
        System.out.println("JConvertor v0.5");
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
        boolean useWebLoads = false;
        boolean useWebAttaches = false;
        boolean noEmpty = false;
        boolean noImage = false;
        boolean parseSmile = false;
        boolean noFix = false;
        boolean noGroups = false;
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
            } else if (args[i].equalsIgnoreCase("-wl")) {
                useWebLoads = true;
            } else if (args[i].equalsIgnoreCase("-wt")) {
                useWebAttaches = true;
            } else if (args[i].equalsIgnoreCase("-noempty")) {
                noEmpty = true;
            } else if (args[i].equalsIgnoreCase("-noimage")) {
                noImage = true;
            } else if (args[i].equalsIgnoreCase("-smile")) {
                parseSmile = true;
            } else if (args[i].equalsIgnoreCase("-nofix")) {
                noFix = true;
            } else if (args[i].equalsIgnoreCase("-nogroups")) {
                noGroups = true;
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

        if (site_new != null && !site_new.toLowerCase().startsWith("http://") && !site_new.toLowerCase().startsWith("https://")) {
            site_new = "http://" + site_new;
        }
        
        String[] uFiles = new String[]{"users", "forum", "loads", "stat", "news", "comments"};

        Converter conv = new Converter(path, pref);
        conv.setPassword(password);
        conv.setWebAvatars(useWebAvatars);
        conv.setWebLoads(useWebLoads);
        conv.setWebAttaches(useWebAttaches);
        conv.setNoEmpty(noEmpty);
        conv.setNoImage(noImage);
        conv.setSmile(parseSmile);
        conv.setSiteName(site_old, site_new, postOnForum);
        conv.setNoFix(noFix);
        conv.setNoGroups(noGroups);
        conv.setVersion(version);

        System.out.println("Stage 1: loading backup files...");
        if (conv != null && conv.initUsers()) {
            conv.loadBackups(parseAll, parse);
            System.out.println("Stage 2: copying files and updating links...");
            conv.linksUpdate();
            System.out.println("Stage 3: parsing backup...");
            ArrayList atmData = null;
            if (!sqlSplit) {
                atmData = new ArrayList();
            }
            ArrayList temp = conv.getSQL();
            if (temp != null) {
                for (int i = 0; i < temp.size(); i++) {
                    if ((!parseAll && !parse[i]) || temp.get(i) == null) {
                        continue;
                    }
                    if (sqlSplit) {
                        String filename = "atomm_" + uFiles[i] + ".sql";
                        System.out.println("Save \"" + filename + "\"...");
                        createFile(filename, (ArrayList) temp.get(i));
                    } else {
                        atmData.addAll((ArrayList) temp.get(i));
                    }
                }
            }
            if (atmData != null) {
                System.out.println("Save \"atomm.sql\"...");
                createFile("atomm.sql", atmData);
            }
        }
    }
}

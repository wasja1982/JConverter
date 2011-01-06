// Исправил багу с `description` в `forums`
// Бага с $rec['message'] и $rec['main'] в parse_news
// Проверить edittime для таблицы 'posts' - OK
// Категории новостей и статей общие?

package Fapos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

public class Converter {
    String DS = File.separator;
    // Dir with ucoz dump
    String DUMP = "";
    // dir with txt files in ucoz dump
    String DUMP_TABLES = "";
    // prefix for tables in data base, if exists
    String PREF = "";

    public Converter(String DUMP) {
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
    }
    
    public Converter(String DUMP, String DUMP_TABLES) {
        this.DUMP = DUMP + DS;
        this.DUMP_TABLES = DUMP_TABLES + DS;
    }

    public Converter(String DUMP, String DUMP_TABLES, String PREF) {
        this.DUMP = DUMP + DS;
        this.DUMP_TABLES = DUMP_TABLES + DS;
        this.PREF = PREF;
    }

    private String parseDate(String date) {
        String parse = "0000-00-00 00:00:00";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            try {
                parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(date) * 1000));
            } catch (Exception e) {
                parse = "0000-00-00 00:00:00";
            }
        }
        return parse;
    }

    private String addslashes(String data) {
        String str = data.replace("\\", "\\\\");
//        str = str.replace("\"", "\\\"");
        str = str.replace("'", "\\'");
        return str;
    }
/*
    private String htmlspecialchars(String data) {
        String str = data.replace("&", "&amp;");
        str = str.replace("\"", "&quot;");
        str = str.replace("'", "&#039;");
        str = str.replace("<", "&lt;");
        str = str.replace(">", "&gt;");
        return str;
    }

    private String adsl(String data) {
        return addslashes(htmlspecialchars(data));
    }
*/

    /**
     * This function create a array with
     * associations ucoz User name -> ucoz User ID
     *
     * Because ucoz havn't uUsersData id
     */
    private TreeMap getUserIds() {
        String filename = DUMP_TABLES + "users.txt";
        File users = new File(filename);
        if (!users.exists()) return null;

        TreeMap uUsers = new TreeMap();

        try {
            BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( filename ), "UTF-8" ) );
            String line = null;
            int id = 1;
            while ( ( line = br.readLine () ) != null ) {
                String [] user = line.split( "\\|" );
                if (user.length < 1) continue;
                uUsers.put(user[0], Integer.toString(id));
                id++;
            }
        }
        catch (Exception e) {}
        if (uUsers.size() == 0) return null;
        else return uUsers;
    }

    /**
     * Work for forum.txt
     *
     */
    private String parse_forum(String[] uRecord, TreeMap uUsers) {
/*
        String last_post = "0000-00-00 00:00:00";
        try {
            last_post = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(uRecord[4]) * 1000));
        } catch (Exception e) {
            last_post = "0000-00-00 00:00:00";
        }
*/
        String id_author = "1";
        try {
            Object ob = uUsers.get(uRecord[10]);
            id_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "1";
        } catch (Exception e) {
            id_author = "1";
        }
        String output = String.format("INSERT INTO `" + PREF + "themes`"
            + " (`id`, `id_forum`, `important`, `last_post`, `locked`, `posts`, `views`, `title`, `id_author`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], uRecord[1], uRecord[3], parseDate(uRecord[4]), uRecord[5], uRecord[6], uRecord[7], addslashes(uRecord[8]), id_author);
//            uRecord[0], uRecord[1], uRecord[3], last_post, uRecord[5], uRecord[6], uRecord[7], addslashes(uRecord[8]), id_author);
        return output;
    }

    /**
     * Work for forump.txt
     *
     */
    private String parse_forump(String[] uRecord, TreeMap uUsers) {
/*
        String time = "0000-00-00 00:00:00";
        try {
            time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(uRecord[2]) * 1000));
        } catch (Exception e) {
            time = "0000-00-00 00:00:00";
        }
*/
        String id_author = "1";
        try {
            Object ob = uUsers.get(uRecord[6]);
            id_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "1";
        } catch (Exception e) {
            id_author = "1";
        }
/*
        String edittime = "0000-00-00 00:00:00";
        try {
            edittime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(uRecord[9]) * 1000));
        } catch (Exception e) {
            edittime = "0000-00-00 00:00:00";
        }
*/
        String attaches = "0";
        try {
            attaches = ((uRecord[10] != null) && !((String)uRecord[10]).isEmpty()) ? "1" : "0";
        } catch (Exception e) {
            attaches = "0";
        }
        String output = String.format("INSERT INTO `" + PREF + "posts`"
            + " (`id`, `id_theme`, `time`, `message`, `id_author`, `edittime`, `attaches`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], uRecord[1], parseDate(uRecord[2]), addslashes(uRecord[4]), id_author, parseDate(uRecord[9]), attaches);
//            uRecord[0], uRecord[1], time, addslashes(uRecord[4]), id_author, edittime, attaches);
        return output;
    }

    /**
     * Work for fr_fr.txt
     *
     */
    private String parse_fr_fr(String[] uRecord, TreeMap uUsers) {
        String output;
        if (uRecord[1] == null || uRecord[1].isEmpty() || uRecord[1].equals("0")) {
            output = String.format("INSERT INTO `" + PREF + "forum_cat`"
                + " (`id`, `title`) VALUES"
                + " ('%s', '%s');",
                uRecord[0], addslashes(uRecord[5]));
        } else {
            String last_theme_id = "";
            try {
                last_theme_id = ((uRecord[16] != null) && !((String)uRecord[16]).isEmpty()) ? (String)uRecord[16] : "";
            } catch (Exception e) {
                last_theme_id = "";
            }
            String themes = "0";
            try {
                themes = ((uRecord[9] != null) && !((String)uRecord[9]).isEmpty()) ? (String)uRecord[9] : "0";
            } catch (Exception e) {
                themes = "0";
            }
            String posts = "0";
            try {
                posts = ((uRecord[10] != null) && !((String)uRecord[10]).isEmpty()) ? (String)uRecord[10] : "0";
            } catch (Exception e) {
                posts = "0";
            }
            output = String.format("INSERT INTO `" + PREF + "forums`"
                + " (`id`, `in_cat`, `title`, `description`, `last_theme_id`, `themes`, `posts`) VALUES"
                + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                uRecord[0], uRecord[1], addslashes(uRecord[5]), addslashes(uRecord[6]), last_theme_id, themes, posts);
        }
        return output;
    }

    /**
     * Work for nw_nw.txt
     *
     */
    private String parse_nw_nw(String[] uRecord, TreeMap uUsers, TreeMap uCategories) {
        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !((String)uRecord[1]).isEmpty()) ? (String)uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        String class_sections = "category";
        try {
            if ((uRecord[2] == null) || ((String)uRecord[2]).isEmpty()) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
        } catch (Exception e) {
            section_id = "category";
        }

        String output = String.format("INSERT INTO `" + PREF + "news_sections`"
            + " (`id`, `section_id`, `title`, `class`) VALUES"
            + " ('%s', '%s', '%s', '%s');",
        uRecord[0], section_id, addslashes(uRecord[5]), class_sections);
        uCategories.put(uRecord[0], section_id); // $_SESSION['categories'][$rec['id']] = $rec['section_id'];
        return output;
    }

    /**
     * Work for pu_pu.txt
     *
     */
    private String parse_pu_pu(String[] uRecord, TreeMap uUsers, TreeMap uCategories) {

        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !((String)uRecord[1]).isEmpty()) ? (String)uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        String class_sections = "category";
        try {
            if ((uRecord[2] == null) || ((String)uRecord[2]).isEmpty()) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
        } catch (Exception e) {
            section_id = "category";
        }

        String output = String.format("INSERT INTO `" + PREF + "stat_sections`"
            + " (`id`, `section_id`, `title`, `class`) VALUES"
            + " ('%s', '%s', '%s', '%s');",
        uRecord[0], section_id, addslashes(uRecord[5]), class_sections);
        uCategories.put(uRecord[0], section_id); // $_SESSION['categories'][$rec['id']] = $rec['section_id'];
        return output;
    }

    /**
     * Work for news.txt
     *
     */
    private String parse_news(String[] uRecord, TreeMap uUsers, TreeMap uCategories) {
        // May be incorect data
        if (uRecord[11] == null || uRecord[13] == null) return null;

        // Prepare variable which uses in SQL query
        String date = "0000-00-00 00:00:00";
        try {
            date = String.format("%04d-%02d-%02d 00:00:00", Integer.parseInt(uRecord[2]), Integer.parseInt(uRecord[3]), Integer.parseInt(uRecord[4]));
        } catch (Exception e) {
            date = "0000-00-00 00:00:00";
        }
        String cat_id = "0";
        String sec_id = "0";
        Object ob = uCategories.get(uRecord[1]);
        if (ob != null) {
            cat_id = uRecord[1];
            sec_id = (String)ob;
        } else {
            if (uCategories.size() > 1) {
                cat_id = (String)uCategories.lastKey();
                sec_id = (String)uCategories.get(uCategories.lastKey());
            } else {
                cat_id = "0";
                sec_id = "0";
            }
        }
        String id_author = "1";
        try {
            Object obj = uUsers.get(uRecord[10]);
            id_author = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "1";
        } catch (Exception e) {
            id_author = "1";
        }

        String output = String.format("INSERT INTO `" + PREF + "news`"
            + " (`id`, `category_id`, `section_id`, `date`, `title`, `main`, `views`, `id_author`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], cat_id, sec_id, date, addslashes(uRecord[11]), addslashes(uRecord[13]), uRecord[16], id_author);
        return output;
    }

    /**
     * Work for publ.txt
     *
     * */
    private String parse_publ(String[] uRecord, TreeMap uUsers, TreeMap uCategories) {
        // May be incorect data
        if (uRecord[11] == null || uRecord[13] == null) return null;


        // Prepare variable which uses in SQL query
        String date = "0000-00-00 00:00:00";
        try {
            date = String.format("%04d-%02d-%02d 00:00:00", Integer.parseInt(uRecord[2]), Integer.parseInt(uRecord[3]), Integer.parseInt(uRecord[4]));
        } catch (Exception e) {
            date = "0000-00-00 00:00:00";
        }
        String cat_id = "0";
        String sec_id = "0";
        Object ob = uCategories.get(uRecord[1]);
        if (ob != null) {
            cat_id = uRecord[1];
            sec_id = (String)ob;
        } else {
            if (uCategories.size() > 1) {
                cat_id = (String)uCategories.lastKey();
                sec_id = (String)uCategories.get(uCategories.lastKey());
            } else {
                cat_id = "0";
                sec_id = "0";
            }
        }
        String id_author = "1";
        try {
            Object obj = uUsers.get(uRecord[10]);
            id_author = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "1";
        } catch (Exception e) {
            id_author = "1";
        }

        String output = String.format("INSERT INTO `" + PREF + "stat`"
            + " (`id`, `category_id`, `section_id`, `date`, `title`, `main`, `views`, `id_author`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], cat_id, sec_id, date, addslashes(uRecord[11]), addslashes(uRecord[13]), uRecord[16], id_author);
        return output;
    }

    /**
     * Work for users.txt
     *
     */
    private String parse_users(String[] uRecord, TreeMap uUsers, TreeMap uUsersMeta) {
        // Prepare variable which uses in SQL query
        String posts = "0";
        String status = "1";
        try {
        Object ob = uUsersMeta.get(uRecord[0]);
            if (ob != null) {
                String[] str = (String[])ob;
                posts = str[9];
                status = ((Integer.parseInt(str[2]) <= 4) ? str[2] : "1");
            } else {
                posts = "0";
                status = "1";
            }
        } catch (Exception e) {};
/*
        String puttime = "0000-00-00 00:00:00";
        try {
            puttime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(uRecord[15]) * 1000));
        } catch (Exception e) {
            puttime = "0000-00-00 00:00:00";
        }
*/
        String output = String.format("INSERT INTO `" + PREF + "users`"
            + " (`id`, `name`, `passw`, `email`, `url`, `icq`, `signature`, `puttime`, `posts`, `status`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uUsers.get(uRecord[0]), addslashes(uRecord[0]), addslashes(uRecord[2]), addslashes(uRecord[7]), addslashes(uRecord[8]), addslashes(uRecord[9]), addslashes(uRecord[13]), parseDate(uRecord[15]), posts, status);
//            uUsers.get(uRecord[0]), addslashes(uRecord[0]), addslashes(uRecord[2]), addslashes(uRecord[7]), addslashes(uRecord[8]), addslashes(uRecord[9]), addslashes(uRecord[13]), puttime, posts, status);
        return output;
    }

    public ArrayList getSQL(String[] uTables) {
        TreeMap uCategories = new TreeMap(); // $_SESSION['categories'] = array();
        TreeMap uUsers = getUserIds();

        TreeMap uUsersMeta = new TreeMap();
        String filename = DUMP_TABLES + "ugen.txt";
        File users = new File(filename);
        if (!users.exists()) {
            System.out.println("Can't find ugen.txt file");
            return null;
        }
        try {
            BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( filename ), "UTF-8" ) );
            String line = null;
            int id = 1;
            while ( ( line = br.readLine () ) != null ) {
                String [] uUsersData = line.split( "\\|" );
                if (uUsersData.length < 2 || uUsersData[1].isEmpty()) continue;
                uUsersMeta.put(uUsersData[1], uUsersData);
            }
        }
        catch (Exception e) {}

        // Geting ucoz files and work for each
        ArrayList FpsData = new ArrayList();
        for (int i = 0; i < uTables.length; i++) {
            String uDumpFile = DUMP_TABLES + uTables[i] + ".txt";
            File dump = new File(uDumpFile);
            if (!dump.exists()) continue;

            // Geting data from file and work for each record
            try {
                BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( uDumpFile ), "UTF-8" ) );
                String line = null;
                int id = 1;
                while ( ( line = br.readLine () ) != null ) {
                    String [] uRecord = line.split( "\\|" );
                    if (uRecord.length < 2) continue;
                    String sqlRecord = null;
                    if (uTables[i].equals("forum")) sqlRecord = parse_forum(uRecord, uUsers);
                    else if (uTables[i].equals("forump")) sqlRecord = parse_forump(uRecord, uUsers);
                    else if (uTables[i].equals("fr_fr")) sqlRecord = parse_fr_fr(uRecord, uUsers);
                    else if (uTables[i].equals("nw_nw")) sqlRecord = parse_nw_nw(uRecord, uUsers, uCategories);
                    else if (uTables[i].equals("pu_pu")) sqlRecord = parse_pu_pu(uRecord, uUsers, uCategories);
                    else if (uTables[i].equals("news")) sqlRecord = parse_news(uRecord, uUsers, uCategories);
                    else if (uTables[i].equals("publ")) sqlRecord = parse_publ(uRecord, uUsers, uCategories);
                    else if (uTables[i].equals("users")) sqlRecord = parse_users(uRecord, uUsers, uUsersMeta);
                    if (sqlRecord == null) continue;
                    FpsData.add(sqlRecord);
                }
            }
            catch (Exception e) {}
            FpsData.add("\r\n -- ---------------------------------- -- \r\n");
        }
        return FpsData;
    }
}

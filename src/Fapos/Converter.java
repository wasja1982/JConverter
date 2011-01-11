// Исправил багу с `description` в `forums` - OK
// Бага с $rec['message'] и $rec['main'] в parse_news - OK
// Проверить edittime для таблицы 'posts' - OK
// Категории новостей и статей общие? - OK

// Добавлено (0.2):
// 1) Генерация запросов на очистку таблиц. +
// 2) Поддержка вложений на форуме, в том числе обработка изображений. +
// 3) Устранен глюк с "последними сообщениями" на форуме. +
// 4) Удалены лишние пробелы. +
// 5) Аватарки. +

// Осталось реализовать:
// 1) Конвертация смайликов.
// 2) Каталог файлов.

// Добавлено (0.3)
// 1) Устранен небольшой баг с категориями (не правильная генерация секций). +
// 2) Нумерация пользователей совпадает с Ucoz. +
// 3) Интегрирована загрузка аватаров из сети Internet. +
// 4) Добавлен каталог файлов (не выводятся на главную). +
// 5) Переносится информация о наличии бана (в том числе из группы "Заблокированные" с ID=255). +
// 6) Устранена проблема с "последнем посещением" пользователей. +
// 7) Переноситься информация о неактивированных пользователях. +
// 8) Переработан алгоритм парсинга файлов бекапов (добавлена поддержка многострочных полей). +
// 9) Исправлен алгоритм работы со статьями (выводятся на главную). +
// 10) Исправлен алгоритм работы с новостями (выводятся на главную, категория "Новости"). +
// 11) Добавлено экспортирование блогов и FAQ (выводятся на главную, категория "Новости"). +
// 12) Добавлена конвертация смайлов (не соотвествует стандартному набору Fapos). +
// 13) Добавлена конвертация комментариев для блогов, новостей, статей и каталога файлов. +

//http://ozka-lemming.blogspot.com/2010/02/md5-java-messagedigest.html

package Fapos;

//import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.TreeMap;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.imageio.ImageIO;

public class Converter {
    private class uBlock {
        public int pos = 0;
        public int type = -1;

        public uBlock(int pos, int type) {
            this.pos = pos;
            this.type = type;
        }
    }

    private String DS = File.separator;
    // Dir with ucoz dump
    private String DUMP = "";
    // dir with txt files in ucoz dump
    private String DUMP_TABLES = "";
    private String ATTACH_TABLES = "";
    private String AVATAR_TABLES = "";
    private String LOADS_TABLES = "";
    // prefix for tables in data base, if exists
    private String PREF = "";

    public String PASSWORD = null;
    public boolean USE_WEB_AVATARS = false;
    public boolean NO_EMPTY = false;
    public boolean NO_IMAGE = false;
    public boolean PARSE_SMILE = false;

    private TreeMap uUsers = null;
    private TreeMap uUsersMeta = null;
    private ArrayList uAttachDir = null;

    public Converter(String DUMP) {
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
        ATTACH_TABLES = this.DUMP + "_fr" + DS;
        AVATAR_TABLES = this.DUMP + "avatar" + DS;
        LOADS_TABLES = this.DUMP + "_ld" + DS;
    }
    
    public Converter(String DUMP, String PREF) {
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
        ATTACH_TABLES = this.DUMP + "_fr" + DS;
        AVATAR_TABLES = this.DUMP + "avatar" + DS;
        LOADS_TABLES = this.DUMP + "_ld" + DS;
        this.PREF = PREF;
    }

    private String getMD5(String str) {
        MessageDigest md5;
        StringBuffer hexString = new StringBuffer();
        try {
            md5 = MessageDigest.getInstance("md5");
            md5.reset();
            md5.update(str.getBytes());
            byte messageDigest[] = md5.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            return e.toString();
        }
        return hexString.toString();
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

    private String attachesName(String post_id, String number, String date, String ext) {
        String parse = "000000000000";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            try {
                parse = new SimpleDateFormat("yyyyMMddHHmm").format(new Date(Long.parseLong(date) * 1000));
            } catch (Exception e) {
                parse = "000000000000";
            }
        }
        return String.format("%s-%s-%s%s", post_id, number, parse, ext);
    }

    private String loadsName(String name, String date) {
        String parse = "00000000000000";
        int pos = name.lastIndexOf('.');
        String ext = (pos >= 0) ? name.substring( pos ) : "";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            try {
                parse = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(Long.parseLong(date) * 1000));
            } catch (Exception e) {
                parse = "00000000000000";
            }
        }
        return String.format("%s-%s%s", getMD5(name), parse, ext);
    }

    private boolean copyFile( String filename, String new_filename ) {
        File file = new File( filename );
        if (file.exists()) {
            try {
                File new_file = new File( new_filename );
                FileChannel ic = new FileInputStream( file ).getChannel();
                FileChannel oc = new FileOutputStream( new_file ).getChannel();
                ic.transferTo(0, ic.size(), oc);
                ic.close();
                oc.close();
                new_file.setLastModified( file.lastModified() );
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private String addslashes(String data) {
        String str = HTMLtoBB(data).replace("\\", "\\\\");
//        String str = data.replace("\\", "\\\\");
//        str = str.replace("\"", "\\\"");
        str = str.replace("'", "\\'");
        return str;
    }

    private String toBB(String html) {
        String parse = html;
        int next = 0;
        do {
                next = parse.indexOf("<img ", next);
                if (next >= 0) {
                    int img_close = parse.indexOf(">", next);
                    if (img_close > 0 && img_close + 1 <= parse.length() &&
                        !parse.substring(img_close-1, img_close+1).equals("/>")) {
                        parse = parse.substring(0, img_close) + "/>" + parse.substring(img_close + 1);
                        next = img_close;
                    }
                }
                if (next >= 0) next++;
        } while (next >= 0 && next < parse.length());

        ParserHTML parser = new ParserHTML();
        try {
            parser.parseSmile = PARSE_SMILE;
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(parser);
            xr.setErrorHandler(parser);
            xr.parse(new InputSource(new StringReader( "<html>" + parse + "</html>" )));
            parse = parser.text;
        }
        catch (Exception e){
            parse = parser.text;
        }
        return parse;
    }

    private String HTMLtoBB(String html) {
        String parse = html;
        while (parse.contains( "  " )) {
            parse = parse.replace("  ", " ");
        }
        parse = parse.replace(" <p> ", "\r\n");
        parse = parse.replace(" <p>", "\r\n");
        parse = parse.replace("<p> ", "\r\n");
        parse = parse.replace("<p>", "\r\n");
        parse = parse.replace(" <br /> ", "\r\n");
        parse = parse.replace(" <br />", "\r\n");
        parse = parse.replace("<br /> ", "\r\n");
        parse = parse.replace("<br />", "\r\n");
        parse = parse.replace("<li>", "[*]");
        parse = parse.replace("</li>", "");

        parse = parse.replace("&nbsp;", " ");
        parse = parse.replace("&copy;", "©");
        parse = parse.replace("&reg;", "®");
        parse = parse.replace("&trade;", "™");

        parse = parse.replace("&#39;", "'");
        parse = parse.replace("&#41;", ")");
        parse = parse.replace("&#91;", "[");
        parse = parse.replace("&#92;", "\\");
        parse = parse.replace("&#96;", "`");
        parse = parse.replace("&#124;", "|");

        parse = parse.replace("&", "&amp;");

        if (!NO_IMAGE) {
            int i = 1;
            while (parse.contains( "<!--IMG" ) && parse.contains( "-->" )) {
                String str = String.format( "<!--IMG%d-->", i );
                if (parse.contains( str )) {
                    int start = parse.indexOf( str );
                    int end = parse.indexOf( str, start + 1 );
                    parse = parse.substring(0, start) + String.format("{IMAGE%d}", i) + parse.substring(end + str.length());
                }
                i++;
            }
        }
        String[] uBlockCodes = { "<!--uzquote-->", "<!--/uzquote-->", // [quote]Цитата из сообщения[/quote]
                                 "<!--uzcode-->", "<!--/uzcode-->", // [code]Код программы[/code]
                                 "<!--BBhide-->", "<!--/BBhide-->", // [hide]Any text goes here...[/hide]
                                 "<!--uSpoiler-->", "<!--/uSpoiler-->" // [spoiler]Any text goes here...[/spoiler]
                                };
        String[][] uBlocksText = {{"<!--uzq-->", "<!--/uzq-->"},
                                  {"<!--uzc-->", "<!--/uzc-->"},
                                  {"<span class=\"UhideBlock\">", "</span>"},
                                  {"<!--ust-->", "<!--/ust-->"}};
        String[] uBlocksBB = {"quote", "code", "hide", "spoiler"};

        Stack<uBlock> uBlocks = new Stack<uBlock>();
        int next = 0;
        do {
            int min_pos = parse.length();
            int min_type = -1;
            for (int i = 0; i < uBlockCodes.length; i++) {
                int pos = parse.indexOf(uBlockCodes[i], next);
                if (pos >= 0 && pos <= min_pos) {
                    min_pos = pos;
                    min_type = i;
                }
            }
            next = min_pos + 1;
            if (min_type >= 0) {
                if (min_type % 2 == 0) uBlocks.push(new uBlock(min_pos, min_type));
                else if (uBlocks.size() > 0 && uBlocks.peek().type + 1 == min_type) {
                    String block = parse.substring(uBlocks.peek().pos + uBlockCodes[uBlocks.peek().type].length(), min_pos);
                    String name = "";
                    String text = "";
                    int find_type = (min_type - 1) / 2;
                    if (find_type == 0) { // Quote
                        if (block.contains("<!--qn-->") && block.contains("<!--/qn-->"))
                            name = block.substring(block.indexOf("<!--qn-->") + 9, block.indexOf("<!--/qn-->"));
                    }
                    if (block.contains(uBlocksText[find_type][0]))
                        if (block.contains(uBlocksText[find_type][1]))
                            text = block.substring(block.indexOf(uBlocksText[find_type][0]) + uBlocksText[find_type][0].length(), block.lastIndexOf(uBlocksText[find_type][1]));
                        else
                            text = block.substring(block.indexOf(uBlocksText[find_type][0]) + uBlocksText[find_type][0].length());
                    
                    if (find_type != 1) text = toBB(text); // Code
                    block = "[" + uBlocksBB[find_type];
                    if (name != null & !name.isEmpty()) block += "=\"" + name + "\"";
                    block += "]" + text + "[/" + uBlocksBB[find_type] + "]";
                    parse = parse.substring(0, uBlocks.peek().pos) + block + parse.substring(min_pos + uBlockCodes[min_type].length());
                    next = uBlocks.peek().pos + block.length() + 1;
                    uBlocks.pop();
                }
            }
        } while (next >= 0 && next < parse.length());
        parse = parse.replace("&", "&amp;");
        parse = toBB(parse);
        parse = parse.replace("&amp;", "&");
        parse = parse.replace("&quot;", "\"");
        parse = parse.replace("&apos;", "'");
        parse = parse.replace("&lt;", "<");
        parse = parse.replace("&gt;", ">");
        return parse;
    }

    /**
     * forum.txt
     */
    private String parse_forum(String[] uRecord) {
        String id_author = "0";
        try {
            Object ob = uUsers.get(uRecord[10]);
            id_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "0";
        } catch (Exception e) {
            id_author = "0";
        }
        String id_last_author = "0";
        try {
            Object ob = uUsers.get(uRecord[12]);
            id_last_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "0";
        } catch (Exception e) {
            id_last_author = "0";
        }
        String output = String.format("INSERT INTO `" + PREF + "themes`"
            + " (`id`, `id_forum`, `important`, `id_last_author`, `last_post`, `locked`, `posts`, `views`, `title`, `id_author`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], uRecord[1], uRecord[3], id_last_author, parseDate(uRecord[4]), uRecord[5], uRecord[6], uRecord[7], addslashes(uRecord[8]), id_author);
        return output;
    }

    /**
     * forump.txt
     */
    private String parse_forump(String[] uRecord) {
        String id_author = "0";
        try {
            Object ob = uUsers.get(uRecord[6]);
            id_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "0";
        } catch (Exception e) {
            id_author = "0";
        }
        String attach = "0";
        String output_a = "";
        if (uRecord.length > 9 && uRecord[10] != null && !uRecord[10].isEmpty()) {
            String[] attaches = uRecord[10].split("`");
            attach = (attaches.length > 0) ? "1" : "0";
            if (uAttachDir.size() > 0) {
                for (int i = 0; i < attaches.length; i++) {
                    if (attaches[i].length() > 0) {
                        int pos = attaches[i].lastIndexOf('.');
                        String ext = (pos >= 0) ? attaches[i].substring( pos ) : "";
                        String is_image = (ext.equalsIgnoreCase( ".png" ) || ext.equalsIgnoreCase( ".jpg" ) ||
                                           ext.equalsIgnoreCase( ".gif" ) || ext.equalsIgnoreCase( ".jpeg" )) ? "1" : "0";
                        String new_filename = attachesName(uRecord[0], Integer.toString( i + 1 ), uRecord[2], ext);
                        boolean exist = false;
                        for (int j = 0; j < uAttachDir.size(); j++) {
                            String filename = "";
                            if (is_image.equals("1") && attaches[i].substring(0, 1).equalsIgnoreCase( "s" )) {
                                filename = ((String)uAttachDir.get(j)) + attaches[i].substring(1);
                            } else {
                                filename = ((String)uAttachDir.get(j)) + attaches[i];
                            }
                            if (copyFile(filename, "files" + DS + "forum" + DS + new_filename)) {
                                exist = true;
                                break;
                            }
                        }
                        if (exist) {
                            String sql = String.format("INSERT INTO `" + PREF + "forum_attaches`"
                                                     + " (`post_id`, `theme_id`, `user_id`, `attach_number`, `filename`, `size`, `date`, `is_image`) VALUES"
                                                     + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');\r\n",
                                                       uRecord[0], uRecord[1], id_author, Integer.toString( i + 1), new_filename,
                                                       Long.toString(new File("files" + DS + "forum" + DS + new_filename).length()),
                                                       parseDate(uRecord[2]), is_image);
                            output_a += sql;
                        } else {
                            System.out.println( "WARNING: Attachment \"" + attaches[i] + "\" not found." );
                        }
                    }
                }
            }
        }

        String output = String.format("INSERT INTO `" + PREF + "posts`"
            + " (`id`, `id_theme`, `time`, `message`, `id_author`, `edittime`, `attaches`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], uRecord[1], parseDate(uRecord[2]), addslashes(uRecord[4]), id_author, parseDate(uRecord[9]), attach);
        return output_a + output;
    }

    /**
     * fr_fr.txt
     */
    private String parse_fr_fr(String[] uRecord) {
        String output;
        if (uRecord[1] == null || uRecord[1].isEmpty() || uRecord[1].equals("0")) {
            output = String.format("INSERT INTO `" + PREF + "forum_cat`"
                + " (`id`, `title`) VALUES"
                + " ('%s', '%s');",
                uRecord[0], addslashes(uRecord[5]));
        } else {
            String last_theme_id = "";
            try {
                last_theme_id = ((uRecord[16] != null) && !uRecord[16].isEmpty()) ? uRecord[16] : "";
            } catch (Exception e) {
                last_theme_id = "";
            }
            String themes = "0";
            try {
                themes = ((uRecord[9] != null) && !uRecord[9].isEmpty()) ? uRecord[9] : "0";
            } catch (Exception e) {
                themes = "0";
            }
            String posts = "0";
            try {
                posts = ((uRecord[10] != null) && !uRecord[10].isEmpty()) ? uRecord[10] : "0";
            } catch (Exception e) {
                posts = "0";
            }
            output = String.format("INSERT INTO `" + PREF + "forums`"
                + " (`id`, `in_cat`, `title`, `last_theme_id`, `themes`, `posts`, `description`) VALUES"
                + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                uRecord[0], uRecord[1], addslashes(uRecord[5]), last_theme_id, themes, posts, addslashes(uRecord[6]));
        }
        return output;
    }

    /**
     * ld_ld.txt
     */
    private String parse_ld_ld(String[] uRecord) {
        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        String class_sections = "category";
        try {
            if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals( "0" )) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
        } catch (Exception e) {
            class_sections = "category";
        }

        String output = String.format("INSERT INTO `" + PREF + "loads_sections`"
            + " (`id`, `section_id`, `title`, `class`, `announce`, `view_on_home`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], section_id, addslashes(uRecord[5]), class_sections, addslashes(uRecord[6]), "0");
        return output;
    }

    /**
     * nw_nw.txt & bl_bl.txt & fq_fq.txt
     */
    private String parse_nw_nw(String[] uRecord, int mode) {
        int id = 3 + mode;
        try {
            id = (Integer.parseInt( uRecord[0] ) + 1) * 3 + mode;
        } catch (Exception e) {
            id = 3 + mode;
        }

        String output = String.format("INSERT INTO `" + PREF + "news_sections`"
            + " (`id`, `section_id`, `title`, `class`, `announce`, `view_on_home`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s');",
            id, mode, addslashes(uRecord[3]), "category", addslashes(uRecord[4]), "1");
        return output;
    }

    /**
     * pu_pu.txt
     */
    private String parse_pu_pu(String[] uRecord) {
        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        String class_sections = "category";
        try {
            if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals( "0" )) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
        } catch (Exception e) {
            class_sections = "category";
        }

        String output = String.format("INSERT INTO `" + PREF + "stat_sections`"
            + " (`id`, `section_id`, `title`, `class`, `view_on_home`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], section_id, addslashes(uRecord[5]), class_sections, "1");
        return output;
    }

    /**
     * loads.txt
     */
    private String parse_loads(String[] uRecord) {
        String download = "";
        if (uRecord[24] != null && !uRecord[24].isEmpty()) {
            String filename = String.format("%s_%s", uRecord[0], uRecord[24]);
            download = loadsName(uRecord[24], uRecord[5]);
            String path = ((Integer)(Integer.parseInt(uRecord[0]) / 100)).toString();
            if (!copyFile( LOADS_TABLES + path + DS + filename, "files" + DS + "load" + DS + download )) {
                System.out.println( "WARNING: File \"" + filename + "\" [load ID=" + uRecord[0] + "] not found." );
            }
        }
        String commented = "1";
        if (uRecord[7].equals("0")) {
            commented = "0";
        }
        String available = "0";
        if (uRecord[6].equals("0")) {
            available = "1";
        }
        String on_home_top = "0";
        if (uRecord[4].equals("1")) {
            on_home_top = "1";
        }
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[26]);
            author_id = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }

        String output = String.format("INSERT INTO `" + PREF + "loads`"
            + " (`id`, `title`, `main`, `author_id`, `category_id`,"
            + " `section_id`, `views`, `downloads`, `download`, `date`,"
            + " `comments`, `description`, `sourse`, `sourse_email`, `sourse_site`,"
            + " `commented`, `available`, `view_on_home`, `on_home_top`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], addslashes(uRecord[15]), addslashes(uRecord[32]), author_id, uRecord[2],
            uRecord[1], uRecord[13], uRecord[14], download, parseDate(uRecord[5]),
            uRecord[8], addslashes(uRecord[16]), uRecord[27], uRecord[28], uRecord[29],
            commented, available, "0", on_home_top);
        if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            output += String.format("INSERT INTO `" + PREF + "loads_add_content`"
                + " (`field_id`, `entity_id`, `content`) VALUES ('%s', '%s', '%s');",
                "1", uRecord[0], uRecord[22]);
        }
        return output;
    }

    /**
     * news.txt & blog.txt
     */
    private String parse_news(String[] uRecord, int mode) {
        int id = 0;
        try {
            id = (Integer.parseInt( uRecord[0] ) - 1) * 3 + mode;
        } catch (Exception e) {
            return null;
        }
        int category_id = 3 + mode;
        try {
            category_id = (Integer.parseInt( uRecord[1] ) + 1) * 3 + mode;
        } catch (Exception e) {
            category_id = 3 + mode;
        }
        if (category_id == 0) category_id = 1;
        String commented = "1";
        if (uRecord[7].equals("0")) {
            commented = "0";
        }
        String available = "0";
        if (uRecord[5].equals("0")) {
            available = "1";
        }
        String on_home_top = "0";
        if (uRecord[6].equals("1")) {
            on_home_top = "1";
        }
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[10]);
            author_id = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }

        String output = String.format("INSERT INTO `" + PREF + "news`"
            + " (`id`, `title`, `main`, `author_id`, `category_id`," 
            + " `section_id`, `views`, `date`, `comments`, `description`,"
            + " `commented`, `available`, `view_on_home`, `on_home_top`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            id, addslashes(uRecord[11]), addslashes(uRecord[13]), author_id, category_id,
            mode, uRecord[16], parseDate(uRecord[8]), uRecord[9], addslashes(uRecord[12]),
            commented, available, "1", on_home_top);
        return output;
    }

    /**
     * faq.txt
     */
    private String parse_faq(String[] uRecord) {
        int id = 0;
        try {
            id = (Integer.parseInt( uRecord[0] ) - 1) * 3 + 3;
        } catch (Exception e) {
            return null;
        }
        int category_id = 6;
        try {
            category_id = (Integer.parseInt( uRecord[1] ) + 1) * 3 + 3;
        } catch (Exception e) {
            category_id = 6;
        }
        String available = "0";
        if (uRecord[5].equals("0")) {
            available = "1";
        }
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[13]);
            author_id = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }

        String output = String.format("INSERT INTO `" + PREF + "news`"
            + " (`id`, `title`, `main`, `author_id`, `category_id`,"
            + " `section_id`, `date`, `description`, `sourse`, `sourse_email`,"
            + " `available`, `view_on_home`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            id, addslashes(uRecord[10]), addslashes(uRecord[12]), author_id, category_id,
            "3", parseDate(uRecord[4]), addslashes(uRecord[11]), uRecord[14], uRecord[15],
            available, "1");
        return output;
    }

    /**
     * publ.txt
     */
    private String parse_publ(String[] uRecord) {
        String commented = "1";
        if (uRecord[7].equals("0")) {
            commented = "0";
        }
        String available = "0";
        if (uRecord[6].equals("0")) {
            available = "1";
        }
        String on_home_top = "0";
        if (uRecord[4].equals("1")) {
            on_home_top = "1";
        }
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[15]);
            author_id = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }

        String output = String.format("INSERT INTO `" + PREF + "stat`"
            + " (`id`, `title`, `main`, `author_id`, `category_id`,"
            + " `section_id`, `views`, `date`, `comments`, `description`,"
            + " `sourse`, `sourse_email`, `sourse_site`, `commented`, `available`,"
            + " `view_on_home`, `on_home_top`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], addslashes(uRecord[13]), addslashes(uRecord[20]), author_id, uRecord[2],
            uRecord[1], uRecord[21], parseDate(uRecord[5]), uRecord[8], addslashes(uRecord[14]),
            uRecord[16], uRecord[17], uRecord[18], commented, available,
            "1", on_home_top);
        if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            output += String.format("INSERT INTO `" + PREF + "stat_add_content`"
                + " (`field_id`, `entity_id`, `content`) VALUES ('%s', '%s', '%s');",
                "1", uRecord[0], uRecord[19]);
        }
        return output;
    }

    /**
     * comments.txt
     */
    private String parse_comments(String[] uRecord) {
        String[] tableName = {null, "news_comments", "news_comments", "stat_comments", null, "loads_comments", null, null};
        String[] columnName = {null, "new_id", "new_id", "entity_id", null, "entity_id", null, null};
        int moduleID = 0;
        try {
            moduleID = Integer.parseInt( uRecord[1] );
        } catch (Exception e) {
            return null;
        }
        if (moduleID >= tableName.length || tableName[moduleID] == null) return null;
        int entity_id = 0;
        try {
            entity_id = Integer.parseInt( uRecord[2] );
        } catch (Exception e) {
            return null;
        }
        if (moduleID == 2) {
            entity_id = (entity_id - 1) * 3 + 2;
        } else if (moduleID == 3) {
            entity_id = (entity_id - 1) * 3 + 1;
        }
        String name = uRecord[5];
        if (name == null || name.isEmpty()) {
            name = uRecord[6];
        }

        String output = String.format("INSERT INTO `" + PREF + tableName[moduleID] + "`"
            + " (`" + columnName[moduleID] + "`, `name`, `message`, `ip`, `mail`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s');",
            entity_id, name, addslashes("[" + parseDate(uRecord[4]) + "]: " + uRecord[10]), uRecord[9], uRecord[7]);
        return output;
    }

    /**
     * users.txt
     */
    private String parse_users(String[] uRecord) {
        if (!uRecord[3].isEmpty() && !uRecord[3].equals( "0" )) {
            String[] path = uRecord[3].split( "/" );
            if (path.length > 1) {
                File file = new File( AVATAR_TABLES + path[path.length - 2] + DS + path[path.length - 1] );
                try {
                    BufferedImage imag = null;
                    if (file.exists()) {
                        imag = ImageIO.read( file );
                    } else if (USE_WEB_AVATARS) {
                        imag = ImageIO.read( new URL ((String)uRecord[3]) );
                    }
                    if (imag != null) {
                        if( imag.getColorModel().getTransparency() != Transparency.OPAQUE) {
                            int w = imag.getWidth();
                            int h = imag.getHeight();
                            BufferedImage image2 = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
                            image2.createGraphics().drawImage( imag, 0, 0, image2.getWidth(), image2.getHeight(), java.awt.Color.WHITE, null );
                            imag = image2;
                        }
                        File new_file = new File( "avatars" + DS + uUsers.get(uRecord[0]) + ".jpg" );
                        ImageIO.write( imag, "JPEG", new_file );
                        if (file.exists()) {
                            new_file.setLastModified( file.lastModified() );
                        }
                    } else {
                        System.out.println( "WARNING: File \"" + file.getName() + "\" not found. Avatar for user \"" + uRecord[0] + "\" not created." );
                    }
                } catch (Exception e) {
                    System.out.println( "WARNING: Avatar for user \"" + uRecord[0] + "\" not created." );
                }
            }
        }

        String posts = "0";
        String status = "1";
        String last_visit = "";
        String locked = "0";
        String activation = uRecord[23].equals("0") ? "" : uRecord[23];
        try {
        Object ob = uUsersMeta.get(uRecord[0]);
            if (ob != null) {
                String[] str = (String[])ob;
                posts = str[9];
                status = ((Integer.parseInt(str[2]) <= 4) ? str[2] : "1");
                last_visit = str[18];
                locked = ((Integer.parseInt(str[2]) == 255) ? "1" : str[3]);
            } else {
                posts = "0";
                status = "1";
            }
        } catch (Exception e) {};

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (uUsers.get(uRecord[0]).equals("1") && PASSWORD != null && !PASSWORD.isEmpty()) {
            uRecord[2] = getMD5(PASSWORD);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        String output = String.format("INSERT INTO `" + PREF + "users`"
            + " (`id`, `name`, `passw`, `email`, `url`, `icq`, `signature`, `puttime`, `last_visit`, `posts`, `status`, `locked`, `activation`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uUsers.get(uRecord[0]), addslashes(uRecord[0]), addslashes(uRecord[2]), addslashes(uRecord[7]), addslashes(uRecord[8]), addslashes(uRecord[9]), addslashes(uRecord[13]), parseDate(uRecord[15]), parseDate(last_visit), posts, status, locked, activation);
        return output;
    }

    public boolean initUsers() {
        System.out.println( "Load \"ugen.txt\"..." );
        String filename = DUMP_TABLES + "ugen.txt";
        if (!new File(filename).exists()) {
            System.out.println( "ERROR: File \"ugen.txt\" not found." );
            return false;
        }
        uUsersMeta = new TreeMap();
        uUsers = new TreeMap();
        try {
            BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( filename ), "UTF-8" ) );
            String line = null;
            while ( ( line = br.readLine () ) != null ) {
                line = line.replace("\\|", "&#124;");
                String [] uUsersData = line.split( "\\|" );
                if (uUsersData.length < 2 || uUsersData[1].isEmpty()) continue;
                for (int i = 0; i < uUsersData.length; i++) {
                    if (uUsersData[i].contains("&#124;")) {
                        uUsersData[i] = uUsersData[i].replace("&#124;", "|");
                    }
                }
                uUsersMeta.put(uUsersData[1], uUsersData);
                uUsers.put(uUsersData[1], uUsersData[0]);
            }
        }
        catch (Exception e) {}
        if (uUsersMeta.size() == 0) {
            System.out.println( "ERROR: File \"ugen.txt\" is empty." );
            return false;
        }
        return true;
    }

    public ArrayList getSQL(String[] uTables) {
        ArrayList FpsData = new ArrayList();
        ArrayList emptySql = new ArrayList();
        boolean forumEmpty = false;
        boolean loadsEmpty = false;
        boolean publEmpty = false;
        boolean newsEmpty = false;
        boolean addNews = false;
        boolean addBlog = false;
        boolean addFAQ = false;
        for (int i = 0; i < uTables.length; i++) {
            System.out.println( "Load \"" + uTables[i] + ".txt\"..." );
            String uDumpFile = DUMP_TABLES + uTables[i] + ".txt";
            if (!new File(uDumpFile).exists()) {
                System.out.println( "WARNING: File \"" + uTables[i] + ".txt\" not found." );
                continue;
            } else {
                if (uTables[i].equals("users")) {
                    if (!NO_EMPTY) {
                        emptySql.add( "TRUNCATE TABLE `" + PREF + "users`;" );
                    }
                    // Инициализация папки для работы с аватарами
                    try {
                        File outputAvatarsDir = new File( "avatars" );
                        if (outputAvatarsDir.exists()) {
                            if (!outputAvatarsDir.isDirectory()) {
                                System.out.println( "WARNING: Path \"avatars\" is not directory. Avatars not supported." );
                            }
                        } else {
                            try {
                                outputAvatarsDir.mkdirs();
                            } catch (Exception e) {
                                System.out.println( "WARNING: Path \"avatars\" can't created. Avatars not supported." );
                            }
                        }
                    } catch (Exception e) {}
                } else if (uTables[i].equals("fr_fr") || uTables[i].equals("forum") || uTables[i].equals("forump")) {
                    if (!forumEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "forum_cat`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "forums`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "themes`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "forum_attaches`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "posts`;" );
                        }
                        // Инициализация папок для работы с вложениями
                        uAttachDir = new ArrayList();
                        try {
                            File attachDir = new File(ATTACH_TABLES);
                            if (attachDir.exists()) {
                                String[] attach_cats = attachDir.list();
                                for (int j = 0; j < attach_cats.length; j++) {
                                    uAttachDir.add(ATTACH_TABLES + attach_cats[j] + DS);
                                }
                                File outputForumDir = new File( "files" + DS + "forum" );
                                if (outputForumDir.exists()) {
                                    if (!outputForumDir.isDirectory()) {
                                        System.out.println( "WARNING: Path \"files" + DS + "forum\" is not directory. Attachments not supported." );
                                        uAttachDir.clear();
                                    }
                                } else {
                                    try {
                                        outputForumDir.mkdirs();
                                    } catch (Exception e) {
                                        System.out.println( "WARNING: Path \"files" + DS + "forum\" can't created. Attachments not supported." );
                                        uAttachDir.clear();
                                    }
                                }
                            } else {
                                System.out.println( "WARNING: Path \"" + ATTACH_TABLES + "\" not found. Attachments not supported." );
                            }
                        } catch (Exception e) {}
                        forumEmpty =true;
                    }
                } else if (uTables[i].equals("ld_ld") || uTables[i].equals("loads")) {
                    if (!loadsEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "loads_sections`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "loads_add_fields`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "loads`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "loads_add_content`;" );
                        }
                        emptySql.add( "INSERT INTO `" + PREF + "loads_add_fields`"
                                    + " (`id`, `type`, `name`,`label`,`size`,`params`) VALUES"
                                    + " ('1', 'text', '', 'Ссылка для скачивания архива с другого сервера', '255', 'a:0:{}');" );
                        // Инициализация папки для работы с файлами
                        try {
                            File outputForumDir = new File( "files" + DS + "load" );
                            if (outputForumDir.exists()) {
                                if (!outputForumDir.isDirectory()) {
                                    System.out.println( "WARNING: Path \"files" + DS + "load\" is not directory. Loads not supported." );
                                }
                            } else {
                                try {
                                    outputForumDir.mkdirs();
                                } catch (Exception e) {
                                    System.out.println( "WARNING: Path \"files" + DS + "load\" can't created. Loads not supported." );
                                }
                            }
                        } catch (Exception e) {}
                        loadsEmpty = true;
                    }
                } else if (uTables[i].equals("pu_pu") || uTables[i].equals("publ")) {
                    if (!publEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "stat_sections`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "stat_add_fields`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "stat`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "stat_add_content`;" );
                        }
                        emptySql.add( "INSERT INTO `" + PREF + "stat_add_fields`"
                                    + " (`id`, `type`, `name`,`label`,`size`,`params`) VALUES"
                                    + " ('1', 'text', '', 'Ссылка на источник материала', '255', 'a:0:{}');" );
                        publEmpty = true;
                    }
                } else if (uTables[i].equals("nw_nw") || uTables[i].equals("bl_bl") || uTables[i].equals("fq_fq") ||
                           uTables[i].equals("news") || uTables[i].equals("blog") || uTables[i].equals("faq")) {
                    if (!newsEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "news`;" );
                            emptySql.add( "TRUNCATE TABLE `" + PREF + "news_sections`;" );
                        }
                        newsEmpty = true;
                    }
                    if (uTables[i].equals("nw_nw") || uTables[i].equals("news")) {
                        if (!addNews) {
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('1', '0', 'Новости', 'section');");
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('4', '1', 'Без категории', 'category');");
                            addNews = true;
                        }
                    } else if (uTables[i].equals("bl_bl") || uTables[i].equals("blog")) {
                        if (!addBlog) {
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('2', '0', 'Блоги', 'section');");
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('5', '2', 'Без категории', 'category');");
                            addBlog = true;
                        }
                    } else if (uTables[i].equals("fq_fq") || uTables[i].equals("faq")) {
                        if (!addFAQ) {
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('3', '0', 'FAQ', 'section');");
                            emptySql.add( "INSERT INTO `" + PREF + "news_sections`"
                                        + " (`id`, `section_id`, `title`, `class`) VALUES"
                                        + " ('6', '3', 'Без категории', 'category');");
                            addFAQ = true;
                        }
                    }
                } else if (uTables[i].equals("comments")) {
                    if (!NO_EMPTY) {
                        emptySql.add( "TRUNCATE TABLE `" + PREF + "loads_comments`;" );
                        emptySql.add( "TRUNCATE TABLE `" + PREF + "stat_comments`;" );
                        emptySql.add( "TRUNCATE TABLE `" + PREF + "news_comments`;" );
                    }
                }
            }

            try {
                BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( uDumpFile ), "UTF-8" ) );
                String line = null;
                String line_int = "";
                while ( ( line = br.readLine () ) != null ) {
                    if ( line.lastIndexOf("\\") == line.length() - 1 ) {
                        line_int += line.substring(0, line.length() - 1) + "<br />";
                        continue;
                    } else {
                        line_int += line;
                    }
                    if (uTables[i].equals("loads") || uTables[i].equals("publ") || uTables[i].equals("news") ||
                        uTables[i].equals("blog") || uTables[i].equals("faq")) {
                        line_int = line_int.replace("<!--IMG", "<!--I");
                    }

                    line_int = line_int.replace("\\|", "&#124;");
                    String [] uRecord = line_int.split( "\\|" );
                    if (uRecord.length < 2) continue;
                    for (int j = 0; j < uRecord.length; j++) {
                        if (uRecord[j].contains("&#124;")) {
                            uRecord[j] = uRecord[j].replace("&#124;", "|");
                        }
                    }
                    String sqlRecord = null;
                    if (uTables[i].equals("users")) sqlRecord = parse_users(uRecord);
                    else if (uTables[i].equals("fr_fr")) sqlRecord = parse_fr_fr(uRecord);
                    else if (uTables[i].equals("forum")) sqlRecord = parse_forum(uRecord);
                    else if (uTables[i].equals("forump")) sqlRecord = parse_forump(uRecord);
                    else if (uTables[i].equals("ld_ld")) sqlRecord = parse_ld_ld(uRecord);
                    else if (uTables[i].equals("loads")) sqlRecord = parse_loads(uRecord);
                    else if (uTables[i].equals("pu_pu")) sqlRecord = parse_pu_pu(uRecord);
                    else if (uTables[i].equals("publ")) sqlRecord = parse_publ(uRecord);
                    else if (uTables[i].equals("nw_nw")) sqlRecord = parse_nw_nw(uRecord, 1);
                    else if (uTables[i].equals("news")) sqlRecord = parse_news(uRecord, 1);
                    else if (uTables[i].equals("bl_bl")) sqlRecord = parse_nw_nw(uRecord, 2);
                    else if (uTables[i].equals("blog")) sqlRecord = parse_news(uRecord, 2);
                    else if (uTables[i].equals("fq_fq")) sqlRecord = parse_nw_nw(uRecord, 3);
                    else if (uTables[i].equals("faq")) sqlRecord = parse_faq(uRecord);
                    else if (uTables[i].equals("comments")) sqlRecord = parse_comments(uRecord);
                    line_int = "";
                    if (sqlRecord == null) continue;
                    FpsData.add(sqlRecord);
                }
            }
            catch (Exception e) {
                System.out.println( e.getMessage() );
            }
            FpsData.add("\r\n -- ---------------------------------- -- \r\n");
        }
        if (emptySql.size() > 0) {
            emptySql.add("\r\n -- ---------------------------------- -- \r\n");
            FpsData.addAll(0, emptySql);
        }
        return FpsData;
    }
}

package AtomM;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.imageio.ImageIO;
import org.xml.sax.SAXParseException;

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
    private String FORUM_ATTACH_TABLES = "";
    private String NEWS_ATTACH_TABLES = "";
    private String BLOG_ATTACH_TABLES = "";
    private String FAQ_ATTACH_TABLES = "";
    private String PUBL_ATTACH_TABLES = "";
    private String AVATAR_TABLES = "";
    private String LOADS_TABLES = "";
    // prefix for tables in data base, if exists
    private String PREF = "";

    public String PASSWORD = null;
    public boolean USE_WEB_AVATARS = false;
    public boolean NO_EMPTY = false;
    public boolean NO_IMAGE = false;
    public boolean PARSE_SMILE = false;
    public boolean NO_FIX = false;
    private String LOADS_OUT = "loads";

    public int VERSION = 0;

    private TreeMap uUsers = null;
    private TreeMap uUsersMeta = null;
    private TreeMap uThemes = null;
    private ArrayList uForumAttachDir = null;
    private ArrayList uNewsAttachDir = null;
    private ArrayList uBlogAttachDir = null;
    private ArrayList uFaqAttachDir = null;
    private ArrayList uStatAttachDir = null;

    public Converter(String DUMP) {
        if (DUMP != null) {
            DUMP = DUMP.replace("\\", DS).replace("/", DS);
            if (DUMP.length() > 0 && DUMP.lastIndexOf(DS) == DUMP.length() - 1) {
                DUMP = DUMP.substring(0, DUMP.length() - 1);
            }
        }
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
        FORUM_ATTACH_TABLES = this.DUMP + "_fr" + DS;
        PUBL_ATTACH_TABLES = this.DUMP + "_pu" + DS;
        NEWS_ATTACH_TABLES = this.DUMP + "_nw" + DS;
        BLOG_ATTACH_TABLES = this.DUMP + "_bl" + DS;
        FAQ_ATTACH_TABLES = this.DUMP + "_fq" + DS;
        AVATAR_TABLES = this.DUMP + "avatar" + DS;
        LOADS_TABLES = this.DUMP + "_ld" + DS;
    }

    public Converter(String DUMP, String PREF) {
        this(DUMP);
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

    private Date parseDate(String date) {
        Date parse = (date != null && !date.isEmpty() && !date.equals("0")) ? new Date(Long.parseLong(date) * 1000) : new Date();
        return parse;
    }

    private String parseDateToString(String date) {
        String parse = "0000-00-00 00:00:00";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            try {
                parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parseDate(date));
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
        String ext = (pos >= 0) ? name.substring(pos) : "";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            try {
                parse = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(Long.parseLong(date) * 1000));
            } catch (Exception e) {
                parse = "00000000000000";
            }
        }
        return String.format("%s-%s%s", getMD5(name), parse, ext);
    }

    private boolean copyFile(String filename, String new_filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                File new_file = new File(new_filename);
                FileChannel ic = new FileInputStream(file).getChannel();
                FileChannel oc = new FileOutputStream(new_file).getChannel();
                ic.transferTo(0, ic.size(), oc);
                ic.close();
                oc.close();
                new_file.setLastModified(file.lastModified());
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
        ParserHTML parser = new ParserHTML();
        try {
            parser.parseSmile = PARSE_SMILE;
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(parser);
            xr.setErrorHandler(parser);
            xr.parse(new InputSource(new StringReader("<html>" + parse + "</html>")));
            parse = parser.text;
        } catch (Exception e) {
            if (!NO_FIX && (e instanceof SAXParseException)) {
                int line = 1;
                int linestart = -6;
                for (int i = 0; i < parse.length(); i++) {
                    if (line == ((SAXParseException) e).getLineNumber()) {
                        try {
                            int pos = linestart + ((SAXParseException) e).getColumnNumber() - 1;
                            if (pos >= 0) {
                                int symbol = -1;
                                int tag = -1;
                                for (int j = pos; j >= 0; j--) {
                                    if (j < parse.length()) {
                                        if (parse.charAt(j) == '<' && tag < 0) {
                                            tag = j;
                                        }
                                        if (parse.charAt(j) == '&' && symbol < 0) {
                                            symbol = j;
                                        }
                                        if (tag >= 0 && symbol >= 0) {
                                            break;
                                        }
                                    }
                                }
                                // Сброс накопленных DIV
                                int start = 0;
                                if (tag > start) {
                                    start = tag;
                                }
                                if (symbol > start) {
                                    start = symbol;
                                }
                                int start_search = start;
                                String p = parse.toUpperCase();
                                while (parser.div != null && !parser.div.empty()) {
                                    int end_div = p.indexOf("</DIV>", start);
                                    if (end_div >= start_search) {
                                        int start_div = p.lastIndexOf("<DIV>", end_div);
                                        int start_div2 = p.lastIndexOf("<DIV ", end_div);
                                        if (start_div2 >= start_search && ((start_div >= start_search && start_div2 > start_div) || start_div < start_search)) {
                                            start_div = start_div2;
                                        }
                                        if (start_div < start_search) {
                                            // Замена
                                            String r = parse.substring(0, end_div);
                                            Integer align = (Integer) parser.div.pop();
                                            if (align != null) {
                                                if (align == 0) {
                                                    r += "[/right]";
                                                } else if (align == 1) {
                                                    r += "[/left]";
                                                } else if (align == 2) {
                                                    r += "[/center]";
                                                }
                                            }
                                            r += parse.substring(end_div + 6);
                                            parse = r;
                                            p = parse.toUpperCase();
                                            start = end_div + 1;
                                        } else {
                                            // Пропуск пары
                                            String r = p.substring(0, start_div);
                                            r += "_";
                                            r += p.substring(start_div + 1, end_div);
                                            r += "_";
                                            r += p.substring(end_div + 1);
                                            p = r;
                                        }
                                    } else {
                                        // Добавление в хвост
                                        Integer align = (Integer) parser.div.pop();
                                        if (align != null) {
                                            if (align == 0) {
                                                parse += "[/right]";
                                            } else if (align == 1) {
                                                parse += "[/left]";
                                            } else if (align == 2) {
                                                parse += "[/center]";
                                            }
                                        }
                                        start = parse.length();
                                    }
                                }
                                // Сброс накопленных SPAN
                                start = start_search;
                                p = parse.toUpperCase();
                                while (parser.span != null && !parser.span.empty()) {
                                    int end_span = p.indexOf("</SPAN>", start);
                                    if (end_span >= start_search) {
                                        int start_span = p.lastIndexOf("<SPAN>", end_span);
                                        int start_span2 = p.lastIndexOf("<SPAN ", end_span);
                                        if (start_span2 >= start_search && ((start_span >= start_search && start_span2 > start_span) || start_span < start_search)) {
                                            start_span = start_span2;
                                        }
                                        if (start_span < start_search) {
                                            // Замена
                                            String r = parse.substring(0, end_span);
                                            Integer style = (Integer) parser.span.pop();
                                            if (style != null) {
                                                if (style == 0) {
                                                    r += "[/color]";
                                                } else if (style == 1) {
                                                    r += "[/size]";
                                                }
                                            }
                                            r += parse.substring(end_span + 7);
                                            parse = r;
                                            p = parse.toUpperCase();
                                            start = end_span + 1;
                                        } else {
                                            // Пропуск пары
                                            String r = p.substring(0, start_span);
                                            r += "_";
                                            r += p.substring(start_span + 1, end_span);
                                            r += "_";
                                            r += p.substring(end_span + 1);
                                            p = r;
                                        }
                                    } else {
                                        // Добавление в хвост
                                        Integer style = (Integer) parser.span.pop();
                                        if (style != null) {
                                            if (style == 0) {
                                                parse += "[/color]";
                                            } else if (style == 1) {
                                                parse += "[/size]";
                                            }
                                        }
                                        start = parse.length();
                                    }
                                }

                                if (tag >= 0 || symbol >= 0) {
                                    String substring = "";
                                    if (tag > symbol) {
                                        if (parse.charAt(tag) == '<') {
                                            substring = "&lt;";
                                            if (tag < parse.length() - 1) {
                                                substring += parse.substring(tag + 1);
                                            }
                                        } else if (tag < parse.length()) {
                                            substring += parse.substring(tag);
                                        }
                                    } else {
                                        substring = "&amp;";
                                        if (symbol < parse.length() - 1) {
                                            substring += parse.substring(symbol + 1);
                                        }
                                    }
                                    parse = parser.text + toBB(substring);
                                }
                            }
                        } catch (Exception ee) {
                            //
                            ee.printStackTrace();
                        } finally {
                            break;
                        }
                    }
                    if (parse.startsWith("\n", i)) {
                        line++;
                        linestart = i;
                    }
                }
            }
        }
        return parse;
    }

    private String HTMLtoBB(String html) {
        String parse = html;
        while (parse.contains("  ")) {
            parse = parse.replace("  ", " ");
        }
        parse = parse.replaceAll("<(?i)b>", "[b]");
        parse = parse.replaceAll("</(?i)b>", "[/b]");
        parse = parse.replaceAll("<(?i)strong>", "[b]");
        parse = parse.replaceAll("</(?i)strong>", "[/b]");
        parse = parse.replaceAll("<(?i)i>", "[i]");
        parse = parse.replaceAll("</(?i)i>", "[/i]");
        parse = parse.replaceAll("<(?i)em>", "[i]");
        parse = parse.replaceAll("</(?i)em>", "[/i]");
        parse = parse.replaceAll("<(?i)u>", "[u]");
        parse = parse.replaceAll("</(?i)u>", "[/u]");
        parse = parse.replaceAll("<(?i)s>", "[s]");
        parse = parse.replaceAll("</(?i)s>", "[/s]");
        parse = parse.replaceAll("(\\s*)<(?i)p>(\\s*)</(?i)p>(\\s*)", "<br />");
        parse = parse.replaceAll("(\\s*)<(?i)p>(\\s*)", "<br />");
        parse = parse.replaceAll("(\\s*)</(?i)p>(\\s*)", "<br />");
        parse = parse.replaceAll("<(?i)br(\\s*)(/?)>", "<br />");
        parse = parse.replaceAll("<(?i)li>", "[*]");
        parse = parse.replaceAll("</(?i)li>", "");

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

        // Замена служебных символов, которые могут испортить парсинг
        for (byte i = 0; i < 32; i++) {
            // Пропуск переноса строки
            if (i == 10 || i == 13) {
                continue;
            }
            Character c = (char) i;
            if (parse.contains(c.toString())) {
                parse = parse.replace(c.toString(), "&#" + Byte.toString(i) + ";");
            }
        }

        if (!NO_IMAGE) {
            int i = 1;
            while (parse.contains("<!--IMG") && parse.contains("-->") && i <= 100) {
                String str = String.format("<!--IMG%d-->", i);
                while (parse.contains(str)) {
                    int start = parse.indexOf(str);
                    int end = parse.indexOf(str, start + 1);
                    if (start >= 0 && end <= start) {
                        parse = parse.replace(str, "");
                    } else {
                        parse = parse.substring(0, start) + String.format("{IMAGE%d}", i) + parse.substring(end + str.length());
                    }
                }
                i++;
            }
        }
        int next = 0;
        do {
            next = parse.indexOf("<img ", next);
            if (next >= 0) {
                int img_close = parse.indexOf(">", next);
                if (img_close > 0 && img_close + 1 <= parse.length() && !parse.substring(img_close - 1, img_close + 1).equals("/>")) {
                    parse = parse.substring(0, img_close) + "/>" + parse.substring(img_close + 1);
                    next = img_close;
                }
            }
            if (next >= 0) {
                next++;
            }
        } while (next >= 0 && next < parse.length());
        String[] uBlockCodes = {"<!--uzquote-->", "<!--/uzquote-->", // [quote]Цитата из сообщения[/quote]
            "<!--uzcode-->", "<!--/uzcode-->", // [code]Код программы[/code]
            "<!--BBhide-->", "<!--/BBhide-->", // [hide]Any text goes here...[/hide]
            "<!--uSpoiler-->", "<!--/uSpoiler-->", // [spoiler]Any text goes here...[/spoiler]
            "<!--BBvideo-->", "<!--/BBvideo-->", // [video]ссылка на страницу ютуб или рутуб[/video]
            "<!--BBaudio-->", "<!--/BBaudio-->" // [audio]ссылка на музыкальный файл[/audio]
    };
        String[][] uBlocksText = {{"<!--uzq-->", "<!--/uzq-->"},
        {"<!--uzc-->", "<!--/uzc-->"},
        {"<span class=\"UhideBlock\">", "</span>"},
        {"<!--ust-->", "<!--/ust-->"},
        {"_uVideoPlayer({'url':'", "'"},
        {"_uAudioPlayer({'url':'", "'"}};
        String[] uBlocksBB = {"quote", "code", "hide", "spoiler", "video", "audio"};

        Stack<uBlock> uBlocks = new Stack<uBlock>();
        next = 0;
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
                if (min_type % 2 == 0) {
                    uBlocks.push(new uBlock(min_pos, min_type));
                } else if (uBlocks.size() > 0 && uBlocks.peek().type + 1 == min_type) {
                    String block = parse.substring(uBlocks.peek().pos + uBlockCodes[uBlocks.peek().type].length(), min_pos);
                    String name = "";
                    String text = "";
                    int find_type = (min_type - 1) / 2;
                    if (find_type == 0) { // Quote
                        if (block.contains("<!--qn-->") && block.contains("<!--/qn-->")) {
                            name = block.substring(block.indexOf("<!--qn-->") + 9, block.indexOf("<!--/qn-->"));
                        }
                    }
                    if (block.contains(uBlocksText[find_type][0])) {
                        if (block.contains(uBlocksText[find_type][1])) {
                            if (find_type == 4 || find_type == 5) { // Video & Audio
                                int pos = block.indexOf(uBlocksText[find_type][0]) + uBlocksText[find_type][0].length();
                                text = block.substring(pos, block.indexOf(uBlocksText[find_type][1], pos));
                            } else {
                                text = block.substring(block.indexOf(uBlocksText[find_type][0]) + uBlocksText[find_type][0].length(), block.lastIndexOf(uBlocksText[find_type][1]));
                            }
                        } else {
                            text = block.substring(block.indexOf(uBlocksText[find_type][0]) + uBlocksText[find_type][0].length());
                        }
                    }

                    if (find_type != 1) {
                        text = toBB(text); // Code
                    }
                    block = "[" + uBlocksBB[find_type];
                    if (name != null & !name.isEmpty()) {
                        block += "=\"" + name + "\"";
                    }
                    block += "]" + text + "[/" + uBlocksBB[find_type] + "]";
                    parse = parse.substring(0, uBlocks.peek().pos) + block + parse.substring(min_pos + uBlockCodes[min_type].length());
                    next = uBlocks.peek().pos + block.length();
                    uBlocks.pop();
                }
            }
        } while (next >= 0 && next < parse.length());
        parse = parse.replace("&", "&amp;");

        parse = toBB(parse);

        // Восстановдение служебных символов
        int start = parse.indexOf("&#");
        int end = parse.indexOf(";", start);
        while (start >= 0 && end >= 0 && end > start) {
            try {
                int symb = Integer.parseInt(parse.substring(start + 2, end));
                Character c = (char) symb;
                parse = parse.replace(parse.substring(start, end + 1), c.toString());
                start = parse.indexOf("&#", start);
                end = parse.indexOf(";", start);
            } catch (Exception e) {
                start = parse.indexOf("&#", start + 1);
                end = parse.indexOf(";", start);
            }
        }

        parse = parse.replace("&amp;", "&");
        parse = parse.replace("&quot;", "\"");
        parse = parse.replace("&apos;", "'");
        parse = parse.replace("&lt;", "<");
        parse = parse.replace("&gt;", ">");
        parse = parse.replaceAll("<(?i)br(\\s*)(/?)>", "\r\n");
        return parse;
    }

    /**
     * forum.txt
     */
    private boolean parse_forum(List FpsData, String[] uRecord) {
        if (uRecord.length < 13) {
            return false;
        }
        String id_author = "0";
        try {
            Object ob = uUsers.get(uRecord[10]);
            id_author = ((ob != null) && !((String) ob).isEmpty()) ? (String) ob : "0";
        } catch (Exception e) {
            id_author = "0";
        }
        String id_last_author = "0";
        try {
            Object ob = uUsers.get(uRecord[12]);
            id_last_author = ((ob != null) && !((String) ob).isEmpty()) ? (String) ob : "0";
        } catch (Exception e) {
            id_last_author = "0";
        }
        InsertQuery query = new InsertQuery(PREF + "themes");
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("id_forum", uRecord[1]));
        query.addItem(new QueryItem("important", uRecord[3]));
        query.addItem(new QueryItem("id_last_author", id_last_author));
        query.addItem(new QueryItem("last_post", parseDate(uRecord[4])));
        query.addItem(new QueryItem("locked", uRecord[5]));
        query.addItem(new QueryItem("posts", uRecord[6]));
        query.addItem(new QueryItem("views", uRecord[7]));
        query.addItem(new QueryItem("title", addslashes(uRecord[8])));
        query.addItem(new QueryItem("id_author", id_author));
        if (VERSION > 0) {
            query.addItem(new QueryItem("description", addslashes(uRecord[9])));
        }
        if (VERSION >= 10) { // AtomM 4 и новее
            uThemes.put(uRecord[0], uRecord[1]);
        }
        FpsData.add(query);
        return true;
    }

    /**
     * forump.txt
     */
    private boolean parse_forump(List FpsData, String[] uRecord) {
        if (uRecord.length < 11) {
            return false;
        }
        String id_author = "0";
        try {
            Object ob = uUsers.get(uRecord[6]);
            id_author = ((ob != null) && !((String) ob).isEmpty()) ? (String) ob : "0";
        } catch (Exception e) {
            id_author = "0";
        }
        String attach = "0";
        if (uRecord.length > 10 && uRecord[10] != null && !uRecord[10].isEmpty()) {
            String[] attaches = uRecord[10].split("`");
            attach = (attaches.length > 0) ? "1" : "0";
            if (uForumAttachDir != null && uForumAttachDir.size() > 0) {
                for (int i = 0; i < attaches.length; i++) {
                    if (attaches[i].length() > 0) {
                        int pos = attaches[i].lastIndexOf('.');
                        String ext = (pos >= 0) ? attaches[i].substring(pos) : "";
                        String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                                || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                        String new_filename = attachesName(uRecord[0], Integer.toString(i + 1), uRecord[2], ext);
                        boolean exist = false;
                        for (int j = 0; j < uForumAttachDir.size(); j++) {
                            String filename = "";
                            if (is_image.equals("1") && attaches[i].substring(0, 1).equalsIgnoreCase("s")) {
                                filename = ((String) uForumAttachDir.get(j)) + attaches[i].substring(1);
                            } else {
                                filename = ((String) uForumAttachDir.get(j)) + attaches[i];
                            }
                            if (copyFile(filename, "files" + DS + "forum" + DS + new_filename)) {
                                exist = true;
                                break;
                            }
                        }
                        if (exist) {
                            InsertQuery query_add = new InsertQuery(PREF + "forum_attaches");
                            query_add.addItem(new QueryItem("post_id", uRecord[0]));
                            query_add.addItem(new QueryItem("theme_id", uRecord[1]));
                            query_add.addItem(new QueryItem("user_id", id_author));
                            query_add.addItem(new QueryItem("attach_number", i + 1));
                            query_add.addItem(new QueryItem("filename", new_filename));
                            query_add.addItem(new QueryItem("size", new File("files" + DS + "forum" + DS + new_filename).length()));
                            query_add.addItem(new QueryItem("date", parseDate(uRecord[2])));
                            query_add.addItem(new QueryItem("is_image", is_image));
                            FpsData.add(query_add);
                        } else {
                            System.out.println("WARNING: Attachment \"" + attaches[i] + "\" not found.");
                        }
                    }
                }
            }
        }

        InsertQuery query = new InsertQuery(PREF + "posts");
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("id_theme", uRecord[1]));
        query.addItem(new QueryItem("time", parseDate(uRecord[2])));
        query.addItem(new QueryItem("message", addslashes(uRecord[4])));
        query.addItem(new QueryItem("id_author", id_author));
        query.addItem(new QueryItem("edittime", parseDate(uRecord[9])));
        query.addItem(new QueryItem("attaches", attach));
        if (VERSION >= 10 && uThemes != null && uThemes.containsKey(uRecord[1])) { // AtomM 4 и новее
            query.addItem(new QueryItem("id_forum", (String)uThemes.get(uRecord[1])));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * fr_fr.txt
     */
    private boolean parse_fr_fr(List FpsData, String[] uRecord) {
        if (uRecord.length < 6) {
            return false;
        }
        InsertQuery query = new InsertQuery(PREF + "forums");
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("title", addslashes(uRecord[5])));
        if (uRecord[1] == null || uRecord[1].isEmpty() || uRecord[1].equals("0")) {
            query.setTable(PREF + "forum_cat");
        } else {
            if (uRecord.length < 17) {
                return false;
            }
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
            query.addItem(new QueryItem("in_cat", uRecord[1]));
            query.addItem(new QueryItem("last_theme_id", last_theme_id));
            query.addItem(new QueryItem("themes", themes));
            query.addItem(new QueryItem("posts", posts));
            query.addItem(new QueryItem("description", addslashes(uRecord[6])));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * ld_ld.txt
     */
    private boolean parse_ld_ld(List FpsData, String[] uRecord) {
        if (uRecord.length < 7) {
            return false;
        }
        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "loads_categories" : "loads_sections"));
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("title", addslashes(uRecord[5])));
        query.addItem(new QueryItem("announce", addslashes(uRecord[6])));
        query.addItem(new QueryItem("view_on_home", "0"));
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem(new QueryItem("parent_id", section_id));
            query.addItem(new QueryItem("no_access", ""));
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem(new QueryItem("path", (section_id.equals("0") ? "" : section_id + ".")));
            }
        } else { // Старше 1.2 beta
            String class_sections = "category";
            try {
                if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals("0")) {
                    class_sections = "category";
                } else {
                    section_id = "0";
                    class_sections = "section";
                }
            } catch (Exception e) {
                class_sections = "category";
            }
            query.addItem(new QueryItem("section_id", section_id));
            query.addItem(new QueryItem("class", class_sections));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * nw_nw.txt & bl_bl.txt & fq_fq.txt
     */
    private boolean parse_nw_nw(List FpsData, String[] uRecord, int mode) {
        if (uRecord.length < 5) {
            return false;
        }
        int id = 3 + mode;
        try {
            id = (Integer.parseInt(uRecord[0]) + 1) * 3 + mode;
        } catch (Exception e) {
            id = 3 + mode;
        }
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
        query.addItem(new QueryItem("id", id));
        query.addItem(new QueryItem("title", addslashes(uRecord[3])));
        query.addItem(new QueryItem("announce", addslashes(uRecord[4])));
        query.addItem(new QueryItem("view_on_home", "1"));
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem(new QueryItem("parent_id", mode));
            query.addItem(new QueryItem("no_access", ""));
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem(new QueryItem("path", mode + "."));
            }
        } else { // Старше 1.2 beta
            query.addItem(new QueryItem("section_id", mode));
            query.addItem(new QueryItem("class", "category"));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * pu_pu.txt
     */
    private boolean parse_pu_pu(List FpsData, String[] uRecord) {
        if (uRecord.length < 6) {
            return false;
        }
        String section_id = "0";
        try {
            section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        } catch (Exception e) {
            section_id = "0";
        }
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "stat_categories" : "stat_sections"));
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("title", addslashes(uRecord[5])));
        query.addItem(new QueryItem("view_on_home", "1"));
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem(new QueryItem("parent_id", section_id));
            query.addItem(new QueryItem("no_access", ""));
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem(new QueryItem("path", (section_id.equals("0") ? "" : section_id + ".")));
            }
        } else { // Старше 1.2 beta
            String class_sections = "category";
            try {
                if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals("0")) {
                    class_sections = "category";
                } else {
                    section_id = "0";
                    class_sections = "section";
                }
            } catch (Exception e) {
                class_sections = "category";
            }
            query.addItem(new QueryItem("section_id", section_id));
            query.addItem(new QueryItem("class", class_sections));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * loads.txt
     */
    private boolean parse_loads(List FpsData, String[] uRecord) {
        if (uRecord.length < 33) {
            return false;
        }
        String download = "";
        if (uRecord[24] != null && !uRecord[24].isEmpty()) {
            String filename = String.format("%s_%s", uRecord[0], uRecord[24]);
            download = loadsName(uRecord[24], uRecord[5]);
            String path = ((Integer) (Integer.parseInt(uRecord[0]) / 100)).toString();
            if (!copyFile(LOADS_TABLES + path + DS + filename, "files" + DS + LOADS_OUT + DS + download)) {
                System.out.println("WARNING: File \"" + filename + "\" [load ID=" + uRecord[0] + "] not found.");
            }
        }
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[6].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[4].equals("1")) ? "1" : "0";
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[26]);
            author_id = ((obj != null) && !((String) obj).isEmpty()) ? (String) obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }

        InsertQuery query = new InsertQuery(PREF + "loads");
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("title", addslashes(uRecord[15])));
        query.addItem(new QueryItem("main", addslashes(uRecord[32])));
        query.addItem(new QueryItem("author_id", author_id));
        query.addItem(new QueryItem("category_id", uRecord[2]));
        query.addItem(new QueryItem("views", uRecord[13]));
        query.addItem(new QueryItem("downloads", uRecord[14]));
        query.addItem(new QueryItem("download", download));
        query.addItem(new QueryItem("date", parseDate(uRecord[5])));
        query.addItem(new QueryItem("comments", uRecord[8]));
        query.addItem(new QueryItem("description", addslashes(uRecord[16])));
        query.addItem(new QueryItem("sourse", uRecord[27]));
        query.addItem(new QueryItem("sourse_email", uRecord[28]));
        query.addItem(new QueryItem("sourse_site", uRecord[29]));
        query.addItem(new QueryItem("commented", commented));
        query.addItem(new QueryItem("available", available));
        query.addItem(new QueryItem("view_on_home", "0"));
        query.addItem(new QueryItem("on_home_top", on_home_top));
        if (VERSION > 3) { // 1.2 beta и новее
            long lsize = 0;
            try {
                lsize = Long.parseLong(uRecord[23]);
            } catch (NumberFormatException ex) {
                lsize = 0;
            }
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem(new QueryItem("premoder", "confirmed"));
            }
            query.addItem(new QueryItem("download_url", uRecord[22]));
            query.addItem(new QueryItem("download_url_size", lsize));
        } else { // Старше 1.2 beta
            query.addItem(new QueryItem("section_id", uRecord[1]));
            if (uRecord[22] != null && !uRecord[22].isEmpty()) {
                InsertQuery query_add = new InsertQuery(PREF + "loads_add_content");
                query_add.addItem(new QueryItem("field_id", "1"));
                query_add.addItem(new QueryItem("entity_id", uRecord[0]));
                query_add.addItem(new QueryItem("content", uRecord[22]));
                FpsData.add(query_add);
            }
        }
        FpsData.add(query);
        return true;
    }

    /**
     * news.txt & blog.txt
     */
    private boolean parse_news(List FpsData, String[] uRecord, int mode) {
        if (uRecord.length < 17) {
            return false;
        }
        int id = 0;
        try {
            id = (Integer.parseInt(uRecord[0]) - 1) * 3 + mode;
        } catch (Exception e) {
            return false;
        }
        int category_id = 3 + mode;
        try {
            category_id = (Integer.parseInt(uRecord[1]) + 1) * 3 + mode;
        } catch (Exception e) {
            category_id = 3 + mode;
        }
        if (category_id == 0) {
            category_id = 1;
        }
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[5].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[6].equals("1")) ? "1" : "0";
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[10]);
            author_id = ((obj != null) && !((String) obj).isEmpty()) ? (String) obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }
        InsertQuery query = new InsertQuery(PREF + "news");
        query.addItem(new QueryItem("id", id));
        query.addItem(new QueryItem("title", addslashes(uRecord[11])));
        query.addItem(new QueryItem("main", addslashes(uRecord[13])));
        query.addItem(new QueryItem("author_id", author_id));
        query.addItem(new QueryItem("category_id", category_id));
        query.addItem(new QueryItem("views", uRecord[16]));
        query.addItem(new QueryItem("date", parseDate(uRecord[8])));
        query.addItem(new QueryItem("comments", uRecord[9]));
        query.addItem(new QueryItem("description", addslashes(uRecord[12])));
        query.addItem(new QueryItem("commented", commented));
        query.addItem(new QueryItem("available", available));
        query.addItem(new QueryItem("view_on_home", "1"));
        query.addItem(new QueryItem("on_home_top", on_home_top));
        if (VERSION > 3) { // 1.2 beta и новее
            String[] files = uRecord[15].split("\\|");
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i] != null && !files[i].isEmpty()) {
                        String[] parts = files[i].split("`");
                        ArrayList<String> attachDir = (mode == 1 ? uNewsAttachDir : (mode == 2 ? uBlogAttachDir : null));
                        if (attachDir != null && attachDir.size() > 0 && parts.length > 1) {
                            String ext = "." + parts[1];
                            String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                                    || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                            String new_filename = attachesName(Integer.toString(id), Integer.toString(i + 1), uRecord[8], ext);
                            boolean exist = false;
                            for (int j = 0; j < attachDir.size(); j++) {
                                String filename = ((String) attachDir.get(j)) + parts[0] + ext;
                                if (copyFile(filename, "files" + DS + "news" + DS + new_filename)) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (exist) {
                                InsertQuery query_add = new InsertQuery(PREF + "news_attaches");
                                query_add.addItem(new QueryItem("entity_id", id));
                                query_add.addItem(new QueryItem("user_id", author_id));
                                query_add.addItem(new QueryItem("attach_number", i + 1));
                                query_add.addItem(new QueryItem("filename", new_filename));
                                query_add.addItem(new QueryItem("size", new File("files" + DS + "news" + DS + new_filename).length()));
                                query_add.addItem(new QueryItem("date", parseDate(uRecord[8])));
                                query_add.addItem(new QueryItem("is_image", is_image));
                                FpsData.add(query_add);
                            } else {
                                System.out.println("WARNING: Attachment \"" + parts[0] + ext + "\" not found.");
                            }
                        }
                    }
                }
            }
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem(new QueryItem("premoder", "confirmed"));
            }
        } else { // Старше 1.2 beta
            query.addItem(new QueryItem("section_id", mode));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * faq.txt
     */
    private boolean parse_faq(List FpsData, String[] uRecord) {
        if (uRecord.length < 18) {
            return false;
        }
        int id = 0;
        try {
            id = (Integer.parseInt(uRecord[0]) - 1) * 3 + 3;
        } catch (Exception e) {
            return false;
        }
        int category_id = 6;
        try {
            category_id = (Integer.parseInt(uRecord[1]) + 1) * 3 + 3;
        } catch (Exception e) {
            category_id = 6;
        }
        String available = (uRecord[5].equals("0")) ? "1" : "0";
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[13]);
            author_id = ((obj != null) && !((String) obj).isEmpty()) ? (String) obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }
        InsertQuery query = new InsertQuery(PREF + "news");
        query.addItem(new QueryItem("id", id));
        query.addItem(new QueryItem("title", addslashes(uRecord[10])));
        query.addItem(new QueryItem("main", addslashes(uRecord[12])));
        query.addItem(new QueryItem("author_id", author_id));
        query.addItem(new QueryItem("category_id", category_id));
        query.addItem(new QueryItem("date", parseDate(uRecord[4])));
        query.addItem(new QueryItem("description", addslashes(uRecord[11])));
        query.addItem(new QueryItem("sourse", uRecord[14]));
        query.addItem(new QueryItem("sourse_email", uRecord[15]));
        query.addItem(new QueryItem("available", available));
        query.addItem(new QueryItem("view_on_home", "1"));
        if (VERSION > 3) { // 1.2 beta и новее
            String[] files = uRecord[17].split("\\|");
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i] != null && !files[i].isEmpty()) {
                        String[] parts = files[i].split("`");
                        if (uFaqAttachDir != null && uFaqAttachDir.size() > 0 && parts.length > 1) {
                            String ext = "." + parts[1];
                            String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                                    || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                            String new_filename = attachesName(Integer.toString(id), Integer.toString(i + 1), uRecord[4], ext);
                            boolean exist = false;
                            for (int j = 0; j < uFaqAttachDir.size(); j++) {
                                String filename = ((String) uFaqAttachDir.get(j)) + parts[0] + ext;
                                if (copyFile(filename, "files" + DS + "news" + DS + new_filename)) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (exist) {
                                InsertQuery query_add = new InsertQuery(PREF + "news_attaches");
                                query_add.addItem(new QueryItem("entity_id", id));
                                query_add.addItem(new QueryItem("user_id", author_id));
                                query_add.addItem(new QueryItem("attach_number", i + 1));
                                query_add.addItem(new QueryItem("filename", new_filename));
                                query_add.addItem(new QueryItem("size", new File("files" + DS + "news" + DS + new_filename).length()));
                                query_add.addItem(new QueryItem("date", parseDate(uRecord[4])));
                                query_add.addItem(new QueryItem("is_image", is_image));
                                FpsData.add(query_add);
                            } else {
                                System.out.println("WARNING: Attachment \"" + parts[0] + ext + "\" not found.");
                            }
                        }
                    }
                }
            }
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem(new QueryItem("premoder", "confirmed"));
            }
        } else { // Старше 1.2 beta
            query.addItem(new QueryItem("section_id", "3"));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * publ.txt
     */
    private boolean parse_publ(List FpsData, String[] uRecord) {
        if (uRecord.length < 25) {
            return false;
        }
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[6].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[4].equals("1")) ? "1" : "0";
        String author_id = "0";
        try {
            Object obj = uUsers.get(uRecord[15]);
            author_id = ((obj != null) && !((String) obj).isEmpty()) ? (String) obj : "0";
        } catch (Exception e) {
            author_id = "0";
        }
        InsertQuery query = new InsertQuery(PREF + "stat");
        query.addItem(new QueryItem("id", uRecord[0]));
        query.addItem(new QueryItem("title", addslashes(uRecord[13])));
        query.addItem(new QueryItem("main", addslashes(uRecord[20])));
        query.addItem(new QueryItem("author_id", author_id));
        query.addItem(new QueryItem("category_id", uRecord[2]));
        query.addItem(new QueryItem("views", uRecord[21]));
        query.addItem(new QueryItem("date", parseDate(uRecord[5])));
        query.addItem(new QueryItem("comments", uRecord[8]));
        query.addItem(new QueryItem("description", addslashes(uRecord[14])));
        query.addItem(new QueryItem("sourse", uRecord[16]));
        query.addItem(new QueryItem("sourse_email", uRecord[17]));
        query.addItem(new QueryItem("sourse_site", uRecord[18]));
        query.addItem(new QueryItem("commented", commented));
        query.addItem(new QueryItem("available", available));
        query.addItem(new QueryItem("view_on_home", "1"));
        query.addItem(new QueryItem("on_home_top", on_home_top));
        if (VERSION > 3) { // 1.2 beta и новее
            String[] files = uRecord[24].split("\\|");
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i] != null && !files[i].isEmpty()) {
                        String[] parts = files[i].split("`");
                        if (uStatAttachDir != null && uStatAttachDir.size() > 0 && parts.length > 1) {
                            String ext = "." + parts[1];
                            String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                                    || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                            String new_filename = attachesName(uRecord[0], Integer.toString(i + 1), uRecord[5], ext);
                            boolean exist = false;
                            for (int j = 0; j < uStatAttachDir.size(); j++) {
                                String filename = ((String) uStatAttachDir.get(j)) + parts[0] + ext;
                                if (copyFile(filename, "files" + DS + "stat" + DS + new_filename)) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (exist) {
                                InsertQuery query_add = new InsertQuery(PREF + "stat_attaches");
                                query_add.addItem(new QueryItem("entity_id", uRecord[0]));
                                query_add.addItem(new QueryItem("user_id", author_id));
                                query_add.addItem(new QueryItem("attach_number", i + 1));
                                query_add.addItem(new QueryItem("filename", new_filename));
                                query_add.addItem(new QueryItem("size", new File("files" + DS + "stat" + DS + new_filename).length()));
                                query_add.addItem(new QueryItem("date", parseDate(uRecord[5])));
                                query_add.addItem(new QueryItem("is_image", is_image));
                                FpsData.add(query_add);
                            } else {
                                System.out.println("WARNING: Attachment \"" + parts[0] + ext + "\" not found.");
                            }
                        }
                    }
                }
            }
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem(new QueryItem("premoder", "confirmed"));
            }
        } else { // Старше 1.2 beta
            query.addItem(new QueryItem("section_id", uRecord[1]));
        }
        if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            if (VERSION < 10) { // Старше AtomM 4
                InsertQuery query_add = new InsertQuery(PREF + "stat_add_content");
                query_add.addItem(new QueryItem("field_id", "1"));
                query_add.addItem(new QueryItem("entity_id", uRecord[0]));
                query_add.addItem(new QueryItem("content", uRecord[19]));
                FpsData.add(query_add);
            } else { // AtomM 4 и новее
                query.addItem(new QueryItem("add_field_1", uRecord[19]));
            }
        }
        FpsData.add(query);
        return true;
    }

    /**
     * comments.txt
     */
    private boolean parse_comments(List FpsData, String[] uRecord) {
        if (uRecord.length < 11) {
            return false;
        }
        String[] moduleName = {null, "news", "news", "stat", "foto", "loads", null, null};
        String[] tableName = {null, "news_comments", "news_comments", "stat_comments", null, "loads_comments", null, null};
        String[] columnName = {null, "new_id", "new_id", "entity_id", null, "entity_id", null, null};
        int moduleID = 0;
        try {
            moduleID = Integer.parseInt(uRecord[1]);
        } catch (Exception e) {
            return false;
        }
        if (VERSION < 6 && (moduleID >= tableName.length || tableName[moduleID] == null)) {
            return false;
        }
        if (VERSION >= 6 && (moduleID >= moduleName.length || moduleName[moduleID] == null)) {
            return false;
        }
        int entity_id = 0;
        try {
            entity_id = Integer.parseInt(uRecord[2]);
        } catch (Exception e) {
            return false;
        }
        if (moduleID == 2) {
            entity_id = (entity_id - 1) * 3 + 2;
        } else if (moduleID == 3) {
            entity_id = (entity_id - 1) * 3 + 1;
        }
        String name = (uRecord[5] == null || uRecord[5].isEmpty()) ? uRecord[6] : uRecord[5];
        String column = "entity_id";
        if (VERSION == 0) {
            column = columnName[moduleID];
        }
        InsertQuery query = new InsertQuery(PREF + (VERSION < 6 ? tableName[moduleID] : "comments"));
        query.addItem(new QueryItem(column, entity_id));
        query.addItem(new QueryItem("name", name));
        query.addItem(new QueryItem("message", addslashes((VERSION > 2 ? "" : "[" + parseDateToString(uRecord[4]) + "]: ") + uRecord[10])));
        query.addItem(new QueryItem("ip", uRecord[9]));
        query.addItem(new QueryItem("mail", uRecord[7]));
        if (VERSION > 2) { // 1.1.9 и новее
            query.addItem(new QueryItem("date", parseDate(uRecord[4])));
        }
        if (VERSION > 4) { // 1.3 RC и новее
            query.addItem(new QueryItem("user_id", uRecord[12] != null && !uRecord[12].isEmpty() ? uRecord[12] : "0"));
        }
        if (VERSION >= 6) { // 2.1 RC7 и новее
            query.addItem(new QueryItem("module", moduleName[moduleID]));
        }
        if (VERSION >= 9) { // 2.5 RC1 и новее
            query.addItem(new QueryItem("premoder", "confirmed"));
        }
        if (VERSION >= 10) { // AtomM 4 и новее
            query.addItem(new QueryItem("parent_id", uRecord[13] != null && !uRecord[13].isEmpty() ? uRecord[13] : "0"));
        }
        FpsData.add(query);
        return true;
    }

    /**
     * users.txt
     */
    private boolean parse_users(List FpsData, String[] uRecord) {
        if (uRecord.length < 24) {
            return false;
        }
        if (!uRecord[3].isEmpty() && !uRecord[3].equals("0")) {
            String[] path = uRecord[3].split("/");
            if (path.length > 1) {
                File file = new File(AVATAR_TABLES + path[path.length - 2] + DS + path[path.length - 1]);
                try {
                    BufferedImage imag = null;
                    if (file.exists()) {
                        imag = ImageIO.read(file);
                    } else if (USE_WEB_AVATARS) {
                        imag = ImageIO.read(new URL((String) uRecord[3]));
                    }
                    if (imag != null) {
                        if (imag.getColorModel().getTransparency() != Transparency.OPAQUE) {
                            int w = imag.getWidth();
                            int h = imag.getHeight();
                            BufferedImage image2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                            image2.createGraphics().drawImage(imag, 0, 0, image2.getWidth(), image2.getHeight(), java.awt.Color.WHITE, null);
                            imag = image2;
                        }
                        File new_file = new File("avatars" + DS + uUsers.get(uRecord[0]) + ".jpg");
                        ImageIO.write(imag, "JPEG", new_file);
                        if (file.exists()) {
                            new_file.setLastModified(file.lastModified());
                        }
                    } else {
                        System.out.println("WARNING: File \"" + file.getName() + "\" not found. Avatar for user \"" + uRecord[0] + "\" not created.");
                    }
                } catch (Exception e) {
                    System.out.println("WARNING: Avatar for user \"" + uRecord[0] + "\" not created.");
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
                String[] str = (String[]) ob;
                posts = str[9];
                status = ((Integer.parseInt(str[2]) <= 4) ? str[2] : "1");
                last_visit = str[18];
                locked = ((Integer.parseInt(str[2]) == 255) ? "1" : str[3]);
            } else {
                posts = "0";
                status = "1";
            }
        } catch (Exception e) {
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (uUsers.get(uRecord[0]).equals("1") && PASSWORD != null && !PASSWORD.isEmpty()) {
            uRecord[2] = getMD5(PASSWORD);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int byear = 0;
        int bmonth = 0;
        int bday = 0;
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(uRecord[22]));
            byear = cal.get(Calendar.YEAR);
            bmonth = cal.get(Calendar.MONTH) + 1;
            bday = cal.get(Calendar.DATE);
        } catch (ParseException ex) {
        }

        InsertQuery query = new InsertQuery(PREF + "users");
        query.addItem(new QueryItem("id", (String) uUsers.get(uRecord[0])));
        query.addItem(new QueryItem("name", addslashes(uRecord[0])));
        query.addItem(new QueryItem("passw", addslashes(uRecord[2])));
        query.addItem(new QueryItem("email", addslashes(uRecord[7])));
        query.addItem(new QueryItem("url", addslashes(uRecord[8])));
        query.addItem(new QueryItem("icq", addslashes(uRecord[9])));
        query.addItem(new QueryItem("signature", addslashes(uRecord[13])));
        query.addItem(new QueryItem("puttime", parseDate(uRecord[15])));
        query.addItem(new QueryItem("last_visit", parseDate(last_visit)));
        query.addItem(new QueryItem("posts", posts));
        query.addItem(new QueryItem("status", status));
        query.addItem(new QueryItem("locked", locked));
        query.addItem(new QueryItem("activation", activation));
        if (VERSION > 1) { // 1.1.8 beta и новее
            query.addItem(new QueryItem("warnings", "0"));
            query.addItem(new QueryItem("ban_expire", "0"));
        }
        if (VERSION > 2) { // 1.1.9 и новее
            query.addItem(new QueryItem("pol", (uRecord[6].equals("2") ? "f" : "m")));
            query.addItem(new QueryItem("jabber", ""));
            query.addItem(new QueryItem("city", addslashes(uRecord[12])));
            query.addItem(new QueryItem("telephone", ""));
            query.addItem(new QueryItem("byear", byear));
            query.addItem(new QueryItem("bmonth", bmonth));
            query.addItem(new QueryItem("bday", bday));
        }
        FpsData.add(query);
        return true;
    }

    public boolean initUsers() {
        System.out.println("Load \"ugen.txt\"...");
        String filename = DUMP_TABLES + "ugen.txt";
        if (!new File(filename).exists()) {
            System.out.println("ERROR: File \"ugen.txt\" not found.");
            return false;
        }
        uUsersMeta = new TreeMap();
        uUsers = new TreeMap();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.replace("\\|", "&#124;");
                String[] uUsersData = line.split("\\|");
                if (uUsersData.length < 2 || uUsersData[1].isEmpty()) {
                    continue;
                }
                for (int i = 0; i < uUsersData.length; i++) {
                    if (uUsersData[i].contains("&#124;")) {
                        uUsersData[i] = uUsersData[i].replace("&#124;", "|");
                    }
                }
                uUsersMeta.put(uUsersData[1], uUsersData);
                uUsers.put(uUsersData[1], uUsersData[0]);
            }
        } catch (Exception e) {
        }
        if (uUsersMeta.size() == 0) {
            System.out.println("ERROR: File \"ugen.txt\" is empty.");
            return false;
        }
        return true;
    }

    public ArrayList getSQL(String[] uTables) {
        ArrayList FpsData = new ArrayList();
        ArrayList emptySql = new ArrayList();
        if (VERSION >= 10) { // AtomM 4 и новее
            uThemes = new TreeMap();
        }
        boolean forumEmpty = false;
        boolean loadsEmpty = false;
        boolean publEmpty = false;
        boolean newsEmpty = false;
        boolean addNews = false;
        boolean addBlog = false;
        boolean addFAQ = false;
        for (int i = 0; i < uTables.length; i++) {
            System.out.println("Load \"" + uTables[i] + ".txt\"...");
            String uDumpFile = DUMP_TABLES + uTables[i] + ".txt";
            if (!new File(uDumpFile).exists()) {
                System.out.println("WARNING: File \"" + uTables[i] + ".txt\" not found.");
                continue;
            } else if (uTables[i].equals("users")) {
                if (!NO_EMPTY) {
                    emptySql.add(new TruncateQuery(PREF + "users"));
                }
                // Инициализация папки для работы с аватарами
                try {
                    File outputAvatarsDir = new File("avatars");
                    if (outputAvatarsDir.exists()) {
                        if (!outputAvatarsDir.isDirectory()) {
                            System.out.println("WARNING: Path \"avatars\" is not directory. Avatars not supported.");
                        }
                    } else {
                        try {
                            outputAvatarsDir.mkdirs();
                        } catch (Exception e) {
                            System.out.println("WARNING: Path \"avatars\" can't created. Avatars not supported.");
                        }
                    }
                } catch (Exception e) {
                }
            } else if (uTables[i].equals("fr_fr") || uTables[i].equals("forum") || uTables[i].equals("forump")) {
                if (!forumEmpty) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + "forum_cat"));
                        emptySql.add(new TruncateQuery(PREF + "forums"));
                        emptySql.add(new TruncateQuery(PREF + "themes"));
                        emptySql.add(new TruncateQuery(PREF + "forum_attaches"));
                        emptySql.add(new TruncateQuery(PREF + "posts"));
                    }
                    // Инициализация папок для работы с вложениями
                    uForumAttachDir = new ArrayList();
                    try {
                        File attachDir = new File(FORUM_ATTACH_TABLES);
                        if (attachDir.exists()) {
                            String[] attach_cats = attachDir.list();
                            for (int j = 0; j < attach_cats.length; j++) {
                                uForumAttachDir.add(FORUM_ATTACH_TABLES + attach_cats[j] + DS);
                            }
                            File outputForumDir = new File("files" + DS + "forum");
                            if (outputForumDir.exists()) {
                                if (!outputForumDir.isDirectory()) {
                                    System.out.println("WARNING: Path \"files" + DS + "forum\" is not directory. Attachments not supported.");
                                    uForumAttachDir.clear();
                                }
                            } else {
                                try {
                                    outputForumDir.mkdirs();
                                } catch (Exception e) {
                                    System.out.println("WARNING: Path \"files" + DS + "forum\" can't created. Attachments not supported.");
                                    uForumAttachDir.clear();
                                }
                            }
                        } else {
                            System.out.println("WARNING: Path \"" + FORUM_ATTACH_TABLES + "\" not found. Attachments not supported.");
                        }
                    } catch (Exception e) {
                    }
                    forumEmpty = true;
                }
            } else if (uTables[i].equals("ld_ld") || uTables[i].equals("loads")) {
                if (!loadsEmpty) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + (VERSION >= 10 ? "loads_categories" : "loads_sections")));
                        if (VERSION < 10) { // Старше AtomM 4
                            emptySql.add(new TruncateQuery(PREF + "loads_add_fields"));
                            emptySql.add(new TruncateQuery(PREF + "loads_add_content"));
                        }
                        emptySql.add(new TruncateQuery(PREF + "loads"));
                    }
                    if (VERSION <= 3) { // Старше 1.2 beta
                        InsertQuery query = new InsertQuery(PREF + "loads_add_fields");
                        query.addItem(new QueryItem("id", "1"));
                        query.addItem(new QueryItem("type", "text"));
                        query.addItem(new QueryItem("name", ""));
                        query.addItem(new QueryItem("label", "Ссылка для скачивания архива с другого сервера"));
                        query.addItem(new QueryItem("size", "255"));
                        query.addItem(new QueryItem("params", "a:0:{}"));
                        emptySql.add(query);
                    }
                    // Инициализация папки для работы с файлами
                    try {
                        File outputForumDir = new File("files" + DS + LOADS_OUT);
                        if (outputForumDir.exists()) {
                            if (!outputForumDir.isDirectory()) {
                                System.out.println("WARNING: Path \"files" + DS + LOADS_OUT + "\" is not directory. Loads not supported.");
                            }
                        } else {
                            try {
                                outputForumDir.mkdirs();
                            } catch (Exception e) {
                                System.out.println("WARNING: Path \"files" + DS + LOADS_OUT + "\" can't created. Loads not supported.");
                            }
                        }
                    } catch (Exception e) {
                    }
                    loadsEmpty = true;
                }
            } else if (uTables[i].equals("pu_pu") || uTables[i].equals("publ")) {
                if (!publEmpty) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + (VERSION >= 10 ? "stat_categories" : "stat_sections")));
                        if (VERSION < 10) { // Старше AtomM 4
                            emptySql.add(new TruncateQuery(PREF + "stat_add_fields"));
                            emptySql.add(new TruncateQuery(PREF + "stat_add_content"));
                        } else {
                            emptySql.add(new TruncateQuery(PREF + "add_fields"));
                        }
                        emptySql.add(new TruncateQuery(PREF + "stat"));
                    }
                    InsertQuery query = new InsertQuery(PREF + "add_fields");
                    query.addItem(new QueryItem("id", "1"));
                    query.addItem(new QueryItem("type", "text"));
                    query.addItem(new QueryItem("name", "link"));
                    query.addItem(new QueryItem("label", "Ссылка на источник материала"));
                    query.addItem(new QueryItem("size", "255"));
                    query.addItem(new QueryItem("params", "a:0:{}"));
                    if (VERSION < 10) { // Старше AtomM 4
                        query.setTable(PREF + "stat_add_fields");
                    } else {
                        query.addItem(new QueryItem("field_id", "1"));
                        query.addItem(new QueryItem("module", "stat"));
                    }
                    emptySql.add(query);
                    if (VERSION > 2) { // 1.1.9 и новее
                        // Инициализация папок для работы с вложениями
                        uStatAttachDir = new ArrayList();
                        try {
                            File attachDir = new File(PUBL_ATTACH_TABLES);
                            if (attachDir.exists()) {
                                String[] attach_cats = attachDir.list();
                                for (int j = 0; j < attach_cats.length; j++) {
                                    uStatAttachDir.add(PUBL_ATTACH_TABLES + attach_cats[j] + DS);
                                }
                                File outputForumDir = new File("files" + DS + "stat");
                                if (outputForumDir.exists()) {
                                    if (!outputForumDir.isDirectory()) {
                                        System.out.println("WARNING: Path \"files" + DS + "stat\" is not directory. Attachments not supported.");
                                        uStatAttachDir.clear();
                                    }
                                } else {
                                    try {
                                        outputForumDir.mkdirs();
                                    } catch (Exception e) {
                                        System.out.println("WARNING: Path \"files" + DS + "stat\" can't created. Attachments not supported.");
                                        uStatAttachDir.clear();
                                    }
                                }
                            } else {
                                System.out.println("WARNING: Path \"" + PUBL_ATTACH_TABLES + "\" not found. Attachments not supported.");
                            }
                        } catch (Exception e) {
                        }
                    }
                    publEmpty = true;
                }
            } else if (uTables[i].equals("nw_nw") || uTables[i].equals("bl_bl") || uTables[i].equals("fq_fq")
                    || uTables[i].equals("news") || uTables[i].equals("blog") || uTables[i].equals("faq")) {
                if (!newsEmpty) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + "news"));
                        emptySql.add(new TruncateQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections")));
                    }
                    if (VERSION > 2) { // 1.1.9 и новее
                        // Инициализация папок для работы с вложениями
                        uNewsAttachDir = new ArrayList();
                        uBlogAttachDir = new ArrayList();
                        uFaqAttachDir = new ArrayList();
                        try {
                            File newsAttachDir = new File(NEWS_ATTACH_TABLES);
                            if (newsAttachDir.exists()) {
                                String[] attach_cats = newsAttachDir.list();
                                for (int j = 0; j < attach_cats.length; j++) {
                                    uNewsAttachDir.add(NEWS_ATTACH_TABLES + attach_cats[j] + DS);
                                }
                            } else {
                                System.out.println("WARNING: Path \"" + NEWS_ATTACH_TABLES + "\" not found. Attachments not supported.");
                            }
                            File blogAttachDir = new File(BLOG_ATTACH_TABLES);
                            if (blogAttachDir.exists()) {
                                String[] attach_cats = blogAttachDir.list();
                                for (int j = 0; j < attach_cats.length; j++) {
                                    uBlogAttachDir.add(BLOG_ATTACH_TABLES + attach_cats[j] + DS);
                                }
                            } else {
                                System.out.println("WARNING: Path \"" + BLOG_ATTACH_TABLES + "\" not found. Attachments not supported.");
                            }
                            File faqAttachDir = new File(FAQ_ATTACH_TABLES);
                            if (faqAttachDir.exists()) {
                                String[] attach_cats = faqAttachDir.list();
                                for (int j = 0; j < attach_cats.length; j++) {
                                    uFaqAttachDir.add(FAQ_ATTACH_TABLES + attach_cats[j] + DS);
                                }
                            } else {
                                System.out.println("WARNING: Path \"" + FAQ_ATTACH_TABLES + "\" not found. Attachments not supported.");
                            }
                            // Результирующая папка
                            File outputForumDir = new File("files" + DS + "news");
                            if (outputForumDir.exists()) {
                                if (!outputForumDir.isDirectory()) {
                                    System.out.println("WARNING: Path \"files" + DS + "news\" is not directory. Attachments not supported.");
                                    uNewsAttachDir.clear();
                                    uBlogAttachDir.clear();
                                    uFaqAttachDir.clear();
                                }
                            } else {
                                try {
                                    outputForumDir.mkdirs();
                                } catch (Exception e) {
                                    System.out.println("WARNING: Path \"files" + DS + "news\" can't created. Attachments not supported.");
                                    uNewsAttachDir.clear();
                                    uBlogAttachDir.clear();
                                    uFaqAttachDir.clear();
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                    newsEmpty = true;
                }
                if (uTables[i].equals("nw_nw") || uTables[i].equals("news")) {
                    if (!addNews) {
                        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "1"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0"));
                        query.addItem(new QueryItem("title", "Новости"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "section"));
                        }
                        emptySql.add(query);
                        query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "4"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "1"));
                        query.addItem(new QueryItem("title", "Без категории"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "category"));
                        }
                        emptySql.add(query);
                        addNews = true;
                    }
                } else if (uTables[i].equals("bl_bl") || uTables[i].equals("blog")) {
                    if (!addBlog) {
                        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "2"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0"));
                        query.addItem(new QueryItem("title", "Блоги"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "section"));
                        }
                        emptySql.add(query);
                        query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "5"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "2"));
                        query.addItem(new QueryItem("title", "Без категории"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "category"));
                        }
                        emptySql.add(query);
                        addBlog = true;
                    }
                } else if (uTables[i].equals("fq_fq") || uTables[i].equals("faq")) {
                    if (!addFAQ) {
                        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "3"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0"));
                        query.addItem(new QueryItem("title", "FAQ"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "section"));
                        }
                        emptySql.add(query);
                        query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                        query.addItem(new QueryItem("id", "6"));
                        query.addItem(new QueryItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "3"));
                        query.addItem(new QueryItem("title", "Без категории"));
                        if (VERSION <= 3) {
                            query.addItem(new QueryItem("class", "category"));
                        }
                        emptySql.add(query);
                        addFAQ = true;
                    }
                }
            } else if (uTables[i].equals("comments")) {
                if (!NO_EMPTY) {
                    emptySql.add(new TruncateQuery(PREF + "loads_comments"));
                    emptySql.add(new TruncateQuery(PREF + "stat_comments"));
                    emptySql.add(new TruncateQuery(PREF + "news_comments"));
                }
            }

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(uDumpFile), "UTF-8"));
                String line = null;
                String line_int = "";
                while ((line = br.readLine()) != null) {
                    if (line.lastIndexOf("\\") == line.length() - 1) {
                        line_int += line.substring(0, line.length() - 1) + "<BR />";
                        continue;
                    } else {
                        line_int += line;
                    }
                    if (uTables[i].equals("loads") || (VERSION < 3 // Старше 1.1.9
                            && (uTables[i].equals("publ") || uTables[i].equals("news") || uTables[i].equals("blog") || uTables[i].equals("faq")))) {
                        line_int = line_int.replace("<!--IMG", "<!--I");
                    }

                    line_int = line_int.replace("\\|", "&#124;");
                    String[] uRecord = line_int.split("\\|");
                    if (uRecord.length < 2) {
                        continue;
                    }
                    for (int j = 0; j < uRecord.length; j++) {
                        if (uRecord[j].contains("&#124;")) {
                            uRecord[j] = uRecord[j].replace("&#124;", "|");
                        }
                    }
                    if (uTables[i].equals("users")) {
                        parse_users(FpsData, uRecord);
                    } else if (uTables[i].equals("fr_fr")) {
                        parse_fr_fr(FpsData, uRecord);
                    } else if (uTables[i].equals("forum")) {
                        parse_forum(FpsData, uRecord);
                    } else if (uTables[i].equals("forump")) {
                        parse_forump(FpsData, uRecord);
                    } else if (uTables[i].equals("ld_ld")) {
                        parse_ld_ld(FpsData, uRecord);
                    } else if (uTables[i].equals("loads")) {
                        parse_loads(FpsData, uRecord);
                    } else if (uTables[i].equals("pu_pu")) {
                        parse_pu_pu(FpsData, uRecord);
                    } else if (uTables[i].equals("publ")) {
                        parse_publ(FpsData, uRecord);
                    } else if (uTables[i].equals("nw_nw")) {
                        parse_nw_nw(FpsData, uRecord, 1);
                    } else if (uTables[i].equals("news")) {
                        parse_news(FpsData, uRecord, 1);
                    } else if (uTables[i].equals("bl_bl")) {
                        parse_nw_nw(FpsData, uRecord, 2);
                    } else if (uTables[i].equals("blog")) {
                        parse_news(FpsData, uRecord, 2);
                    } else if (uTables[i].equals("fq_fq")) {
                        parse_nw_nw(FpsData, uRecord, 3);
                    } else if (uTables[i].equals("faq")) {
                        parse_faq(FpsData, uRecord);
                    } else if (uTables[i].equals("comments")) {
                        parse_comments(FpsData, uRecord);
                    }
                    line_int = "";
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
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

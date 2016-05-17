package AtomM;

import AtomM.SQL.InsertQuery;
import AtomM.SQL.TruncateQuery;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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
    private String DUMP = "";
    private String DUMP_TABLES = "";
    private String FORUM_ATTACH_TABLES = "";
    private String NEWS_ATTACH_TABLES = "";
    private String BLOG_ATTACH_TABLES = "";
    private String FAQ_ATTACH_TABLES = "";
    private String PUBL_ATTACH_TABLES = "";
    private String AVATAR_TABLES = "";
    private String LOADS_TABLES = "";
    private String PREF = "";

    private String PASSWORD = null;
    private boolean USE_WEB_AVATARS = false;
    private boolean USE_WEB_LOADS = false;
    private boolean USE_WEB_ATTACHES = false;
    private boolean NO_EMPTY = false;
    private boolean NO_IMAGE = false;
    private boolean PARSE_SMILE = false;
    private boolean NO_FIX = false;
    private boolean NO_GROUPS = false;
    private String SITE_NAME_OLD = null;
    private String SITE_NAME_NEW = null;
    private Integer POST_ON_FORUM = null;
    private int MAX_WEB_SIZE = 1 << 24; // 16 Mb

    private int VERSION = 0;

    private ArrayList<ArrayList> atmData = null;
    
    private TreeMap<String, String> uUsers = null;
    private TreeMap<String, String[]> uUsersMeta = null;
    private TreeMap<String, String[]> uThemesMeta = null;

    private ArrayList[][] uData = {{null},
    {null, null, null},
    {null, null},
    {null, null},
    {null, null, null, null, null, null},
    {null}};
    private String[][] uTables = {{"users"},
    {"fr_fr", "forum", "forump"},
    {"ld_ld", "loads"},
    {"pu_pu", "publ"},
    {"nw_nw", "news", "bl_bl", "blog", "fq_fq", "faq"},
    {"comments"}};

    private String[][] uTableNames = {{"users"},
    {"forum_cat", "forums", "themes", "posts", "forum_attaches"},
    {"loads_sections", "loads", "loads_add_fields", "loads_add_content"},
    {"stat_sections", "stat", "stat_add_fields", "stat_add_content"},
    {"news", "news_sections"},
    {"loads_comments", "stat_comments", "news_comments"}};

    private TreeMap<String, String> uLinks = null;

        public void setPassword(String PASSWORD) {
        this.PASSWORD = PASSWORD;
    }

    public void setWebAvatars(boolean USE_WEB_AVATARS) {
        this.USE_WEB_AVATARS = USE_WEB_AVATARS;
    }

    public void setWebLoads(boolean USE_WEB_LOADS) {
        this.USE_WEB_LOADS = USE_WEB_LOADS;
    }

    public void setWebAttaches(boolean USE_WEB_ATTACHES) {
        this.USE_WEB_ATTACHES = USE_WEB_ATTACHES;
    }

    public void setNoEmpty(boolean NO_EMPTY) {
        this.NO_EMPTY = NO_EMPTY;
    }

    public void setNoImage(boolean NO_IMAGE) {
        this.NO_IMAGE = NO_IMAGE;
    }

    public void setSmile(boolean PARSE_SMILE) {
        this.PARSE_SMILE = PARSE_SMILE;
    }

    public void setNoFix(boolean NO_FIX) {
        this.NO_FIX = NO_FIX;
    }

    public void setNoGroups(boolean NO_GROUPS) {
        this.NO_GROUPS = NO_GROUPS;
    }
    
    public void setSiteName(String SITE_NAME_OLD, String SITE_NAME_NEW, Integer POST_ON_FORUM) {
        this.SITE_NAME_OLD = SITE_NAME_OLD;
        this.SITE_NAME_NEW = SITE_NAME_NEW;
        this.POST_ON_FORUM = POST_ON_FORUM;
    }

    public void setVersion(int VERSION) {
        this.VERSION = VERSION;
    }

    /**
     * Конструктор.
     *
     * @param DUMP путь к бекапу Ucoz (без завершающего слеша).
     */
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

    /**
     * Конструктор.
     *
     * @param DUMP путь к бекапу Ucoz (без завершающего слеша);
     * @param PREF префикс таблицы.
     */
    public Converter(String DUMP, String PREF) {
        this(DUMP);
        if (PREF != null) {
            this.PREF = PREF;
        } else {
            this.PREF = "";
        }
    }

    /**
     * Вычисление MD5-хэш от строки.
     *
     * @param str строка для вычисления MD5-хэша.
     * @return результирующий MD5-хэш.
     */
    private String getMD5(String str) {
        MessageDigest md5;
        StringBuilder hexString = new StringBuilder();
        try {
            md5 = MessageDigest.getInstance("md5");
            md5.reset();
            md5.update(str.getBytes());
            for (byte digest : md5.digest()) {
                hexString.append(Integer.toHexString(0xFF & digest));
            }
        } catch (NoSuchAlgorithmException e) {
            return e.toString();
        }
        return hexString.toString();
    }

    /**
     * Вывод информационных сообщений
     *
     * @param output строка для вывода в консоль
     */
    public static void println(String output) {
        System.out.println(output);
    }

    /**
     * Конвертация даты
     *
     * @param date строка содержащая число (количество милисекунд, прошедших с 1
     * января 1970 года, 00:00:00 GMT).
     * @return дата.
     */
    private Date parseDate(String date) {
        try {
            Date parse = (date != null && !date.isEmpty() && !date.equals("0")) ? new Date(Long.parseLong(date) * 1000) : new Date();
            return parse;
        } catch (Exception e) {
            return new Date();
        }
    }

    /**
     * Конвертация даты в строку.
     *
     * @param date строка содержащая число (количество милисекунд, прошедших с 1
     * января 1970 года, 00:00:00 GMT).
     * @return строка с датой.
     */
    private String parseDateToString(String date) {
        String parse = "0000-00-00 00:00:00";
        if (date != null && !date.isEmpty() && !date.equals("0")) {
            parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parseDate(date));
        }
        return parse;
    }

    /**
     * Генерация имени для файла вложения на основе его ID, номера вложения,
     * даты и расширения файла.
     *
     * @param post_id ID сообщения, содержащего вложение;
     * @param number номер вложения в сообщении;
     * @param date дата добавления (создания сообщения);
     * @param ext расширение файла (с точкой).
     * @return строка, содержащая уникальное имя файла.
     */
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

    /**
     * Генерация случайного имени для файла из каталога на основе имени и даты.
     *
     * @param name имя файла;
     * @param date дата добавления.
     * @return строка, содержащая уникальное имя файла.
     */
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

    /**
     * Копирование файла.
     *
     * @param filename путь к исходному файлу;
     * @param new_filename путь к результирующему файлу.
     * @return <tt>true</tt> если файл скопирован, иначе <tt>false</tt>.
     */
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

    /**
     * Загрузка файла.
     *
     * @param url путь к исходному файлу;
     * @param new_filename путь к результирующему файлу.
     * @return <tt>true</tt> если файл скопирован, иначе <tt>false</tt>.
     */
    private long loadFile(String url, String new_filename) {
        if (url != null) {
            try {
                File new_file = new File(new_filename);
                ReadableByteChannel ic = Channels.newChannel(new URL(url.replace(DS, "/")).openStream());
                FileChannel oc = new FileOutputStream(new_file).getChannel();
                long total = oc.transferFrom(ic, 0, MAX_WEB_SIZE);
                ic.close();
                oc.close();
                return total;
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Загрузка файла.
     *
     * @param urls массив путей к исходному файлу;
     * @param new_filename путь к результирующему файлу.
     * @return <tt>true</tt> если файл скопирован, иначе <tt>false</tt>.
     */
    private long loadFile(String[] urls, String new_filename) {
        if (urls != null && urls.length > 0) {
            for (String url : urls) {
                long total;
                if ((total = loadFile(url, new_filename)) >= 0) {
                    return total;
                }
            }
        }
        return -1;
    }

    /**
     * Проверка на существование каталога и его создание при отсуствии.
     *
     * @param path путь к каталогу;
     * @param error текст ошибки.
     * @return <tt>true</tt> если каталог создан или существует, иначе <tt>false</tt>.
     */
    private boolean createDir(String path, String error, boolean needAccessFile) {
        try {
            File dir = new File(path);
            if (dir.exists()) {
                if (!dir.isDirectory()) {
                    println("WARNING: Path \"" + path + "\" is not directory. " + error);
                    return false;
                }
            } else {
                dir.mkdirs();
            }
            if (needAccessFile) {
                createAccessFile(path);
            }
        } catch (Exception e) {
            println("WARNING: Path \"" + path + "\" can't created. " + error);
            return false;
        }
        return true;
    } 
    
    /**
     * Создание файла ".htaccess" (с разрешением качать всем) в заданном
     * каталоге.
     *
     * @param path путь к каталогу.
     * @return <tt>true</tt> если файл создан, иначе <tt>false</tt>.
     */
    private boolean createAccessFile(String path) {
        File new_file = new File(path + DS + ".htaccess");
        if (!new_file.exists()) {
            try {
                FileOutputStream os = new FileOutputStream(new_file);
                os.write(("Allow from all").getBytes());
                os.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Возврат массива старых доменов сайта.
     *
     * @return массив со списком доменов.
     */
    private String[] getOldSites() {
        String[] sites = {};
        if (SITE_NAME_OLD != null) {
            String[] old_sites = SITE_NAME_OLD.toLowerCase().split(";");
            ArrayList<String> keys = new ArrayList<String>();
            for (String site : old_sites) {
                keys.add("http://" + site);
                keys.add("http://www." + site);
            }
            sites = keys.size() > 0 ? keys.toArray(old_sites) : sites;
        }
        return sites;
    }
    
    /**
     * Возврат массива старых ссылок.
     *
     * @return массив со списком ссылок.
     */
    private String[] getOldLinks(String url) {
        String[] urls = getOldSites();
        for (int i = 0; i < urls.length; i++) {
            urls[i] += url;
        }
        return urls;
    }
    
    /**
     * Модификация ссылок.
     *
     * @param old_path старый путь (относительный или полный);
     * @param new_path новый путь (если null, то ссылка удаляется из списка).
     * @return <tt>true</tt> если файл создан, иначе <tt>false</tt>.
     */
    private void updateLink(String old_path, String new_path) {
        if (SITE_NAME_OLD != null && SITE_NAME_NEW != null && uLinks != null) {
            String new_url = (new_path != null && !new_path.isEmpty()) ? new_path.replace(DS, "/") : "";
            new_url = SITE_NAME_NEW.toLowerCase() + (new_url.isEmpty() || new_url.startsWith("/") ? "" : "/") + new_url;
            if (old_path.toLowerCase().startsWith("http://") || old_path.toLowerCase().startsWith("https://")) {
                if (new_path == null) {
                    uLinks.put(old_path, null);
                } else {
                    uLinks.put(old_path, new_url);
                }
            } else {
                String old_url = (!old_path.isEmpty()) ? old_path.replace(DS, "/") : "";
                old_url = (old_url.isEmpty() || old_url.startsWith("/") ? "" : "/") + old_url;
                for (String site : getOldSites()) {
                    if (new_path == null) {
                        if (old_url.endsWith("/")) {
                            uLinks.put(site + old_url.substring(0, old_url.length()-1), null);
                        } else {
                            uLinks.put(site + old_url + "/", null);
                        }
                        uLinks.put(site + old_url, null);
                    } else {
                        if (old_url.endsWith("/")) {
                            uLinks.put(site + old_url.substring(0, old_url.length()-1), new_url);
                        } else {
                            uLinks.put(site + old_url + "/", new_url);
                        }
                        uLinks.put(site + old_url, new_url);
                    }
                }
            }
        }
    }

    /**
     * Нахождение относительного пути по ссылке.
     *
     * @param url анализируемый URL.
     * @return <tt>null</tt> если домен не совпадает, иначе относительный путь.
     */
    private String trimUrl(String url) {
        String path = null;
        if (SITE_NAME_OLD != null) {
            String[] old_sites = SITE_NAME_OLD.toLowerCase().split(";");
            ArrayList<String> keys = new ArrayList<String>();
            for (String site : old_sites) {
                keys.add("http://" + site);
                keys.add("http://www." + site);
            }
            for (String key : keys.toArray(old_sites)) {
                if (url.toLowerCase().startsWith(key)) {
                    path = url.substring(key.length());
                    path = path.startsWith("/") ? path.substring(1) : path;
                }
            }
        }
        return path;
    }
    
    /**
     * Возврат мета-данных, привязанных к ID записей.
     *
     * @param table имя таблицы, для которой необходимы мета-данные.
     * @return мета-данные.
     */
    private TreeMap<String, String[]> getMeta(String table) {
        TreeMap<String, String[]> uMeta = new TreeMap<String, String[]>();
        int x = -1;
        int y = -1;
        boolean found = false;
        for (int i = 0; i < uTables.length; i++) {
            if (found) break;
            for (int j = 0; j < uTables[i].length; j++) {
                if (table.equalsIgnoreCase(uTables[i][j])) {
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
        }
        if (x >= 0 && y >= 0 && uData[x][y] != null && !uData[x][y].isEmpty()) {
            for (int k = 0; k < uData[x][y].size(); k++) {
                String[] uRecord = (String[])uData[x][y].get(k);
                if (uRecord.length > 0 && !uRecord[0].isEmpty()) {
                    uMeta.put(uRecord[0], uRecord);
                }
            }
        }
        return uMeta;
    }
    
    
    /**
     * Перевод строки в число.
     *
     * @param str строка, содержащая число;
     * @param def значение по умолчанию, присваемое при невозможности перевода.
     * @return преобразованное число.
     */
    private int parseInt(String str, int def) {
        int result = def;
        try {
            result = str != null ? Integer.parseInt(str) : def;
        } catch (Exception e) {
            result = def;
        }
        return result;
    }
    /**
     * Экранирование спецсимволов в строке.
     *
     * @param data входная строка.
     * @return обработанная строка.
     */
    private String addslashes(String data) {
        String str = HTMLtoBB(data).replace("\\", "\\\\");
//        String str = data.replace("\\", "\\\\");
//        str = str.replace("\"", "\\\"");
        str = str.replace("'", "\\'");
        return str;
    }

    /**
     * Конвертация HTML-тегов в BB-коды.
     *
     * @param html строка, содержащая HTML-теги.
     * @return обработанная строка.
     */
    private String toBB(String html) {
        String parse = html;
        ParserHTML parser = new ParserHTML();
        try {
            parser.parseSmile = PARSE_SMILE;
            parser.uLinks = uLinks;
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

    /**
     * Конвертация HTML-кода.
     *
     * @param html строка, содержащая HTML-код.
     * @return обработанная строка.
     */
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
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок форума.
     *
     * @param url_id ссылка.
     */
    private String parse_forum_stage1(String url_id, TreeMap<String, String[]> uForumsMeta, TreeMap<String, Object[]> uPosts) {
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index.length > 0 && index[0].equals("0")) {
                value = "/forum/";
                if (index.length >= 4) {
                    if (index[3].equals("34")) {// http://site.ucoz.ru/forum/0-0-1-34 Ленточный форум
                        boolean page = !index[2].isEmpty() && !index[2].equals("0") && !index[2].equals("1");
                        value = "/forum/last_posts/" + (page ? "?page=" + index[2] : "");
                        if (index.length >= 5 && !index[4].isEmpty()) {
                            value += (page ? "&" : "?") + "order=";
                            if (index[4].equals("1")) value += "title&asc=1";
                            else if (index[4].equals("2")) value += "title";
                            else if (index[4].equals("3")) value += "posts&asc=1";
                            else if (index[4].equals("4")) value += "posts";
                            else if (index[4].equals("5")) value += "views&asc=1";
                            else if (index[4].equals("6")) value += "views";
                            else if (index[4].equals("7")) value += ""; // TODO: Автор темы
                            else if (index[4].equals("8")) value += ""; // TODO: Автор темы
                            else if (index[4].equals("9")) value += "last_post&asc=1";
                            else if (index[4].equals("10")) value += "last_post";
                        }
                    } else if (index[3].equals("35")) { // http://site.ucoz.ru/forum/0-0-1-35 Пользователи форума
                        value = "/users/index/";
                    } else if (index[3].equals("36")) { // http://site.ucoz.ru/forum/0-0-0-36 Правила форума
                        value = "/forum/"; // TODO: Правила форума 
                   } else if (index[3].equals("37")) { // http://site.ucoz.ru/forum/0-0-0-37 RSS для форума
                        value = "/forum/rss/";
                    } else if (index[3].equals("6")) {// http://site.ucoz.ru/forum/0-0-0-6 Поиск для форума
                        value = "/search/";
                    } else if (index[3].equals("3") && index.length >= 5 && !index[4].isEmpty()) { // http://site.ucoz.ru/forum/0-0-1-3-1 Сообщения пользователя
                        value = "/forum/user_posts/" + index[4] + (!index[2].isEmpty() && !index[2].equals("0") && !index[2].equals("1") ? "?page=" + index[2] : "");
                    } else {
                        value = "/forum/";
                    }
                } else {
                    value = "/forum/";
                }
            } else if (index.length == 1 || (index.length > 1 && index[1].isEmpty())) { // http://site.ucoz.ru/forum/1 Категория форума или форум
                value = "/forum/";
                if (uForumsMeta != null && uForumsMeta.containsKey(index[0])) {
                    String[] uRecord = uForumsMeta.get(index[0]);
                    if (uRecord.length >= 2 && (uRecord[1] == null || uRecord[1].isEmpty() || uRecord[1].equals("0"))) {
                        value = "/forum" + (VERSION < 9 ? "/index/" + index[0] : "");
                    } else {
                        value = "/forum/view_forum/" + index[0];
                    }
                }
            } else if ((index.length == 2 || (index.length > 2 && index[2].isEmpty())) // http://site.ucoz.ru/forum/1-1 Тема форума
                    || (index.length == 3 || (index.length > 3 && index[3].isEmpty()))) { // http://site.ucoz.ru/forum/1-1-1 Тема форума с номером страницы
                if (index[1].equals("0")) {
                    value = "/forum/view_forum/" + index[0];
                } else {
                    value = "/forum/view_theme/" + index[1] + (index.length > 2 && !index[2].isEmpty() && !index[2].equals("0") && !index[2].equals("1") ? "?page=" + index[2] : "");;
                }
            } else if (index.length >= 4) {
                value = "/forum/";
                if (index[2].isEmpty() || index[2].equals("0")) {
                    if (!index[3].isEmpty() && index[3].equals("17")) { // http://site.ucoz.ru/forum/1-1-0-17-1 Последнее сообщение темы
                        value = "/forum/view_theme/" + index[1];
                        if (VERSION >= 10) { // AtomM 4 и новее
                            if (POST_ON_FORUM != null && POST_ON_FORUM > 0) {
                                String theme_id = index.length > 1 ? index[1] : null;
                                if (uThemesMeta != null && uThemesMeta.containsKey(theme_id)) {
                                    String[] uTheme = uThemesMeta.get(theme_id);
                                    int countPost = uTheme.length > 6 ? parseInt(uTheme[6], 1) : 1;
                                    int page = ((countPost - 1) / POST_ON_FORUM) + 1;
                                    value += "?page=" + page + "#post" + countPost;
                                }
                            }
                        } else { // Старше AtomM 4
                            value += "?page=999";
                        }
                    } else if (index[1].equals("0")) {
                        value = "/forum/view_forum/" + index[0];
                    } else {
                        value = "/forum/view_theme/" + index[1];
                    }
                } else if (index[3].equals("16")) { // http://site.ucoz.ru/forum/1-1-1-16-1350000000 Обработка ссылок на посты форума
                    if (VERSION >= 6) {
                        value = "/forum/view_post/" + index[2];
                    } else if (POST_ON_FORUM != null && POST_ON_FORUM > 0) {
                        if (uPosts != null && uPosts.containsKey(index[2])) {
                            Object[] entry = uPosts.get(index[2]);
                            if (entry != null && entry.length > 1) {
                                String theme_id = (String)entry[1];
                                if (theme_id == null || theme_id.isEmpty()) theme_id = index.length > 1 ? index[1] : null;
                                if (theme_id != null && !theme_id.isEmpty()) {
                                    int countPost = (Integer)entry[0];
                                    int page = ((countPost - 1) / POST_ON_FORUM) + 1;
                                    value = "/forum/view_theme/" + theme_id + "?page=" + page + "#post" + countPost;
                                }
                            }
                        }
                    }
                }
            } else {
                value = "/forum/";
            }
        } else {
            value = "/forum/";
        }
        return value;
    }
    
    /**
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок файлового архива.
     *
     * @param url_id ссылка.
     */
    private String parse_load_stage1(String url_id, TreeMap<String, String[]> uLoadsMeta) {
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index.length > 0 && index.length < 4) { // http://site.ucoz.ru/load/1-1 Категория
                value = "/loads/";
                if (index[0].equals("0")) {
                    value = "/loads/" + (index.length > 1 ? "?page=" + index[1] : "");
                } else {
                    if (uLoadsMeta != null && uLoadsMeta.containsKey(index[0])) {
                        String[] uRecord = uLoadsMeta.get(index[0]);
                        if (uRecord.length >= 3) {
                            String class_sections = (VERSION >= 3 || uRecord[2] == null || uRecord[2].isEmpty() || uRecord[2].equals("0")) ? "category" : "section";
                            value = "/loads/" + class_sections + "/" + index[0] + (index.length > 1 ? "?page=" + index[1] : "");
                        } else {
                            value = "/loads/";
                        }
                    }
                }
            } else if (index.length >= 4 && index[3] != null && !index[3].isEmpty() && !index[3].equals("0")) { // http://site.ucoz.ru/load/1-1-0-1 Материал
                value = "/loads/view/" + index[3];
            } else if (index.length == 5) {
                value = "/loads/";
                if (index[4] != null && !index[4].isEmpty()) {
                    if (index[4].equals("1")) { // http://site.ucoz.ru/load/0-0-0-0-1 Добавление материала
                        value = "/loads/add_form/";
                    } else if (index[4].equals("16")) { // Неактивные материалы и ТОП-ы
                        boolean page = !index[1].isEmpty() && !index[1].equals("0") && !index[1].equals("1");
                        value = "/loads/" + (page ? "?page=" + index[1] : "");
                        if (!index[2].isEmpty()) {
                            value += (page ? "&" : "?") + "order=";
                            if (index[2].equals("0")) value += "date"; // TODO: http://site.ucoz.ru/load/0-1-0-0-16 Неактивные материалы
                            else if (index[2].equals("1")) value += "date"; // http://site.ucoz.ru/load/0-1-1-0-16 Последние поступления (ТОП материалов, отсортированных по дате добавления)
                            else if (index[2].equals("2")) value += "date"; // TODO: http://site.ucoz.ru/load/0-1-2-0-16 Лучшие материалы (ТОП материалов, отсортированных по рейтингу)
                            else if (index[2].equals("3")) value += "downloads"; // http://site.ucoz.ru/load/0-1-3-0-16 Самые скачиваемые материалы (ТОП материалов, отсортированных по загрузкам)
                            else if (index[2].equals("4")) value += "views"; // http://site.ucoz.ru/load/0-1-4-0-16 Самые читаемые материалы (ТОП материалов, отсортированных по просмотрам)
                            else if (index[2].equals("5")) value += "comments"; // http://site.ucoz.ru/load/0-1-5-0-16 Самые комментируемые материалы (ТОП материалов, отсортированных по комментариям)
                        }
                        
                    } else if (index[4].equals("17")) { // http://site.ucoz.ru/load/0-0-1-0-17 Материалы пользователя
                        value = "/loads/" + (!index[2].isEmpty() && !index[2].equals("0") ? "user/" + index[2] : "")
                                + (!index[1].isEmpty() && !index[1].equals("0") && !index[1].equals("1") ? "?page=" + index[1] : "");
                    } else if (index[4].equals("13")) { // http://site.ucoz.ru/load/0-0-0-1-13 Редактирование материала
                        value = "/loads/" + (!index[3].isEmpty() && !index[3].equals("0") ? "edit_form/" + index[3] : "");
                    } else if (index[4].equals("20")) { // http://site.ucoz.ru/load/0-0-0-1-20 Ссылка для скачивания материала
                        value = "/loads/" + (!index[3].isEmpty() && !index[3].equals("0") ? "download_file/" + index[3] : "");
                    } else {
                        value = "/loads/";
                    }
                }
            } else {
                value = "/loads/";
            }
        } else {
            value = "/loads/";
        }
        return value;
    }
    
    /**
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок публикаций.
     *
     * @param url_id ссылка.
     */
    private String parse_publ_stage1(String url_id, TreeMap<String, String[]> uStatsMeta) {
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index.length > 0 && index.length < 4) { // http://site.ucoz.ru/publ/1-1 Категория
                value = "/stat/";
                if (index[0].equals("0")) {
                    value = "/stat/" + (index.length > 1 ? "?page=" + index[1] : "");
                } else {
                    if (uStatsMeta != null && uStatsMeta.containsKey(index[0])) {
                        String[] uRecord = uStatsMeta.get(index[0]);
                        if (uRecord.length >= 3) {
                            String class_sections = (VERSION >= 3 || uRecord[2] == null || uRecord[2].isEmpty() || uRecord[2].equals("0")) ? "category" : "section";
                            value = "/stat/" + class_sections + "/" + index[0] + (index.length > 1 ? "?page=" + index[1] : "");
                        } else {
                            value = "/stat/";
                        }
                    }
                }
            } else if (index.length >= 4 && index[3] != null && !index[3].isEmpty() && !index[3].equals("0")) { // http://site.ucoz.ru/publ/1-1-0-1 Материал
                value = "/stat/view/" + index[3];
            } else if (index.length == 5) {
                value = "/stat/";
                if (index[4] != null && !index[4].isEmpty()) {
                    if (index[4].equals("1")) { // http://site.ucoz.ru/publ/0-0-0-0-1 Добавление материала
                        value = "/stat/add_form/";
                    } else if (index[4].equals("16")) { // Неактивные материалы и ТОП-ы
                        boolean page = !index[1].isEmpty() && !index[1].equals("0") && !index[1].equals("1");
                        value = "/stat/" + (page ? "?page=" + index[1] : "");
                        if (!index[2].isEmpty()) {
                            value += (page ? "&" : "?") + "order=";
                            if (index[2].equals("0")) value += "date"; // TODO: http://site.ucoz.ru/publ/0-1-0-0-16 Неактивные материалы
                            else if (index[2].equals("1")) value += "date"; // http://site.ucoz.ru/publ/0-1-1-0-16 Последние поступления (ТОП материалов, отсортированных по дате добавления)
                            else if (index[2].equals("2")) value += "date"; // TODO: http://site.ucoz.ru/publ/0-1-2-0-16 Лучшие материалы (ТОП материалов, отсортированных по рейтингу)
                            else if (index[2].equals("3")) value += "downloads"; // http://site.ucoz.ru/publ/0-1-3-0-16 Самые скачиваемые материалы (ТОП материалов, отсортированных по загрузкам)
                            else if (index[2].equals("4")) value += "views"; // http://site.ucoz.ru/publ/0-1-4-0-16 Самые читаемые материалы (ТОП материалов, отсортированных по просмотрам)
                            else if (index[2].equals("5")) value += "comments"; // http://site.ucoz.ru/publ/0-1-5-0-16 Самые комментируемые материалы (ТОП материалов, отсортированных по комментариям)
                        }
                        
                    } else if (index[4].equals("17")) { // http://site.ucoz.ru/publ/0-0-1-0-17 Материалы пользователя
                        value = "/stat/" + (!index[2].isEmpty() && !index[2].equals("0") ? "user/" + index[2] : "")
                                + (!index[1].isEmpty() && !index[1].equals("0") && !index[1].equals("1") ? "?page=" + index[1] : "");
                    } else if (index[4].equals("13")) { // http://site.ucoz.ru/publ/0-0-0-1-13 Редактирование материала
                        value = "/stat/" + (!index[3].isEmpty() && !index[3].equals("0") ? "edit_form/" + index[3] : "");
                    } else {
                        value = "/stat/";
                    }
                }
            } else {
                value = "/stat/";
            }
        } else {
            value = "/stat/";
        }
        return value;
     }
    
    /**
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок новостей.
     *
     * @param url_id ссылка.
     */
    private String parse_news_stage1(String url_id, TreeMap<String, String[]> uNewsMeta, int mode) {
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index.length > 0 && index.length < 4) {
                value = "/news/";
                if (index.length == 1) { // http://site.ucoz.ru/news/2 2-я страница архива новостей
                    boolean page = !index[0].isEmpty() && !index[0].equals("0") && !index[0].equals("1");
                    value = "/news/" + (page ? "?page=" + index[0] : "");
                } else if (index.length == 2 && index[1] != null) {
                    if (index[1].equals("00")) { // TODO: http://site.ucoz.ru/news/2011-00 Календарь с сообщениями за 2011 год
                    } else { // TODO: http://site.ucoz.ru/news/2011-02 Календарь с сообщениями за февраль 2011 года
                    }
                } else if (index.length == 3 && index[1] != null && index[2] != null) {
                    if (index[1].equals("0")) { // http://site.ucoz.ru/news/2-0-1 2-я страница категории
                        boolean page = !index[0].isEmpty() && !index[0].equals("0") && !index[0].equals("1");
                        int id = (parseInt(index[2], 1) + 1) * 3 + mode;
                        if (uNewsMeta != null && uNewsMeta.containsKey(index[0])) {
                            String[] uRecord = uNewsMeta.get(index[0]);
                            if (uRecord.length >= 3) {
                                String class_sections = (VERSION >= 3 || uRecord[2] == null || uRecord[2].isEmpty() || uRecord[2].equals("0")) ? "category" : "section";
                                value = "/news/" + class_sections + "/" + id + (page ? "?page=" + index[0] : "");
                            } else {
                                value = "/news/";
                            }
                        }
                    } else { // TODO: http://site.ucoz.ru/news/2011-02-06 Сообщения за 6 февраля 2011 года
                    }
                }
            } else if (index.length >= 4 && index[3] != null && !index[3].isEmpty() && !index[3].equals("0")) { // http://site.ucoz.ru/news/2011-02-06-1 Материал
                int id = (parseInt(index[3], 1) - 1) * 3 + mode;
                value = "/news/view/" + id;
            } else {
                value = "/news/";
            }
        } else {
            value = "/news/";
        }
        return value;
    }
    
    /**
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок FAQ.
     *
     * @param url_id ссылка.
     */
    private String parse_faq_stage1(String url_id, TreeMap<String, String[]> uFaqMeta) {
        int mode = 3;
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index.length > 0 && index.length < 3) {
                boolean page = index.length > 1 && !index[1].isEmpty() && !index[1].equals("0") && !index[1].equals("1");
                if (index[0] == null || index[0].equals("0")) { // http://site.ucoz.ru/faq/0-2 2-я страница FAQ
                    value = "/news/" + (page ? "?page=" + index[1] : "");
                } else { // http://site.ucoz.ru/faq/1-2 2-я страница категории
                    int id = (parseInt(index[0], 1) + 1) * 3 + mode;
                    if (uFaqMeta != null && uFaqMeta.containsKey(index[0])) {
                        String[] uRecord = uFaqMeta.get(index[0]);
                        if (uRecord.length >= 3) {
                            String class_sections = (VERSION >= 3 || uRecord[2] == null || uRecord[2].isEmpty() || uRecord[2].equals("0")) ? "category" : "section";
                            value = "/news/" + class_sections + "/" + id + (page ? "?page=" + index[1] : "");
                        } else {
                            value = "/news/";
                        }
                    }
                }
            } else if (index.length >= 3 && index[2] != null && !index[2].isEmpty() && !index[2].equals("0")) { // http://site.ucoz.ru/faq/1-0-9 Материал
                value = "/news/view/" + index[2];
            } else {
                value = "/news/";
            }
        } else {
            value = "/news/";
        }
        return value;
    }
    
    /**
     * Первоначальный парсинг ссылок (этап 1) - обработка ссылок пользователей.
     *
     * @param url_id ссылка.
     */
    private String parse_index_stage1(String url_id) {
        String value = null;
        if (url_id != null) {
            String[] index = url_id.split("-");
            if (index[0].equals("15")) {
                // Пользователи сайта
                value = "/users/index/";
            } else if (index[0].equals("1")) {
                // Страница входа
                value = "/users/login_form/";
            } else if (index[0].equals("3")) {
                // Страница регистрации
                value = "/users/add_form/";
            } else if (index[0].equals("34")) {
                // Комментарии пользователя index[1]
            } else if (index[0].equals("8")) {
                index = url_id.split("-", 3);
                // Профиль пользователя с номером index[1] или именем index[2]
                if (index.length == 2) {
                    value = "/users/info/" + index[1];
                } else if (index.length == 3 && index[1].equals("0")) {
                    String user_id = (String) uUsers.get(index[2]);
                    if (user_id != null) {
                        value = "/users/info/" + user_id;
                    }
                }
            }
        } else {
            value = "";
        }
        return value;
    }
    
    /**
     * Обработка файла "forum.txt" (этап 3) - конвертация тем форума.
     *
     * @param uRecord массив строк, содержащий данные о теме.
     */
    private boolean parse_forum_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 13) {
            return false;
        }
        String id_author = (uUsers.get(uRecord[10]) != null && !(uUsers.get(uRecord[10])).isEmpty()) ? uUsers.get(uRecord[10]) : "0";
        String id_last_author = (uUsers.get(uRecord[12]) != null && !(uUsers.get(uRecord[12])).isEmpty()) ? uUsers.get(uRecord[12]) : "0";
        InsertQuery query = new InsertQuery(PREF + "themes");
        query.addItem("id", uRecord[0]);
        query.addItem("id_forum", uRecord[1]);
        query.addItem("important", uRecord[3]);
        query.addItem("id_last_author", id_last_author);
        query.addItem("last_post", parseDate(uRecord[4]));
        query.addItem("locked", uRecord[5]);
        query.addItem("posts", uRecord[6]);
        query.addItem("views", uRecord[7]);
        query.addItem("title", addslashes(uRecord[8]));
        query.addItem("id_author", id_author);
        if (VERSION > 0) {
            query.addItem("description", addslashes(uRecord[9]));
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "forump.txt" (этап 3) - конвертация сообщений форума.
     *
     * @param uRecord массив строк, содержащий данные о сообщении.
     */
    private boolean parse_forump_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 11) {
            return false;
        }
        String id_author = (uUsers.get(uRecord[6]) != null && !(uUsers.get(uRecord[6])).isEmpty()) ? uUsers.get(uRecord[6]) : "0";
        String attach = "0";
        if (uRecord[10] != null && !uRecord[10].isEmpty()) {
            attach = (uRecord[10].split("`").length > 0) ? "1" : "0";
        }
        InsertQuery query = new InsertQuery(PREF + "posts");
        query.addItem("id", uRecord[0]);
        query.addItem("id_theme", uRecord[1]);
        query.addItem("time", parseDate(uRecord[2]));
        query.addItem("message", addslashes(uRecord[4]));
        query.addItem("id_author", id_author);
        if (!uRecord[9].isEmpty() && !uRecord[9].equals("0")) {
            query.addItem("edittime", parseDate(uRecord[9]));
        }
        query.addItem("attaches", attach);
        if (VERSION >= 10 && uThemesMeta != null && uThemesMeta.containsKey(uRecord[1])) { // AtomM 4 и новее
            String[] uTheme = uThemesMeta.get(uRecord[1]);
            query.addItem("id_forum", uTheme.length > 1 ? uTheme[1] : "0");
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "forump.txt" (этап 2) - перенос файлов-вложений для
     * сообщений форума, конвертация ссылок на сообщения форума и вложения.
     *
     * @param uRecord массив строк, содержащий данные о сообщении.
     */
    private boolean parse_forump_stage2(List sqlData, String[] uRecord) {
        if (uRecord.length < 11) {
            return false;
        }
        String path = FORUM_ATTACH_TABLES + ((Integer) (Integer.parseInt(uRecord[1]) / 100)).toString() + DS;
        String id_author = (uUsers.get(uRecord[6]) != null && !(uUsers.get(uRecord[6])).isEmpty()) ? uUsers.get(uRecord[6]) : "0";
        if (uRecord[10] != null && !uRecord[10].isEmpty()) {
            String[] attaches = uRecord[10].split("`");
            for (int i = 0; i < attaches.length; i++) {
                if (attaches[i].length() > 0) {
                    int pos = attaches[i].lastIndexOf('.');
                    String ext = (pos >= 0) ? attaches[i].substring(pos) : "";
                    String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                            || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                    String new_path = (VERSION >= 11 && is_image.equals("1") ? "images" : "files") + DS + "forum" + DS;
                    String new_filename = attachesName(uRecord[0], Integer.toString(i + 1), uRecord[2], ext);
                    String filename = path + ((is_image.equals("1") && attaches[i].substring(0, 1).equalsIgnoreCase("s")) ? attaches[i].substring(1) : attaches[i]);
                    if (filename.startsWith(DUMP) && (copyFile(filename, new_path + new_filename)
                            || (USE_WEB_ATTACHES && (loadFile(getOldLinks(filename.substring(DUMP.length() - 1).replace(DS, "/")), new_path + new_filename) >= 0)))) {
                        updateLink(filename.substring(DUMP.length()), (VERSION >= 10 ? "/data/" : "/sys/") + (new_path + new_filename).replace(DS, "/"));
                        if (is_image.equals("1") && attaches[i].substring(0, 1).equalsIgnoreCase("s")) {
                            filename = path + attaches[i];
                            if (filename.lastIndexOf(ext) >= 0) {
                                filename = filename.substring(0, filename.lastIndexOf(ext)) + ".jpg";
                                updateLink(filename.substring(DUMP.length()), (VERSION >= 10 ? "/data/" : "/sys/") + (new_path + new_filename).replace(DS, "/"));
                            }
                        }
                        InsertQuery query_add = new InsertQuery(PREF + "forum_attaches");
                        query_add.addItem("post_id", uRecord[0]);
                        query_add.addItem("theme_id", uRecord[1]);
                        query_add.addItem("user_id", id_author);
                        query_add.addItem("attach_number", i + 1);
                        query_add.addItem("filename", new_filename);
                        query_add.addItem("size", new File((VERSION >= 11 && is_image.equals("1") ? "images" : "files") + DS + "forum" + DS + new_filename).length());
                        query_add.addItem("date", parseDate(uRecord[2]));
                        query_add.addItem("is_image", is_image);
                        sqlData.add(query_add);
                    } else {
                        println("WARNING: Attachment \"" + attaches[i] + "\" [forum post ID=\"" + uRecord[0] + "\"] not found.");
                    }
                }
            }
        }
        return true;
    }

    /**
     * Обработка файла "fr_fr.txt" (этап 3) - конвертация разделов и форумов
     * модуля "Форум".
     *
     * @param uRecord массив строк, содержащий данные о разделах и форумах.
     */
    private boolean parse_fr_fr_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 6) {
            return false;
        }
        InsertQuery query = new InsertQuery(PREF + "forums");
        query.addItem("id", uRecord[0]);
        query.addItem("title", addslashes(uRecord[5]));
        if (uRecord[1] == null || uRecord[1].isEmpty() || uRecord[1].equals("0")) {
            query.setTable(PREF + "forum_cat");
        } else {
            if (uRecord.length < 17) {
                return false;
            }
            String last_theme_id = ((uRecord[16] != null) && !uRecord[16].isEmpty()) ? uRecord[16] : "";
            String themes = ((uRecord[9] != null) && !uRecord[9].isEmpty()) ? uRecord[9] : "0";
            String posts = ((uRecord[10] != null) && !uRecord[10].isEmpty()) ? uRecord[10] : "0";
            query.addItem("in_cat", uRecord[1]);
            query.addItem("last_theme_id", last_theme_id);
            query.addItem("themes", themes);
            query.addItem("posts", posts);
            query.addItem("description", addslashes(uRecord[6]));
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "ld_ld.txt" (этап 3) - конвертация разделов и категорий
     * каталога файлов.
     *
     * @param uRecord массив строк, содержащий данные о разделах и категориях.
     */
    private boolean parse_ld_ld_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 7) {
            return false;
        }
        String section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "loads_categories" : "loads_sections"));
        query.addItem("id", uRecord[0]);
        query.addItem("title", addslashes(uRecord[5]));
        query.addItem("announce", addslashes(uRecord[6]));
        query.addItem("view_on_home", "0");
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem("parent_id", section_id);
            query.addItem("no_access", "");
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem("path", (section_id.equals("0") ? "" : section_id + "."));
            }
        } else { // Старше 1.2 beta
            String class_sections = "category";
            if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals("0")) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
            query.addItem("section_id", section_id);
            query.addItem("class", class_sections);
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файлов "nw_nw.txt", "bl_bl.txt" и "fq_fq.txt" (этап 3) -
     * конвертация категорий новостей, блогов и FAQ.
     *
     * @param uRecord массив строк, содержащий данные о категории;
     * @param mode режим работы:
     * <tt>1</tt> - обработка категорий новостей,
     * <tt>2</tt> - обработка категорий блогов,
     * <tt>3</tt> - обработка категорий FAQ.
     */
    private boolean parse_nw_nw_stage3(List sqlData, String[] uRecord, int mode) {
        if (uRecord.length < 5) {
            return false;
        }
        int id = (parseInt(uRecord[0], 0) + 1) * 3 + mode;
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
        query.addItem("id", id);
        query.addItem("title", addslashes(uRecord[3]));
        query.addItem("announce", addslashes(uRecord[4]));
        query.addItem("view_on_home", "1");
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem("parent_id", mode);
            query.addItem("no_access", "");
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem("path", mode + ".");
            }
        } else { // Старше 1.2 beta
            query.addItem("section_id", mode);
            query.addItem("class", "category");
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "pu_pu.txt" (этап 3) - конвертация категорий статей.
     *
     * @param uRecord массив строк, содержащий данные о категории.
     */
    private boolean parse_pu_pu_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 6) {
            return false;
        }
        String section_id = ((uRecord[1] != null) && !uRecord[1].isEmpty()) ? uRecord[1] : "0";
        InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "stat_categories" : "stat_sections"));
        query.addItem("id", uRecord[0]);
        query.addItem("title", addslashes(uRecord[5]));
        query.addItem("view_on_home", "1");
        if (VERSION > 3) { // 1.2 beta и новее
            query.addItem("parent_id", section_id);
            query.addItem("no_access", "");
            if (VERSION >= 8) { // 2.4 RC5 и новее
                query.addItem("path", (section_id.equals("0") ? "" : section_id + "."));
            }
        } else { // Старше 1.2 beta
            String class_sections = "category";
            if ((uRecord[2] == null) || uRecord[2].isEmpty() || uRecord[2].equals("0")) {
                class_sections = "category";
            } else {
                section_id = "0";
                class_sections = "section";
            }
            query.addItem("section_id", section_id);
            query.addItem("class", class_sections);
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "loads.txt" (этап 3) - конвертация материалов каталога
     * файлов.
     *
     * @param uRecord массив строк, содержащий данные о материале.
     */
    private boolean parse_loads_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 33) {
            return false;
        }
        String download = "";
        if (uRecord[24] != null && !uRecord[24].isEmpty()) {
            download = loadsName(uRecord[24], uRecord[5]);
        } else if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            if (uRecord[22].startsWith("file://")) {
                download = uRecord[22].substring(("file://").length());
                uRecord[22] = "";
            }
        }
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[6].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[4].equals("1")) ? "1" : "0";
        String author_id = (uUsers.get(uRecord[26]) != null && !(uUsers.get(uRecord[26])).isEmpty()) ? uUsers.get(uRecord[26]) : "0";
        InsertQuery query = new InsertQuery(PREF + "loads");
        query.addItem("id", uRecord[0]);
        query.addItem("title", addslashes(uRecord[15]));
        query.addItem("main", addslashes(uRecord[32]));
        query.addItem("author_id", author_id);
        query.addItem("category_id", uRecord[2]);
        query.addItem("views", uRecord[13]);
        query.addItem("downloads", uRecord[14]);
        query.addItem("download", download);
        query.addItem("date", parseDate(uRecord[5]));
        query.addItem("comments", uRecord[8]);
        query.addItem("description", addslashes(uRecord[16]));
        query.addItem("sourse", uRecord[27]);
        query.addItem("sourse_email", uRecord[28]);
        query.addItem("sourse_site", uRecord[29]);
        query.addItem("commented", commented);
        query.addItem("available", available);
        query.addItem("view_on_home", "0");
        query.addItem("on_home_top", on_home_top);
        if (VERSION > 3) { // 1.2 beta и новее
            long lsize = 0;
            try {
                lsize = Long.parseLong(uRecord[23]);
            } catch (NumberFormatException ex) {
                lsize = 0;
            }
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem("premoder", "confirmed");
            }
            query.addItem("download_url", uRecord[22]);
            query.addItem("download_url_size", lsize);
        } else { // Старше 1.2 beta
            query.addItem("section_id", uRecord[1]);
            if (uRecord[22] != null && !uRecord[22].isEmpty()) {
                InsertQuery query_add = new InsertQuery(PREF + "loads_add_content");
                query_add.addItem("field_id", "1");
                query_add.addItem("entity_id", uRecord[0]);
                query_add.addItem("content", uRecord[22]);
                sqlData.add(query_add);
            }
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "loads.txt" (этап 2) - перенос файлов модуля "Каталог
     * файлов", конвертация ссылок на материалы каталога файлов, модификация
     * внешних ссылок.
     *
     * @param uRecord массив строк, содержащий данные о материале.
     * @return имя скопированного файла или <tt>null</tt>, если запрос
     * сформировать невозможно.
     */
    private String parse_loads_stage2(List sqlData, String[] uRecord) {
        if (uRecord.length < 25) {
            return null;
        }
        String download = "";
        String output = null;
        if (uRecord[24] != null && !uRecord[24].isEmpty()) {
            download = loadsName(uRecord[24], uRecord[5]);
            String filename = String.format("%s_%s", uRecord[0], uRecord[24]);
            String old_path = LOADS_TABLES + ((Integer) (parseInt(uRecord[0], 0) / 100)).toString() + DS + filename;
            String new_path = "files" + DS + "loads" + DS + download;
            if (old_path.startsWith(DUMP) && (copyFile(old_path, new_path)
                    || (USE_WEB_LOADS && (loadFile(getOldLinks(old_path.substring(DUMP.length() - 1).replace(DS, "/")), new_path) >= 0)))) {
                updateLink(old_path.substring(DUMP.length()), (VERSION >= 10 ? "/data/" : "/sys/") + new_path.replace(DS, "/"));
            } else {
                println("WARNING: File \"" + filename + "\" [load ID=" + uRecord[0] + "] not found.");
            }
        } else if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            String filename = trimUrl(uRecord[22]);
            if (filename != null) {
                download = loadsName(uRecord[22], uRecord[5]);
                String old_path = DUMP + filename.replace("/", DS);
                String new_path = "files" + DS + "loads" + DS + download;
                if (copyFile(old_path, new_path) || (USE_WEB_LOADS && loadFile(uRecord[22], new_path) >= 0)) {
                    updateLink(filename, (VERSION >= 10 ? "/data/" : "/sys/") + new_path.replace(DS, "/"));
                    output = "file://" + download; // TODO: Check return value
                } else {
                    println("WARNING: File \"" + filename + "\" [load ID=" + uRecord[0] + "] not found.");
                }
            }
        }
        return output;
    }

    /**
     * Обработка файлов "news.txt" и "blog.txt" (этап 3) - конвертация
     * материалов новостей и блогов.
     *
     * @param uRecord массив строк, содержащий данные о материале;
     * @param mode режим работы:
     * <tt>1</tt> - обработка материала новостей,
     * <tt>2</tt> - обработка материала блогов.
     */
    private boolean parse_news_stage3(List sqlData, String[] uRecord, int mode) {
        if (uRecord.length < 17) {
            return false;
        }
        int id = (parseInt(uRecord[0], 1) - 1) * 3 + mode;
        int category_id = (parseInt(uRecord[1], 0) + 1) * 3 + mode;
        category_id = category_id <= 0 ? 1 : category_id;
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[5].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[6].equals("1")) ? "1" : "0";
        String author_id = (uUsers.get(uRecord[10]) != null && !(uUsers.get(uRecord[10])).isEmpty()) ? uUsers.get(uRecord[10]) : "0";
        InsertQuery query = new InsertQuery(PREF + "news");
        query.addItem("id", id);
        query.addItem("title", addslashes(uRecord[11]));
        query.addItem("main", addslashes(uRecord[13]));
        query.addItem("author_id", author_id);
        query.addItem("category_id", category_id);
        query.addItem("views", uRecord[16]);
        query.addItem("date", parseDate(uRecord[8]));
        query.addItem("comments", uRecord[9]);
        query.addItem("description", addslashes(uRecord[12]));
        query.addItem("commented", commented);
        query.addItem("available", available);
        query.addItem("view_on_home", "1");
        query.addItem("on_home_top", on_home_top);
        if (VERSION > 3) { // 1.2 beta и новее
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem("premoder", "confirmed");
            }
        } else { // Старше 1.2 beta
            query.addItem("section_id", mode);
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "faq.txt" (этап 3) - конвертация материалов FAQ.
     *
     * @param uRecord массив строк, содержащий данные о материале.
     */
    private boolean parse_faq_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 18) {
            return false;
        }
        int id = (parseInt(uRecord[0], 1) - 1) * 3 + 3;
        int category_id = (parseInt(uRecord[1], 0) + 1) * 3 + 3;
        String available = (uRecord[5].equals("0")) ? "1" : "0";
        String author_id = (uUsers.get(uRecord[13]) != null && !(uUsers.get(uRecord[13])).isEmpty()) ? uUsers.get(uRecord[13]) : "0";
        InsertQuery query = new InsertQuery(PREF + "news");
        query.addItem("id", id);
        query.addItem("title", addslashes(uRecord[10]));
        query.addItem("main", addslashes(uRecord[12]));
        query.addItem("author_id", author_id);
        query.addItem("category_id", category_id);
        query.addItem("date", parseDate(uRecord[4]));
        query.addItem("description", addslashes(uRecord[11]));
        query.addItem("sourse", uRecord[14]);
        query.addItem("sourse_email", uRecord[15]);
        query.addItem("available", available);
        query.addItem("view_on_home", "1");
        if (VERSION > 3) { // 1.2 beta и новее
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem("premoder", "confirmed");
            }
        } else { // Старше 1.2 beta
            query.addItem("section_id", "3");
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "publ.txt" (этап 3) - конвертация статей.
     *
     * @param uRecord массив строк, содержащий данные о статье.
     */
    private boolean parse_publ_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 25) {
            return false;
        }
        String commented = (uRecord[7].equals("0")) ? "0" : "1";
        String available = (uRecord[6].equals("0")) ? "1" : "0";
        String on_home_top = (uRecord[4].equals("1")) ? "1" : "0";
        String author_id = (uUsers.get(uRecord[15]) != null && !(uUsers.get(uRecord[15])).isEmpty()) ? uUsers.get(uRecord[15]) : "0";
        InsertQuery query = new InsertQuery(PREF + "stat");
        query.addItem("id", uRecord[0]);
        query.addItem("title", addslashes(uRecord[13]));
        query.addItem("main", addslashes(uRecord[20]));
        query.addItem("author_id", author_id);
        query.addItem("category_id", uRecord[2]);
        query.addItem("views", uRecord[21]);
        query.addItem("date", parseDate(uRecord[5]));
        query.addItem("comments", uRecord[8]);
        query.addItem("description", addslashes(uRecord[14]));
        query.addItem("sourse", uRecord[16]);
        query.addItem("sourse_email", uRecord[17]);
        query.addItem("sourse_site", uRecord[18]);
        query.addItem("commented", commented);
        query.addItem("available", available);
        query.addItem("view_on_home", "1");
        query.addItem("on_home_top", on_home_top);
        if (VERSION > 3) { // 1.2 beta и новее
            if (VERSION >= 7) { // 2.2 RC1 и новее
                query.addItem("premoder", "confirmed");
            }
        } else { // Старше 1.2 beta
            query.addItem("section_id", uRecord[1]);
        }
        if (uRecord[22] != null && !uRecord[22].isEmpty()) {
            if (VERSION < 10) { // Старше AtomM 4
                InsertQuery query_add = new InsertQuery(PREF + "stat_add_content");
                query_add.addItem("field_id", "1");
                query_add.addItem("entity_id", uRecord[0]);
                query_add.addItem("content", uRecord[19]);
                sqlData.add(query_add);
            } else { // AtomM 4 и новее
                query.addItem("add_field_1", uRecord[19]);
            }
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файлов "publ.txt & news.txt", "blog.txt" и "faq.txt" (этап 2) -
     * перенос файлов-вложений для материалов статей, новостей, блогов и FAQ,
     * конвертация ссылок на вложения.
     *
     * @param uRecord массив строк, содержащий данные о материале;
     * @param mode режим работы:
     * <tt>0</tt> - обработка статьи,
     * <tt>1</tt> - обработка новости,
     * <tt>2</tt> - обработка материала блога,
     * <tt>3</tt> - обработка вопроса FAQ.
     * @return ссылки на вложение или <tt>null</tt>, если вложений нет.
     */
    private String parse_news_stage2(List sqlData, String[] uRecord, int mode) {
        if (mode > 3 || (mode == 0 && uRecord.length < 25) || (mode == 1 && uRecord.length < 16)
                || (mode == 2 && uRecord.length < 16) || (mode == 3 && uRecord.length < 18)) {
            return null;
        }
        String id = mode > 0 ? Integer.toString((parseInt(uRecord[0], 1) - 1) * 3 + mode) : uRecord[0];
        String files = mode == 0 ? uRecord[24] : (mode == 3 ? uRecord[17] : uRecord[15]);
        String date = mode == 0 ? uRecord[5] : (mode == 3 ? uRecord[4] : uRecord[8]);
        String author_name = mode == 0 ? uRecord[15] : (mode == 3 ? uRecord[13] : uRecord[10]);
        String[] full_paths = {PUBL_ATTACH_TABLES, NEWS_ATTACH_TABLES, BLOG_ATTACH_TABLES, FAQ_ATTACH_TABLES};
        String[] modules = {"publication", "news", "blog", "FAQ"};
        String[] new_paths = {"stat", "news", "news", "news"};
        String[] tables = {"stat_attaches", "news_attaches", "news_attaches", "news_attaches"};
        String author_id = (uUsers.get(author_name) != null && !(uUsers.get(author_name)).isEmpty()) ? uUsers.get(author_name) : "0";
        String path = full_paths[mode] + ((Integer) (Integer.parseInt(uRecord[0]) / 100)).toString() + DS;
        String output = "";
        String[] attaches = files.split("\\|");
        if (attaches != null && attaches.length > 0) {
            for (int i = 0; i < attaches.length; i++) {
                if (attaches[i] != null && !attaches[i].isEmpty()) {
                    String[] parts = attaches[i].split("`");
                    if (parts.length > 1) {
                        String ext = (parts[1].isEmpty()) ? "" : "." + parts[1];
                        String is_image = (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".jpg")
                                || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".jpeg")) ? "1" : "0";
                        String new_path = (VERSION >= 11 && is_image.equals("1") ? "images" : "files") + DS + new_paths[mode] + DS;
                        String new_filename = attachesName(id, Integer.toString(i + 1), date, ext);
                        String filename = path + parts[0] + ext;
                        if (filename.startsWith(DUMP) && (copyFile(filename, new_path + new_filename)
                                || (USE_WEB_ATTACHES && (loadFile(getOldLinks(filename.substring(DUMP.length() - 1).replace(DS, "/")), new_path + new_filename) >= 0)))) {
                            if (VERSION > 3) { // 1.2 beta и новее
                                InsertQuery query_add = new InsertQuery(PREF + tables[mode]);
                                query_add.addItem("entity_id", id);
                                query_add.addItem("user_id", author_id);
                                query_add.addItem("attach_number", i + 1);
                                query_add.addItem("filename", new_filename);
                                query_add.addItem("size", new File(new_path + new_filename).length());
                                query_add.addItem("date", parseDate(date));
                                query_add.addItem("is_image", is_image);
                                sqlData.add(query_add);
                            } else { // Старше 1.2 beta
                                float size = (float) new File(new_path + new_filename).length() / 1024;
                                output += String.format("<br />Вложение %d: <a href=\"%s\">%s (%.3f Кбайт)</a>", i + 1,
                                        SITE_NAME_NEW.toLowerCase() + "/sys/" + (new_path + new_filename).replace(DS, "/"),
                                        parts[0] + ext, size);
                            }
                            updateLink(filename.substring(DUMP.length()), (VERSION >= 10 ? "/data/" : "/sys/") + (new_path + new_filename).replace(DS, "/"));
                            if (is_image.equals("1")) {
                                filename = path + "s" + parts[0] + ".jpg";
                                updateLink(filename.substring(DUMP.length()), (VERSION >= 10 ? "/data/" : "/sys/") + (new_path + new_filename).replace(DS, "/"));
                            }
                        } else {
                            println("WARNING: Attachment \"" + parts[0] + ext + "\" [" + modules[mode] + " ID=\"" + uRecord[0] + "\"] not found.");
                        }
                    }
                }
            }
        }
        return output;
    }

    /**
     * Обработка файла "comments.txt" (этап 3) - конвертация комментариев.
     *
     * @param uRecord массив строк, содержащий данные комментария.
     */
    private boolean parse_comments_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 11) {
            return false;
        }
        String[] moduleName = {null, "news", "news", "stat", "foto", "loads", null, null};
        String[] tableName = {null, "news_comments", "news_comments", "stat_comments", null, "loads_comments", null, null};
        String[] columnName = {null, "new_id", "new_id", "entity_id", null, "entity_id", null, null};
        int moduleID = parseInt(uRecord[1], 0);
        if (moduleID == 0) {
            return false;
        }
        if (VERSION < 6 && (moduleID >= tableName.length || tableName[moduleID] == null)) {
            return false;
        }
        if (VERSION >= 6 && (moduleID >= moduleName.length || moduleName[moduleID] == null)) {
            return false;
        }
        int entity_id = parseInt(uRecord[2], 0);
        if (entity_id == 0) {
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
        query.addItem(column, entity_id);
        query.addItem("name", name);
        query.addItem("message", addslashes((VERSION > 2 ? "" : "[" + parseDateToString(uRecord[4]) + "]: ") + uRecord[10]));
        query.addItem("ip", uRecord[9]);
        query.addItem("mail", uRecord[7]);
        if (VERSION > 2) { // 1.1.9 и новее
            query.addItem("date", parseDate(uRecord[4]));
        }
        if (VERSION > 4) { // 1.3 RC и новее
            query.addItem("user_id", uRecord[12] != null && !uRecord[12].isEmpty() ? uRecord[12] : "0");
        }
        if (VERSION >= 6) { // 2.1 RC7 и новее
            query.addItem("module", moduleName[moduleID]);
        }
        if (VERSION >= 9) { // 2.5 RC1 и новее
            query.addItem("premoder", "confirmed");
        }
        if (VERSION >= 10) { // AtomM 4 и новее
            query.addItem("parent_id", uRecord[13] != null && !uRecord[13].isEmpty() ? uRecord[13] : "0");
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "users.txt" (этап 3) - конвертация пользователей.
     *
     * @param uRecord массив строк, содержащий данные о пользователе.
     */
    private boolean parse_users_stage3(List sqlData, String[] uRecord) {
        if (uRecord.length < 24) {
            return false;
        }
        String posts = "0";
        String status = "1";
        String last_visit = "";
        String locked = "0";
        String activation = uRecord[23].equals("0") ? "" : uRecord[23];
        String[] str = uUsersMeta.get(uRecord[0]);
        if (str != null) {
            posts = str[9];
            status = ((NO_GROUPS || parseInt(str[2], 1) <= 4) ? str[2] : "1");
            if (!status.equalsIgnoreCase(str[2])) {
                println("WARNING: Group for user \"" + uRecord[0] + "\" changed from \"" + str[2] + "\" to \"" + status + "\".");
            }
            last_visit = str[18];
            locked = ((Integer.parseInt(str[2]) == 255) ? "1" : str[3]);
        } else {
            posts = "0";
            status = "1";
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
        query.addItem("id", (String) uUsers.get(uRecord[0]));
        query.addItem("name", addslashes(uRecord[0]));
        query.addItem("passw", addslashes(uRecord[2]));
        query.addItem("email", addslashes(uRecord[7]));
        query.addItem("url", addslashes(uRecord[8]));
        query.addItem("icq", addslashes(uRecord[9]));
        query.addItem("signature", addslashes(uRecord[13]));
        query.addItem("puttime", parseDate(uRecord[15]));
        if (!last_visit.isEmpty() && !last_visit.equals("0")) {
            query.addItem("last_visit", parseDate(last_visit));
        }
        query.addItem("posts", posts);
        query.addItem("status", status);
        query.addItem("locked", locked);
        query.addItem("activation", activation);
        query.addItem("state", addslashes(uRecord[14]));
        if (VERSION > 1) { // 1.1.8 beta и новее
            query.addItem("warnings", "0");
            query.addItem("ban_expire", "0");
        }
        if (VERSION > 2) { // 1.1.9 и новее
            query.addItem("pol", (uRecord[6].equals("2") ? "f" : "m"));
            query.addItem("jabber", "");
            query.addItem("city", addslashes(uRecord[12]));
            query.addItem("telephone", "");
            query.addItem("byear", byear);
            query.addItem("bmonth", bmonth);
            query.addItem("bday", bday);
        }
        sqlData.add(query);
        return true;
    }

    /**
     * Обработка файла "users.txt" (этап 2) - копирование аватаров пользователей
     * и конвертация ссылок на аватары.
     *
     * @param uRecord массив строк, содержащий данные о пользователе.
     */
    private boolean parse_users_stage2(List sqlData, String[] uRecord) {
        if (uRecord.length < 4) {
            return false;
        }
        if (!uRecord[3].isEmpty() && !uRecord[3].equals("0")) {
            File file = null;
            BufferedImage imag = null;
            String path = trimUrl(uRecord[3]);
            if (path != null) {
                path = path.replace("/", DS);
                file = new File(DUMP + path);
            } else {
                String[] paths = uRecord[3].split("/");
                if (paths.length > 1) {
                    file = new File(AVATAR_TABLES + paths[paths.length - 2] + DS + paths[paths.length - 1]);
                }
            }
            try {
                if (file != null && file.exists()) {
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
                    updateLink(uRecord[3], (VERSION >= 10 ? "/data/" : "/sys/") + "avatars/" + uUsers.get(uRecord[0]) + ".jpg");
                    ImageIO.write(imag, "JPEG", new_file);
                    if (file != null && file.exists()) {
                        new_file.setLastModified(file.lastModified());
                    }
                } else {
                    println("WARNING: " + (file != null ? "File \"" + file.getName() + "\" not found. " : "") + "Avatar for user \"" + uRecord[0] + "\" not created.");
                }
            } catch (Exception e) {
                println("WARNING: Avatar for user \"" + uRecord[0] + "\" not created.");
            }
        }
        return true;
    }

    /**
     * Загрузка служебной информации о пользователях (файл "ugen.txt").
     *
     * @return <tt>true</tt> если информация загружена, иначе <tt>false</tt>.
     */
    public boolean initUsers() {
        println("Load \"ugen.txt\"...");
        String filename = DUMP_TABLES + "ugen.txt";
        if (!new File(filename).exists()) {
            println("ERROR: File \"ugen.txt\" not found.");
            return false;
        }
        uUsersMeta = new TreeMap<String, String[]>();
        uUsers = new TreeMap<String, String>();
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
            println("ERROR: File \"ugen.txt\" is empty.");
            return false;
        }
        return true;
    }

    /**
     * Этап 1: Загрузка файлов бекапа Ucoz, инициализация массива ссылок,
     * первоначальный парсинг ссылок.
     *
     * @param parseAll если <tt>true</tt>, то загружаются все файлы, иначе
     * анализируется параметр parse;
     * @param parse массив, содержащий указания о необходимости загрузки
     * конкретных файлов.
     */
    public void loadBackups(boolean parseAll, boolean[] parse) {
        uLinks = new TreeMap();
        // Загрузка файлов бекапа Ucoz
        for (int i = 0; i < uTables.length; i++) {
            if (!parseAll && !parse[i]) {
                for (int j = 0; j < uTables[i].length; j++) {
                    uData[i][j] = null;
                }
            } else {
                for (int j = 0; j < uTables[i].length; j++) {
                    int id = 0;
                    println("Load \"" + uTables[i][j] + ".txt\"...");
                    String uDumpFile = DUMP_TABLES + uTables[i][j] + ".txt";
                    if (!new File(uDumpFile).exists()) {
                        println("WARNING: File \"" + uTables[i][j] + ".txt\" not found.");
                        uData[i][j] = null;
                        continue;
                    }

                    try {
                        uData[i][j] = new ArrayList();
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(uDumpFile), "UTF-8"));
                        String line = null;
                        String line_int = "";
                        while ((line = br.readLine()) != null) {
                            if (line.lastIndexOf("\\") == line.length() - 1) {
                                line_int += line.substring(0, line.length() - 1) + "<br />";
                                continue;
                            } else {
                                line_int += line;
                            }
                            if (VERSION <= 3) { // Старше 1.2 beta
                                if (uTables[i][j].equals("loads") || uTables[i][j].equals("publ")
                                        || uTables[i][j].equals("news") || uTables[i][j].equals("blog")
                                        || uTables[i][j].equals("faq")) {
                                    line_int = line_int.replace("<!--IMG", "<!--I");
                                }
                            }
                            line_int = line_int.replace("\\|", "&#124;");
                            String[] uRecord = line_int.split("\\|");
                            int new_id = (!uTables[i][j].equals("users")) ? parseInt(uRecord[0], -1) : -1;
                            for (int k = 0; k < uRecord.length; k++) {
                                if (uRecord[k].contains("&#124;")) {
                                    uRecord[k] = uRecord[k].replace("&#124;", "|");
                                }
                                // Инициализация массива ссылок
                                if (SITE_NAME_OLD != null) {
                                    int next = 0;
                                    String[] keys = getOldSites();
                                    boolean found = false;
                                    int found_index = -1;
                                    int start = uRecord[k].length();
                                    for (int l = 0; l < keys.length; l++) {
                                        int pos = uRecord[k].indexOf(keys[l], next);
                                        if (pos >= 0 && pos < start) {
                                            found = true;
                                            found_index = l;
                                            start = pos;
                                        }
                                    }
                                    while (found && next >= 0 && next < uRecord[k].length()) {
                                        String[] breaks = {" ", "\"", "'", "<", ":", ",", "(", ")", "[", "]", ";"};
                                        int end = uRecord[k].length();
                                        for (String pos_break : breaks) {
                                            int pos = uRecord[k].indexOf(pos_break, start + keys[found_index].length());
                                            if (pos > 0 && pos < end) {
                                                end = pos;
                                            }
                                        }
                                        String key = uRecord[k].substring(start, end);
                                        if (key != null) {
                                            updateLink(key, null);
                                        }
                                        next = end + 1;
                                        found = false;
                                        found_index = -1;
                                        start = uRecord[k].length();
                                        for (int l = 0; l < keys.length; l++) {
                                            int pos = uRecord[k].indexOf(keys[l], next);
                                            if (pos >= 0 && pos < start) {
                                                found = true;
                                                found_index = l;
                                                start = pos;
                                            }
                                        }
                                    }
                                }
                            }
                            line_int = "";
                            // Сортировка по ID
                            if (new_id >= 0 && new_id < id) {
                                int ii = uData[i][j].size();
                                while (ii > 0) {
                                    String[] record = (String[]) uData[i][j].get(ii - 1);
                                    int old_id = parseInt(record[0], 0);
                                    if (old_id < new_id) {
                                        break;
                                    }
                                    ii--;
                                }
                                uData[i][j].add(ii, uRecord);
                            } else {
                                uData[i][j].add(uRecord);
                            }
                            if (new_id >= id) {
                                id = new_id;
                            }
                        }
                    } catch (Exception e) {
                        println(e.getMessage());
                    }
                }
            }
        }
        // Инициализация данных для первоначального парсинга ссылок
        TreeMap<String, Object[]> uPosts = null;
        if (VERSION >= 10) { // AtomM 4 и новее
            uThemesMeta = getMeta("forum");
        }
        if (VERSION < 6) { // Старше Fapos 2.1 RC7
            uPosts = new TreeMap<String, Object[]>();
            TreeMap<String, Integer> uThemes = new TreeMap<String, Integer>();
            TreeMap<String, String[]> uPostsMeta = getMeta("forump");
            int max_id = -1;
            for (String id : uPostsMeta.keySet()) {
                if (parseInt(id, -1) > max_id) {
                    max_id = parseInt(id, -1);
                }
            }
            for (int i = 0; i < max_id; i++) {
                String id = Integer.toString(i);
                if (uPostsMeta.containsKey(id)) {
                    String[] uRecord = uPostsMeta.get(id);
                    if (uRecord != null && uRecord.length > 1 && !uRecord[1].isEmpty()) {
                        String theme_id = uRecord[1];
                        int countPosts = 0;
                        if (uThemes.containsKey(theme_id)) {
                            countPosts = uThemes.get(theme_id);
                        }
                        countPosts++;
                        uThemes.put(theme_id, countPosts);
                        Object[] entry = {countPosts, theme_id};
                        uPosts.put(id, entry);
                    }
                }
            }
        }
        
        // Первоначальный парсинг ссылок
        if (uLinks != null) {
            for (String key : uLinks.keySet()) {
                if (uLinks.get(key) == null) {
                    String value = null;
                    String[] record = null;
                    if (key.contains("?")) {
                        record = key.substring(0, key.indexOf("?")).split("/");
                    } else {
                        record = key.split("/");
                    }
                    if (record.length > 3) {
                        String url_id = (record.length > 4) ? record[record.length - 1] : null;
                        /*
                        String url_id = null;
                        if (record.length == 5) {
                            url_id = record[4];
                        } else if (record.length == 6) {
                            url_id = record[5];
                        }
                         */
                        if (record[3].equals("forum")) {
                            value = parse_forum_stage1(url_id, getMeta("fr_fr"), uPosts);
                        } else if (record[3].equals("load")) {
                            value = parse_load_stage1(url_id, getMeta("ld_ld"));
                        } else if (record[3].equals("publ")) {
                            value = parse_publ_stage1(url_id, getMeta("pu_pu"));
                        } else if (record[3].equals("news")) {
                            value = parse_news_stage1(url_id, getMeta("nw_nw"), 1);
                        } else if (record[3].equals("blog")) {
                            value = parse_news_stage1(url_id, getMeta("bl_bl"), 2);
                        } else if (record[3].equals("faq")) {
                            value = parse_faq_stage1(url_id, getMeta("fq_fq"));
                        } else if (record[3].equals("index")) {
                            value = parse_index_stage1(url_id);
                        }
                    } else if (record.length == 3) {
                        value = "";
                    }
                    updateLink(key, value);
                }
            }
        }
    }

    /**
     * Этап 2: Обработка ссылок и работа с файлами (вложения, материалы, аватары
     * и т.п.).
     */
    public void linksUpdate() {
        atmData = new ArrayList<ArrayList>();
        for (int i = 0; i < uTables.length; i++) {
            atmData.add(new ArrayList());
            if (uTables[i].length > 0) {
                if (uTables[i][0].equals("users")) {
                    // Инициализация папки для работы с аватарами
                    createDir("avatars", "Avatars not supported.", true);
                } else if (uTables[i][0].equals("fr_fr") || uTables[i][0].equals("forum") || uTables[i][0].equals("forump")) {
                    // Инициализация папок для работы с вложениями
                    File attachDir = new File(FORUM_ATTACH_TABLES);
                    if (attachDir.exists()) {
                        createDir("files" + DS + "forum", "Attachments for forum not supported.", true);
                        if (VERSION >= 11) {
                            createDir("images" + DS + "forum", "Attachments for forum not supported.", true);
                        }
                    } else {
                        println("WARNING: Path \"" + FORUM_ATTACH_TABLES + "\" not found. Attachments for forum not supported.");
                    }
                } else if (uTables[i][0].equals("ld_ld") || uTables[i][0].equals("loads")) {
                    // Инициализация папки для работы с файлами
                    File attachDir = new File(LOADS_TABLES);
                    if (attachDir.exists()) {
                        createDir("files" + DS + "loads", "Loads not supported.", false);
                    } else {
                        println("WARNING: Path \"" + LOADS_TABLES + "\" not found. Loads not supported.");
                    }
                } else if (uTables[i][0].equals("pu_pu") || uTables[i][0].equals("publ")) {
                    if (VERSION > 2) { // 1.1.9 и новее
                        // Инициализация папок для работы с вложениями
                        File attachDir = new File(PUBL_ATTACH_TABLES);
                        if (attachDir.exists()) {
                            createDir("files" + DS + "stat", "Attachments for publications not supported.", true);
                            if (VERSION >= 11) {
                                createDir("images" + DS + "stat", "Attachments for publications not supported.", true);
                            }
                        } else {
                            println("WARNING: Path \"" + PUBL_ATTACH_TABLES + "\" not found. Attachments for publications not supported.");
                        }
                    }
                } else if (uTables[i][0].equals("nw_nw") || uTables[i][0].equals("bl_bl") || uTables[i][0].equals("fq_fq")
                        || uTables[i][0].equals("news") || uTables[i][0].equals("blog") || uTables[i][0].equals("faq")) {
                    if (VERSION > 2) { // 1.1.9 и новее
                        // Инициализация папок для работы с вложениями
                        File newsAttachDir = new File(NEWS_ATTACH_TABLES);
                        if (!newsAttachDir.exists()) {
                            println("WARNING: Path \"" + NEWS_ATTACH_TABLES + "\" not found. Attachments for news not supported.");
                        }
                        File blogAttachDir = new File(BLOG_ATTACH_TABLES);
                        if (!blogAttachDir.exists()) {
                            println("WARNING: Path \"" + BLOG_ATTACH_TABLES + "\" not found. Attachments for blogs not supported.");
                        }
                        File faqAttachDir = new File(FAQ_ATTACH_TABLES);
                        if (!faqAttachDir.exists()) {
                            println("WARNING: Path \"" + FAQ_ATTACH_TABLES + "\" not found. Attachments for FAQs not supported.");
                        }
                        // Результирующая папка
                        createDir("files" + DS + "news", "Attachments for news not supported.", true);
                        if (VERSION >= 11) {
                            createDir("images" + DS + "news", "Attachments for news not supported.", true);
                        }
                    }
                }
            }
            for (int j = 0; j < uTables[i].length; j++) {
                if (uData[i][j] == null) {
                    continue;
                }
                for (int k = 0; k < uData[i][j].size(); k++) {
                    String[] uRecord = (String[]) uData[i][j].get(k);
                    if (uTables[i][j].equals("users")) {
                        parse_users_stage2(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("fr_fr")) {
                        // parse_fr_fr_stage2(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("forump")) {
                        parse_forump_stage2(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("ld_ld")) {
                        // parse_ld_ld_stage2(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("loads")) {
                        String str = parse_loads_stage2(atmData.get(i), uRecord);
                        if (str != null && !str.isEmpty()) {
                            uRecord[22] = str;
                            uData[i][j].set(k, uRecord);
                        }
                    } else if (uTables[i][j].equals("publ") || uTables[i][j].equals("news")
                            || uTables[i][j].equals("blog") || uTables[i][j].equals("faq")) {
                        int mode = 0;
                        if (uTables[i][j].equals("publ")) {
                            mode = 0;
                        } else if (uTables[i][j].equals("news")) {
                            mode = 1;
                        } else if (uTables[i][j].equals("blog")) {
                            mode = 2;
                        } else if (uTables[i][j].equals("faq")) {
                            mode = 3;
                        }
                        String str = parse_news_stage2(atmData.get(i), uRecord, mode);
                        if (str != null && !str.isEmpty()) {
                            int text_index = -1;
                            if (uTables[i][j].equals("publ")) {
                                text_index = 20;
                            } else if (uTables[i][j].equals("news") || uTables[i][j].equals("blog")) {
                                text_index = 13;
                            } else if (uTables[i][j].equals("faq")) {
                                text_index = 12;
                            }
                            if (text_index >= 0 && text_index < uRecord.length) {
                                uRecord[text_index] += "<br />" + str;
                                uData[i][j].set(k, uRecord);
                            }
                        }
                    } else if (uTables[i][j].equals("forum")) {
                        //sqlRecord = parse_forum_stage3(uRecord);
                    } else if (uTables[i][j].equals("pu_pu")) {
                        //sqlRecord = parse_pu_pu_stage3(uRecord);
                    } else if (uTables[i][j].equals("nw_nw")) {
                        //sqlRecord = parse_nw_nw_stage3(uRecord, 1);
                    } else if (uTables[i][j].equals("bl_bl")) {
                        //sqlRecord = parse_nw_nw_stage3(uRecord, 2);
                    } else if (uTables[i][j].equals("fq_fq")) {
                        //sqlRecord = parse_nw_nw_stage3(uRecord, 3);
                    } else if (uTables[i][j].equals("comments")) {
                        //sqlRecord = parse_comments_stage3(uRecord);
                    }
                }
            }
        }
        println("Check bad links...");
        if (uLinks != null) {
            for (String key : uLinks.keySet()) {
                if (!key.endsWith("/") && uLinks.get(key) == null) {
                    boolean exist = false;
                    // Проверка существования файла и его копирование при наличии
                    String path = trimUrl(key);
                    if (path != null) {
                        path = path.replace("/", DS);
                        File file = new File(DUMP + path);
                        if (file.exists() && file.isFile()) {
                            String filename = path.substring(0, path.lastIndexOf(DS));
                            if (createDir(filename, "", false)) {
                                if (copyFile(DUMP + path, path)) {
                                    String value = "/" + path.replace(DS, "/");
                                    updateLink(key, value);
                                    exist = true;
                                }
                            }
                        }
                    }
                    if (!exist) {
                        println("WARNING: URL \"" + key + "\" not modified.");
                    }
                }
            }
        }
    }

    /**
     * Этап 3: Генерация SQL-запросов.
     *
     * @return список, в котором лежат списки с SQL-запросами.
     */
    public ArrayList getSQL() {
        boolean forumEmpty = false;
        boolean loadsEmpty = false;
        boolean publEmpty = false;
        boolean newsEmpty = false;
        boolean addNews = false;
        boolean addBlog = false;
        boolean addFAQ = false;
        for (int i = 0; i < uTables.length; i++) {
            ArrayList emptySql = new ArrayList();
            for (int j = 0; j < uTables[i].length; j++) {
                if (uData[i][j] == null) {
                    continue;
                }
                if (uTables[i][j].equals("users")) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + "users"));
                    }
                } else if (uTables[i][j].equals("fr_fr") || uTables[i][j].equals("forum") || uTables[i][j].equals("forump")) {
                    if (!forumEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add(new TruncateQuery(PREF + "forum_cat"));
                            emptySql.add(new TruncateQuery(PREF + "forums"));
                            emptySql.add(new TruncateQuery(PREF + "themes"));
                            emptySql.add(new TruncateQuery(PREF + "forum_attaches"));
                            emptySql.add(new TruncateQuery(PREF + "posts"));
                        }
                        forumEmpty = true;
                    }
                } else if (uTables[i][j].equals("ld_ld") || uTables[i][j].equals("loads")) {
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
                            query.addItem("id", "1");
                            query.addItem("type", "text");
                            query.addItem("name", "");
                            query.addItem("label", "Ссылка для скачивания архива с другого сервера");
                            query.addItem("size", "255");
                            query.addItem("params", "a:0:{}");
                            emptySql.add(query);
                        }
                        loadsEmpty = true;
                    }
                } else if (uTables[i][j].equals("pu_pu") || uTables[i][j].equals("publ")) {
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
                        query.addItem("id", "1");
                        query.addItem("type", "text");
                        query.addItem("size", "255");
                        query.addItem("params", "a:0:{}");
                        if (VERSION < 10) { // Старше AtomM 4
                            query.addItem("name", "link");
                            query.addItem("label", "Ссылка на источник материала");
                            query.setTable(PREF + "stat_add_fields");
                        } else {
                            query.addItem("label", "link");
                            query.addItem("field_id", "1");
                            query.addItem("module", "stat");
                        }
                        emptySql.add(query);
                        publEmpty = true;
                    }
                } else if (uTables[i][j].equals("nw_nw") || uTables[i][j].equals("bl_bl") || uTables[i][j].equals("fq_fq")
                        || uTables[i][j].equals("news") || uTables[i][j].equals("blog") || uTables[i][j].equals("faq")) {
                    if (!newsEmpty) {
                        if (!NO_EMPTY) {
                            emptySql.add(new TruncateQuery(PREF + "news"));
                            emptySql.add(new TruncateQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections")));
                        }
                        newsEmpty = true;
                    }
                    if (uTables[i][j].equals("nw_nw") || uTables[i][j].equals("news")) {
                        if (!addNews) {
                            InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "1");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0");
                            query.addItem("title", "Новости");
                            if (VERSION <= 3) {
                                query.addItem("class", "section");
                            }
                            emptySql.add(query);
                            query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "4");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "1");
                            query.addItem("title", "Без категории");
                            if (VERSION <= 3) {
                                query.addItem("class", "category");
                            }
                            emptySql.add(query);
                            addNews = true;
                        }
                    } else if (uTables[i][j].equals("bl_bl") || uTables[i][j].equals("blog")) {
                        if (!addBlog) {
                            InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "2");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0");
                            query.addItem("title", "Блоги");
                            if (VERSION <= 3) {
                                query.addItem("class", "section");
                            }
                            emptySql.add(query);
                            query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "5");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "2");
                            query.addItem("title", "Без категории");
                            if (VERSION <= 3) {
                                query.addItem("class", "category");
                            }
                            emptySql.add(query);
                            addBlog = true;
                        }
                    } else if (uTables[i][j].equals("fq_fq") || uTables[i][j].equals("faq")) {
                        if (!addFAQ) {
                            InsertQuery query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "3");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "0");
                            query.addItem("title", "FAQ");
                            if (VERSION <= 3) {
                                query.addItem("class", "section");
                            }
                            emptySql.add(query);
                            query = new InsertQuery(PREF + (VERSION >= 10 ? "news_categories" : "news_sections"));
                            query.addItem("id", "6");
                            query.addItem((VERSION > 3 ? "`parent_id`" : "`section_id`"), "3");
                            query.addItem("title", "Без категории");
                            if (VERSION <= 3) {
                                query.addItem("class", "category");
                            }
                            emptySql.add(query);
                            addFAQ = true;
                        }
                    }
                } else if (uTables[i][j].equals("comments")) {
                    if (!NO_EMPTY) {
                        emptySql.add(new TruncateQuery(PREF + "loads_comments"));
                        emptySql.add(new TruncateQuery(PREF + "stat_comments"));
                        emptySql.add(new TruncateQuery(PREF + "news_comments"));
                    }
                }
                for (int k = 0; k < uData[i][j].size(); k++) {
                    String[] uRecord = (String[]) uData[i][j].get(k);
                    if (uTables[i][j].equals("users")) {
                        parse_users_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("fr_fr")) {
                        parse_fr_fr_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("forum")) {
                        parse_forum_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("forump")) {
                        parse_forump_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("ld_ld")) {
                        parse_ld_ld_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("loads")) {
                        parse_loads_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("pu_pu")) {
                        parse_pu_pu_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("publ")) {
                        parse_publ_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("nw_nw")) {
                        parse_nw_nw_stage3(atmData.get(i), uRecord, 1);
                    } else if (uTables[i][j].equals("news")) {
                        parse_news_stage3(atmData.get(i), uRecord, 1);
                    } else if (uTables[i][j].equals("bl_bl")) {
                        parse_nw_nw_stage3(atmData.get(i), uRecord, 2);
                    } else if (uTables[i][j].equals("blog")) {
                        parse_news_stage3(atmData.get(i), uRecord, 2);
                    } else if (uTables[i][j].equals("fq_fq")) {
                        parse_nw_nw_stage3(atmData.get(i), uRecord, 3);
                    } else if (uTables[i][j].equals("faq")) {
                        parse_faq_stage3(atmData.get(i), uRecord);
                    } else if (uTables[i][j].equals("comments")) {
                        parse_comments_stage3(atmData.get(i), uRecord);
                    }
                }
                atmData.get(i).add("\r\n -- ---------------------------------- -- \r\n");
            }
            if (emptySql.size() > 0) {
                emptySql.add("\r\n -- ---------------------------------- -- \r\n");
                atmData.get(i).addAll(0, emptySql);
            }
        }
        return atmData;
    }
}

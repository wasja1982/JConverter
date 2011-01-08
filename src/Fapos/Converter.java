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
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
//import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;
//import javax.imageio.IIOImage;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.imageio.ImageIO;
//import javax.imageio.ImageWriteParam;
//import javax.imageio.ImageWriter;
//import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
//import javax.imageio.stream.FileImageOutputStream;

public class Converter {
    protected class uBlock {
        public int pos = 0;
        public int type = -1;

        public uBlock(int pos, int type) {
            this.pos = pos;
            this.type = type;
        }
    }

    String DS = File.separator;
    // Dir with ucoz dump
    String DUMP = "";
    // dir with txt files in ucoz dump
    String DUMP_TABLES = "";
    String ATTACH_TABLES = "";
    String AVATAR_TABLES = "";
    // prefix for tables in data base, if exists
    String PREF = "";

    public Converter(String DUMP) {
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
        ATTACH_TABLES = this.DUMP + "_fr" + DS;
        AVATAR_TABLES = this.DUMP + "avatar" + DS;
    }
    
    public Converter(String DUMP, String PREF) {
        this.DUMP = DUMP + DS;
        DUMP_TABLES = this.DUMP + "_s1" + DS;
        ATTACH_TABLES = this.DUMP + "_fr" + DS;
        AVATAR_TABLES = this.DUMP + "avatar" + DS;
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

        for (int i = 1; i <= 5; i++) {
            String str = String.format( "<!--IMG%d-->", i );
            if (parse.contains( str )) {
                int start = parse.indexOf( str );
                int end = parse.indexOf( str, start + 1 );
                parse = parse.substring(0, start) + String.format("{IMAGE%d}", i) + parse.substring(end + str.length());
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
        return uUsers;
    }

    /**
     * Work for forum.txt
     *
     */
    private String parse_forum(String[] uRecord, TreeMap uUsers) {
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
     * Work for forump.txt
     *
     */
    private String parse_forump(String[] uRecord, TreeMap uUsers, ArrayList uAttachDir) {
        String id_author = "0";
        try {
            Object ob = uUsers.get(uRecord[6]);
            id_author = ((ob != null) && !((String)ob).isEmpty()) ? (String)ob : "0";
        } catch (Exception e) {
            id_author = "0";
        }
        String attach = "0";
        String output_a = "";
        if (uRecord.length > 9 && uRecord[10] != null && !((String)uRecord[10]).isEmpty()) {
            String[] attaches = ((String)uRecord[10]).split("`");
            attach = (attaches.length > 0) ? "1" : "0";
            if (uAttachDir.size() > 0) {
                for (int i = 0; i < attaches.length; i++) {
                    if (attaches[i].length() > 0) {
                        int pos = attaches[i].lastIndexOf('.');
                        String ext = (pos >= 0) ? attaches[i].substring( pos ) : "";
                        String is_image = (ext.equalsIgnoreCase( ".png" ) || ext.equalsIgnoreCase( ".jpg" ) ||
                                           ext.equalsIgnoreCase( ".gif" ) || ext.equalsIgnoreCase( ".jpeg" )) ? "1" : "0";
                        String filename = attachesName(uRecord[0], Integer.toString( i + 1 ), uRecord[2], ext);
                        boolean exist = false;
                        File file = null;
                        for (int j = 0; j < uAttachDir.size(); j++) {
                            if (is_image.equals("1") && attaches[i].substring(0, 1).equalsIgnoreCase( "s" )) {
                                file = new File( ((String)uAttachDir.get(j)) + attaches[i].substring(1) );
                            } else {
                                file = new File( ((String)uAttachDir.get(j)) + attaches[i] );
                            }
                            if (file.exists()) {
                                try {
                                    File new_file = new File( "files" + DS + "forum" + DS + filename );
                                    FileChannel ic = new FileInputStream( file ).getChannel();
                                    FileChannel oc = new FileOutputStream( new_file ).getChannel();
                                    ic.transferTo(0, ic.size(), oc);
                                    ic.close();
                                    oc.close();
                                    new_file.setLastModified( file.lastModified() );
                                    exist = true;
                                    break;
                                } catch (Exception e) {
                                    exist = false;
                                    break;
                                }
                            }
                        }
                        if (exist) {
                            String sql = String.format("INSERT INTO `" + PREF + "forum_attaches`"
                                                     + " (`post_id`, `theme_id`, `user_id`, `attach_number`, `filename`, `size`, `date`, `is_image`) VALUES"
                                                     + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');\r\n",
                                                       uRecord[0], uRecord[1], id_author, Integer.toString( i + 1), filename, Long.toString(file.length()), parseDate(uRecord[2]), is_image);
                            output_a += sql;
                        } else {
                            System.out.println( "WARNING: Attachment \"" + attaches[i] + "\" not found." );
                        }
                    }
                }
            }
        }

        /*
        try {
            attach = ((uRecord[10] != null) && !((String)uRecord[10]).isEmpty()) ? "1" : "0";
        } catch (Exception e) {
            attach = "0";
        }
        */

        /*
        if (uRecord[0].equals("1140")) {
            System.out.println("1140");
        }
        if (uRecord[0].equals("1395")) {
            System.out.println("1395");
        }
        */
        String output = String.format("INSERT INTO `" + PREF + "posts`"
            + " (`id`, `id_theme`, `time`, `message`, `id_author`, `edittime`, `attaches`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uRecord[0], uRecord[1], parseDate(uRecord[2]), addslashes(uRecord[4]), id_author, parseDate(uRecord[9]), attach);
        return output_a + output;
    }

    /**
     * Work for fr_fr.txt
     *
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
                + " (`id`, `in_cat`, `title`, `last_theme_id`, `themes`, `posts`, `description`) VALUES"
                + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                uRecord[0], uRecord[1], addslashes(uRecord[5]), last_theme_id, themes, posts, addslashes(uRecord[6]));
        }
        return output;
    }

    /**
     * Work for nw_nw.txt
     *
     */
    private String parse_nw_nw(String[] uRecord, TreeMap uCategories) {
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
    private String parse_pu_pu(String[] uRecord, TreeMap uCategories) {

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
        String id_author = "0";
        try {
            Object obj = uUsers.get(uRecord[10]);
            id_author = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            id_author = "0";
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
        String id_author = "0";
        try {
            Object obj = uUsers.get(uRecord[10]);
            id_author = ((obj != null) && !((String)obj).isEmpty()) ? (String)obj : "0";
        } catch (Exception e) {
            id_author = "0";
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
        if (!((String)uRecord[3]).isEmpty() && !((String)uRecord[3]).equals( "0" )) {
            String[] path = ((String)uRecord[3]).split( "/" );
            if (path.length > 1) {
                File file = new File( AVATAR_TABLES + path[path.length - 2] + DS + path[path.length - 1] );
                if (file.exists()) {
                    try {
                        BufferedImage imag = ImageIO.read( file );
                        if( imag.getColorModel().getTransparency() != Transparency.OPAQUE) {
                            int w = imag.getWidth();
                            int h = imag.getHeight();
                            BufferedImage image2 = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
                            image2.createGraphics().drawImage( imag, 0, 0, image2.getWidth(), image2.getHeight(), java.awt.Color.WHITE, null );
                            imag = image2;
                        }
/*
                        Iterator iter = ImageIO.getImageWritersByFormatName( "JPEG" );
                        if (iter.hasNext()) {
                            ImageWriter writer = (ImageWriter) iter.next();
                            ImageWriteParam iwp = writer.getDefaultWriteParam();
                            iwp.setCompressionMode( ImageWriteParam.MODE_EXPLICIT );
                            iwp.setCompressionQuality( 1.0f );
                            File new_file = new File( "avatars" + DS + uUsers.get(uRecord[0]) + ".jpg" );
                            writer.setOutput( new FileImageOutputStream( new_file ) );
                            writer.write(null, new IIOImage( imag, null, null ), iwp);
                            new_file.setLastModified( file.lastModified() );
                        } else {
                            System.out.println( "WARNING: Avatar for user \"" + uRecord[0] + "\" not created." );
                        }
*/
                        File new_file = new File( "avatars" + DS + uUsers.get(uRecord[0]) + ".jpg" );
                        ImageIO.write( imag, "JPEG", new_file );
                        new_file.setLastModified( file.lastModified() );
                    } catch (Exception e) {
                        System.out.println( "WARNING: Avatar for user \"" + uRecord[0] + "\" not created." );
                    }
                } else {
                    System.out.println( "WARNING: File \"" + file.getName() + "\" not found. Avatar for user \"" + uRecord[0] + "\" not created." );
                }
            }
        }

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
        String output = String.format("INSERT INTO `" + PREF + "users`"
            + " (`id`, `name`, `passw`, `email`, `url`, `icq`, `signature`, `puttime`, `posts`, `status`) VALUES"
            + " ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            uUsers.get(uRecord[0]), addslashes(uRecord[0]), addslashes(uRecord[2]), addslashes(uRecord[7]), addslashes(uRecord[8]), addslashes(uRecord[9]), addslashes(uRecord[13]), parseDate(uRecord[15]), posts, status);
        return output;
    }

    public ArrayList getSQL(String[] uTables) {
        ArrayList uAttachDir = new ArrayList();
        try {
            File attachDir = new File(ATTACH_TABLES);
            if (attachDir.exists()) {
                String[] attach_cats = attachDir.list();
                for (int i = 0; i < attach_cats.length; i++) {
                    uAttachDir.add(ATTACH_TABLES + attach_cats[i] + DS);
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
        TreeMap uNewsCategories = new TreeMap(); // $_SESSION['news_categories'] = array();
        TreeMap uStatCategories = new TreeMap(); // $_SESSION['stat_categories'] = array();
        System.out.println( "Load \"users.txt\"..." );
        TreeMap uUsers = getUserIds();
        if (uUsers == null) {
            System.out.println( "ERROR: File \"users.txt\" not found." );
            return null;
        } else if (uUsers.size() == 0) {
            System.out.println( "ERROR: File \"users.txt\" is empty." );
            return null;
        }

        TreeMap uUsersMeta = new TreeMap();
        System.out.println( "Load \"ugen.txt\"..." );
        String filename = DUMP_TABLES + "ugen.txt";
        if (!new File(filename).exists()) {
            System.out.println( "ERROR: File \"ugen.txt\" not found." );
            return null;
        }
        try {
            BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( filename ), "UTF-8" ) );
            String line = null;
            while ( ( line = br.readLine () ) != null ) {
                String [] uUsersData = line.split( "\\|" );
                if (uUsersData.length < 2 || uUsersData[1].isEmpty()) continue;
                uUsersMeta.put(uUsersData[1], uUsersData);
            }
        }
        catch (Exception e) {}
        if (uUsersMeta.size() == 0) {
            System.out.println( "ERROR: File \"ugen.txt\" is empty." );
            return null;
        }

        // Geting ucoz files and work for each
        ArrayList FpsData = new ArrayList();
        ArrayList emptySql = new ArrayList();
        for (int i = 0; i < uTables.length; i++) {
            System.out.println( "Load \"" + uTables[i] + ".txt\"..." );
            String uDumpFile = DUMP_TABLES + uTables[i] + ".txt";
            if (!new File(uDumpFile).exists()) {
                System.out.println( "WARNING: File \"" + uTables[i] + ".txt\" not found." );
                continue;
            } else {
                if (uTables[i].equals("users")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "users`;" );
                } else if (uTables[i].equals("fr_fr")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "forum_cat`;" );
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "forums`;" );
                } else if (uTables[i].equals("forum")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "themes`;" );
                } else if (uTables[i].equals("forump")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "forum_attaches`;" );
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "posts`;" );
                } else if (uTables[i].equals("nw_nw")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "news_sections`;" );
                } else if (uTables[i].equals("news")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "news`;" );
                } else if (uTables[i].equals("pu_pu")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "stat_sections`;" );
                } else if (uTables[i].equals("publ")) {
                    emptySql.add( "TRUNCATE TABLE `" + PREF + "stat`;" );
                }
            }

            // Geting data from file and work for each record
            try {
                BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream ( uDumpFile ), "UTF-8" ) );
                String line = null;
                while ( ( line = br.readLine () ) != null ) {
                    String [] uRecord = line.split( "\\|" );
                    if (uRecord.length < 2) continue;
                    String sqlRecord = null;
                    if (uTables[i].equals("users")) sqlRecord = parse_users(uRecord, uUsers, uUsersMeta);
                    else if (uTables[i].equals("fr_fr")) sqlRecord = parse_fr_fr(uRecord);
                    else if (uTables[i].equals("forum")) sqlRecord = parse_forum(uRecord, uUsers);
                    else if (uTables[i].equals("forump")) sqlRecord = parse_forump(uRecord, uUsers, uAttachDir);
                    else if (uTables[i].equals("nw_nw")) sqlRecord = parse_nw_nw(uRecord, uNewsCategories);
                    else if (uTables[i].equals("news")) sqlRecord = parse_news(uRecord, uUsers, uNewsCategories);
                    else if (uTables[i].equals("pu_pu")) sqlRecord = parse_pu_pu(uRecord, uStatCategories);
                    else if (uTables[i].equals("publ")) sqlRecord = parse_publ(uRecord, uUsers, uStatCategories);
                    if (sqlRecord == null) continue;
                    FpsData.add(sqlRecord);
                }
            }
            catch (Exception e) {}
            FpsData.add("\r\n -- ---------------------------------- -- \r\n");
        }
        if (emptySql.size() > 0) {
            emptySql.add("\r\n -- ---------------------------------- -- \r\n");
            FpsData.addAll(0, emptySql);
        }
        return FpsData;
    }
}

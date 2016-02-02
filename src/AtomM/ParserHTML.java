package AtomM;

import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ParserHTML extends DefaultHandler {
    public String text = "";
    public boolean parseSmile = false;
    public Stack span = new Stack();
    public Stack div = new Stack();
    //Начало документа
    @Override
    public void startDocument() throws SAXException{
        try {
            text = "";
            if (span != null) span.clear();
            if (div != null) div.clear();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    //Окончание документа
    @Override
    public void endDocument() throws SAXException{
    }

    //Начало элемента
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attr) throws SAXException{
        try {
            String[][] smile = {{"wink", ";)"},
                                {"wacko", "%)"},
                                {"tongue", ":p"},
                                {"surprised", ":o"},
                                {"smile", ":)"},
                                {"sad", ":("},
                                {"happy", "^_^"},
                                {"dry", "<_<"},
                                {"cry", ":'("},
                                {"cool", "B)"},
                                {"biggrin", ":D"},
                                {"angry", ">("},
                                {"zonked", ":zonked:"},
                                {"unsure", ":unsure:"},
                                {"tired", ":tired:"},
                                {"thebox", ":thebox:"},
                                {"rollseyes", ":rollseyes:"},
                                {"quizzical", ":quizzical:"},
                                {"nervous", ":nervous:"},
                                {"lol", ":lol:"},
                                {"heart", ":heart:"},
                                {"grin", ":grin:"},
                                {"glad", ":glad:"},
                                {"furious", ":furious:"},
                                {"frown", ":frown:"},
                                {"cyclops", ":cyclops:"},
                                {"confused", ":confused:"},
                                {"aghast", ":aghast:"},
                                {"aggrieved", ":aggrieved:"}};

            if (qName.equalsIgnoreCase("br") || qName.equalsIgnoreCase("p")) text += "\r\n";
            else if (qName.equalsIgnoreCase("b") || qName.equalsIgnoreCase("strong")) text += "[b]";
            else if (qName.equalsIgnoreCase("i") || qName.equalsIgnoreCase("em")) text += "[i]";
            else if (qName.equalsIgnoreCase("u")) text += "[u]";
            else if (qName.equalsIgnoreCase("s")) text += "[s]";
            else if (qName.equalsIgnoreCase("ul")) text += "[list]";
            else if (qName.equalsIgnoreCase("ol")) text += "[list=1]";
            else if (qName.equalsIgnoreCase("img")) {
                boolean change = false;
                if (parseSmile) {
                    if (attr.getValue("alt") != null) {
                        for (int i = 0; i < smile.length; i++) {
                            if (attr.getValue("alt").equals(smile[i][0])) {
                                text += " " + smile[i][1] + " ";
                                change = true;
                                break;
                            }
                        }
                    }
                    if (!change && attr.getValue("src") != null) {
                        text += "[img]" + attr.getValue("src") + "[/img]";
                    }
                } else if (attr.getValue("src") != null) {
                    text += "[img]" + attr.getValue("src") + "[/img]";
                }
            } else if (qName.equalsIgnoreCase("span")) {
                String style = attr.getValue("style");
                if (style != null) {
                    if (style.contains("color:")) {
                        String color_style = style.substring(style.indexOf("color:") + 6);
                        if (color_style.indexOf(";") >= 0) color_style = color_style.substring(0, color_style.indexOf(";"));
                        text += "[color=" + color_style + "]";
                        if (span != null) span.push(0);
                    } else if (style.contains("font-size:")) {
                        String size_style = style.substring(style.indexOf("font-size:") + 10);
                        if (size_style.indexOf(";") >= 0) size_style = size_style.substring(0, size_style.indexOf(";")).trim();
                        if (size_style.indexOf("pt") >= 0) size_style = size_style.substring(0, size_style.indexOf("pt")).trim();
                        int size = 0;
                        try {
                            size = Integer.parseInt(size_style);
                        } catch (NumberFormatException e) {}
                        if (size <= 12) size_style = "10";
                        else if (size <= 17) size_style = "15";
                        else if (size <= 22) size_style = "20";
                        else size_style = "25";
                        text += "[size=" + size_style + "]";
                        if (span != null) span.push(1);
                    } else {
                        if (span != null) span.push(2);
                    }
                }
            } else if (qName.equalsIgnoreCase("div")) {
                String align = attr.getValue("align");
                if (align != null) {
                    if (align.contains("right")) {
                        text += "[right]";
                        if (div != null) div.push(0);
                    } else if (align.contains("left")) {
                        text += "[left]";
                        if (div != null) div.push(1);
                    } else if (align.contains("center")) {
                        text += "[center]";
                        if (div != null) div.push(2);
                    } else {
                        if (div != null) div.push(3);
                    }
                }
            } else if (qName.equalsIgnoreCase("a") && attr.getValue("href") != null) {
                text += "[url=" + attr.getValue("href") + "]";
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    //Окончание элемента
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException{
        try {
            if (qName.equalsIgnoreCase("b") || qName.equalsIgnoreCase("strong")) text += "[/b]";
            else if (qName.equalsIgnoreCase("i") || qName.equalsIgnoreCase("em")) text += "[/i]";
            else if (qName.equalsIgnoreCase("u")) text += "[/u]";
            else if (qName.equalsIgnoreCase("s")) text += "[/s]";
            else if (qName.equalsIgnoreCase("ul")) text += "[/list]";
            else if (qName.equalsIgnoreCase("ol")) text += "[/list]";
            else if (qName.equalsIgnoreCase("span")) {
                if (span != null && !span.empty()) {
                    Integer style = (Integer)span.pop();
                    if (style != null) {
                        if (style == 0) {
                            text += "[/color]";
                        } else if (style == 1) {
                            text += "[/size]";
                        }
                    }
                }
            } else if (qName.equalsIgnoreCase("div")) {
                if (div != null && !div.empty()) {
                    Integer align = (Integer)div.pop();
                    if (align != null) {
                        if (align == 0) {
                            text += "[/right]";
                        } else if (align == 1) {
                            text += "[/left]";
                        } else if (align == 2) {
                            text += "[/center]";
                        }
                    }
                }
            } else if (qName.equalsIgnoreCase("a")) {
                text += "[/url]";
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            text += String.copyValueOf(ch, start, length);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Override
    public void error (SAXParseException e) {
//        System.out.println(e.toString());
    }

    @Override
    public void warning (SAXParseException e) {
//        System.out.println(e.toString());
    }

    @Override
    public void fatalError (SAXParseException e) {
//        System.out.println(e.toString());
    }
}
// http://khpi-iip.mipk.kharkiv.edu/library/extent/prog/iipXML/usax.html
// Кавычки в ссылках
// Кавычки в цитате
// Фиксированный размер шрифта

package Fapos;

import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ParserHTML extends DefaultHandler {
    String text = "";
    Stack span = new Stack();
    Stack div = new Stack();
    //Начало документа
    @Override
    public void startDocument() throws SAXException{
        text = "";
        span.clear();
        div.clear();
    }

    //Окончание документа
    @Override
    public void endDocument() throws SAXException{
    }

    //Начало элемента
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attr) throws SAXException{
        if (qName.equalsIgnoreCase("br") || qName.equalsIgnoreCase("p")) text += "\r\n";
        else if (qName.equalsIgnoreCase("b")) text += "[b]";
        else if (qName.equalsIgnoreCase("i")) text += "[i]";
        else if (qName.equalsIgnoreCase("u")) text += "[u]";
        else if (qName.equalsIgnoreCase("s")) text += "[s]";
        else if (qName.equalsIgnoreCase("ul")) text += "[list]";
        else if (qName.equalsIgnoreCase("ol")) text += "[list=1]";
        else if (qName.equalsIgnoreCase("img")) {
            text += "[img]" + attr.getValue("src") + "[/img]";
        } else if (qName.equalsIgnoreCase("span")) {
            String style = attr.getValue("style");
            if (style != null) {
                if (style.contains("color:")) {
                    String color_style = style.substring(style.indexOf("color:") + 6);
                    if (color_style.indexOf(";") >= 0) color_style = color_style.substring(0, color_style.indexOf(";"));
                    text += "[color=" + color_style + "]";
                    span.push(0);
                } else if (style.contains("font-size:")) {
                    String size_style = style.substring(style.indexOf("font-size:") + 10);
                    if (size_style.indexOf(";") >= 0) size_style = size_style.substring(0, size_style.indexOf(";"));
                    if (size_style.indexOf("pt") >= 0) size_style = size_style.substring(0, size_style.indexOf("pt"));
                    int size = Integer.parseInt(size_style);
                    if (size <= 12) size_style = "10";
                    else if (size <= 17) size_style = "15";
                    else if (size <= 22) size_style = "20";
                    else size_style = "25";
                    text += "[size=" + size_style + "]";
                    span.push(1);
                } else {
                    span.push(2);
                }
            }
        } else if (qName.equalsIgnoreCase("div")) {
            String align = attr.getValue("align");
            if (align != null) {
                if (align.contains("right")) {
                    text += "[right]";
                    div.push(0);
                } else if (align.contains("left")) {
                    text += "[left]";
                    div.push(1);
                } else if (align.contains("center")) {
                    text += "[center]";
                    div.push(2);
                } else {
                    div.push(3);
                }
            }
        } else if (qName.equalsIgnoreCase("a")) {
            text += "[url=" + attr.getValue("href") + "]";
        }
    }

    //Окончание элемента
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException{
        if (qName.equalsIgnoreCase("b")) text += "[/b]";
        else if (qName.equalsIgnoreCase("i")) text += "[/i]";
        else if (qName.equalsIgnoreCase("u")) text += "[/u]";
        else if (qName.equalsIgnoreCase("s")) text += "[/s]";
        else if (qName.equalsIgnoreCase("ul")) text += "[/list]";
        else if (qName.equalsIgnoreCase("ol")) text += "[/list]";
        else if (qName.equalsIgnoreCase("span")) {
            Integer style = (Integer)span.pop();
            if (style != null) {
                if (style == 0) {
                    text += "[/color]";
                } else if (style == 1) {
                    text += "[/size]";
                }
            }
        } else if (qName.equalsIgnoreCase("div")) {
            Integer align = (Integer)div.pop();
            if (align != null) {
                if (align == 0) {
                    text += "[/right]";
                } else if (align == 1) {
                    text += "[/left]";
                } else if (align == 2) {
                    text += "[/center]";
                } else {
                    div.push(3);
                }
            }
        } else if (qName.equalsIgnoreCase("a")) {
            text += "[/url]";
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        text += String.copyValueOf(ch, start, length);
    }

    @Override
    public void error (SAXParseException e) {
        System.out.println(e.toString());
    }

    @Override
    public void warning (SAXParseException e) {
        System.out.println(e.toString());
    }

    @Override
    public void fatalError (SAXParseException e) {
        System.out.println(e.toString());
    }
}
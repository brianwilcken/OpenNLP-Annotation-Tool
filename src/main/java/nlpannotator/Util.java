/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nlpannotator;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Mountain
 */
public class Util {

    public static ArrayList<Offset> overlapsAdd(Offset offset_uniq, ArrayList<Offset> coordinates) {
        int start = offset_uniq.getStart(), end = offset_uniq.getEnd();
        ArrayList<Offset> removeOffset = new ArrayList();
        for (Offset offset_data : coordinates) {
            if (offset_data.getStart() <= offset_uniq.getStart() && offset_data.getEnd() >= offset_uniq.getStart()) {
                removeOffset.add(offset_data);//no longer needed
                start = Math.min(start, offset_data.getStart());
                end = Math.max(end, offset_data.getEnd());
            }
            if (offset_data.getStart() <= offset_uniq.getEnd() && offset_data.getEnd() >= offset_uniq.getEnd()) {
                removeOffset.add(offset_data);//no longer needed
                start = Math.min(start, offset_data.getStart());
                end = Math.max(end, offset_data.getEnd());
            }
            if (offset_data.getStart() >= offset_uniq.getStart() && offset_data.getEnd() <= offset_uniq.getEnd()) {
                removeOffset.add(offset_data);
                start = Math.min(start, offset_data.getStart());
                end = Math.max(end, offset_data.getEnd());
            }
            if (offset_data.getStart() < offset_uniq.getStart() && offset_data.getEnd() < offset_uniq.getEnd()) {
                //don't add
            }

        }
        removeOffset.forEach((toRemove) -> {
            coordinates.remove(toRemove);
        });

        coordinates.add(new Offset(start, end));

        return coordinates;
    }

    public static String readData(String filePath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.out.println("File reading exception" + e);
        }
        return content;
    }

    public static Path writeData(String data, String pathFile) {

        byte[] strToBytes = data.getBytes();
        try {
            Path path = Paths.get(pathFile);
            return Files.write(path, strToBytes);
        } catch (IOException e) {
            System.out.println("File writing exception" + e);
        }
        return null;
    }

    public static void refreshColor(JTextPane playground) {
        StyledDocument style = playground.getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet defaultcolor = sc.getEmptySet();
        style.setCharacterAttributes(0, playground.getText().length(), defaultcolor, true);
    }

    public static void colorIt(ArrayList<Offset> coordinates, JTextPane playground) {
        StyledDocument style = playground.getStyledDocument();

        coordinates.forEach((offset_data) -> {
            int start = offset_data.getStart(), end = offset_data.getEnd();
            AttributeSet oldSet = style.getCharacterElement(end - 1).getAttributes();
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet red = sc.addAttribute(oldSet, StyleConstants.Foreground, Color.BLUE);
            style.setCharacterAttributes(start, (end - start), red, true);
        });
    }
    
    public static ArrayList<Offset> removeSingleChar(ArrayList<Offset> coordinates){
        ArrayList<Offset> removeOffset = new ArrayList();
        for(Offset offset_data:coordinates)
        {
            if((offset_data.getEnd()-offset_data.getStart())<2)
                removeOffset.add(offset_data);
        }
        removeOffset.forEach((toRemove) -> {
            coordinates.remove(toRemove);
        });
        
        return coordinates;
    }
    
    public static String makeEvenSpaces(String in)
    {
        String out="";
        String[] data=in.split(" +");
        
        for(String words : data){
            out=out+" "+words;
        }
        return out;
    }

}

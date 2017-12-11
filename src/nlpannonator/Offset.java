/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nlpannonator;

/**
 *
 * @author Mountain
 */
public class Offset implements Comparable<Offset> {

    private final int start;
    private final int end;

    public Offset(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof Offset)) {
            return false;
        }
        Offset offset = (Offset) ob;
        return (start == offset.getStart()) && (end == offset.getEnd());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.start;
        hash = 59 * hash + this.end;
        return hash;
    }

    @Override
    public int compareTo(Offset o) {
        return this.getStart()-o.getStart();
    }

}

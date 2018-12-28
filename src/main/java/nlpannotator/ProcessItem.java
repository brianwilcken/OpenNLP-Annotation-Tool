package nlpannotator;

import java.util.UUID;

public class ProcessItem {
    private String id;
    private String text;

    public ProcessItem(String text) {
        this.text = text;
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

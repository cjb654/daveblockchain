package pdc.model;

import java.net.URL;
import java.util.ArrayList;

public class ExternalItem extends DataItem {

    private URL url;
    
    public ExternalItem(int index, String name, NodeData originalNode, ArrayList<NodeData> storedOn, URL url, byte[] data) {
        super(index, name, originalNode, storedOn);
        this.type = "external";
        this.url = url;
    }

    public URL getUrl() { return url; }
}

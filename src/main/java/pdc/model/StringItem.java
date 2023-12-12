package pdc.model;

import java.util.ArrayList;

public class StringItem extends DataItem {
        
    private String message;

    public StringItem(int index, String name, NodeData originalNode, ArrayList<NodeData> storedOn, String message) {
        super(index, name, originalNode, storedOn);
        this.type = "Text";
        this.message = message;
        this.checksum = getChecksumValue(message.getBytes());
    }

    @Override
    public String toPrint() {
        return type+" Data with ID: "+Long.toString(id)+" sent from Node: "+originalNode.getName()+" is stored in index: "+index;
    }

    @Override
    public String toPrintDetailed() {
        return "Data type: "+type+", ID: "+Long.toString(id)+", sent from Node: "+originalNode.getName()+", stored in index: "+index+". Text value: "+message;
    }

    
    public String getMessage(){ return message; }
}

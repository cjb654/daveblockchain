package pdc.model;

import java.util.ArrayList;

public class FilePart extends FilePartRef {
    private int index;
    private byte[] data;

    public FilePart(NodeData originalNode, ArrayList<NodeData> storedOn, int index, byte[] data) {
		super(storedOn);
        this.originalNode = originalNode;
        this.index = index;
        this.data = data;
        this.checksum = getChecksumValue(data);
        this.id = id/(index+1);
    }
    
    public int getIndex() { return index; }
    public byte[] getData() { return data; }
	public int getLength() { return data.length; }
}

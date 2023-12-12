package pdc.model;

import java.util.ArrayList;

public class FileItem extends DataItem {
    private long[] fileParts;
    private long fileSize;

    public FileItem(int index, String name, NodeData originalNode, ArrayList<NodeData> storedOn, long[] fileParts, byte[] data) {
        super(index, name, originalNode, storedOn);
        this.type = "file";
        this.fileParts = fileParts;
        this.fileSize = data.length;
        this.checksum = getChecksumValue(data);
    }

    @Override
    public String toPrintDetailed() {
        return "Data type: "+type+", ID: "+Long.toString(id)+", sent from Node ID: "+originalNode.getName()+", stored in index: "+index+". It is "+(fileParts.length*64)+" bytes";
    }

    public long[] getFileParts() { return fileParts; }
    public long getFileSize() { return fileSize; }
    public long getNumFileParts() { return fileParts.length; }
}

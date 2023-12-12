package pdc.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;

public class DataItem extends SendData {
    protected long id;
    protected String type;
    protected String name;
    protected int index;
    protected ArrayList<NodeData> storedOn;
    protected String checksum;

    public DataItem(ArrayList<NodeData> storedOn) {
        id = Instant.now().getEpochSecond();
        this.storedOn = storedOn;
    }

    public DataItem(int index, String name, NodeData originalNode, ArrayList<NodeData> storedOn) {
        id = Instant.now().getEpochSecond();
        this.name = name;
        this.index = index;
        this.originalNode = originalNode;
        this.storedOn = storedOn;
    }

    public String getChecksumValue(byte[] bytes) {
		MessageDigest digest = null;
		try { digest = MessageDigest.getInstance("MD5"); } catch(NoSuchAlgorithmException e) { System.out.println("why did you change this"); e.printStackTrace(); }
		byte[] hash = digest.digest(bytes);

		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

    public String toPrint() {
        return "Data type: "+type+", ID: "+Long.toString(id)+", sent from Node: "+originalNode.getName()+", stored in index: "+index;
    }

    public String toPrintDetailed() {
        return "Data type: "+type+", ID: "+Long.toString(id)+", sent from Node ID: "+originalNode.getName()+", stored in index: "+index;
    }

    public String toReceivedPrint() {
        return "New Data received. ID: "+Long.toString(id)+" from Node ID: "+originalNode.getName()+" is stored in index: "+index;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public ArrayList<NodeData> getStoredOn() { return storedOn; }
    public void removeNodeFromStoredOn(long id) {
        for(int i=0;i<storedOn.size();i++) if(storedOn.get(i).getId() == id) {storedOn.remove(i); }
    }
    public String getChecksum() { return checksum; }
}

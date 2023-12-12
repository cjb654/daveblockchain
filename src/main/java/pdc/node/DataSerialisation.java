package pdc.node;

import pdc.model.*;
import pdc.model.ServiceItem.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class DataSerialisation {
    
    private NodeData node;
    private ArrayList<NodeData> nodes;
    private byte serialisedSend[] = null;
    private ArrayList<DataItem> dataItems;
    private ArrayList<FilePartRef> fileParts;
    private NodeData pinging;
    
    private DatagramSocket dsReceive;
    private DatagramSocket dsSend;    
    
    public DataSerialisation( NodeData nodeData) { 
        dataItems = new ArrayList<DataItem>();
        fileParts = new ArrayList<FilePartRef>();
        nodes = new ArrayList<NodeData>();
        node = nodeData;
        nodes.add(node);

        try { 
            dsReceive  = new DatagramSocket(node.getPort(), InetAddress.getByName("0.0.0.0"));
            dsSend = new DatagramSocket();
        } 
        catch(SocketException e) { System.out.println("error creating sockets at setup"); e.printStackTrace();} 
        catch (UnknownHostException e) { e.printStackTrace(); }

    } 

    public void populateArrayLists(SetupData s) {
        dataItems = s.getData();
        nodes = s.getNodes();
    }

    public void setup(SetupData setup) {
        populateArrayLists(setup);
       
        pauseNetwork();
        HelloMessage helloMessage = new HelloMessage(node);
        sendNetwork(helloMessage);
        unpauseNetwork();
    }

    public void connectNetworks(InetAddress ip, int port) {
        NodeData requestNode = new NodeData("network connection request", ip, port);
        ConnectData connect = new ConnectData(node, dataItems, nodes, fileParts);
        serialisedSend = serialiseData(connect);
        sendNode(connect, requestNode);
        
        System.out.println("waiting on connection");
        SetupData s = null;
        while(true) {
            SendData data = receiveData();
            if(data == null) { continue; }
            if(data instanceof SetupData) {
                s = ((SetupData) data);
                populateArrayLists(s);
                return;
            }
        }
    }

    public void sendNetwork(SendData data) {
        serialisedSend = serialiseData(data); 
        
        for ( NodeData n : nodes ) { 
            if(n.getId() == node.getId()) { continue; }
            sendNode(serialisedSend, n); 
        }
    }

    public void sendNode(SendData data, NodeData node) {
        serialisedSend = serialiseData(data);
        sendNode(serialisedSend, node);
    }

    public void sendNode(byte[] send, NodeData node) {  
        DatagramPacket sendPacket = new DatagramPacket(send, send.length, node.getIp(), node.getPort());
        try { dsSend.send(sendPacket); } catch(IOException e) { System.out.println("IO error sending data"); e.printStackTrace(); }
    }

    public int getNextIndex() { return dataItems.size(); }

    public void addAndSendDataItem(DataItem data) {
        dataItems.add(data);
        DataItem ref = data;
        for(NodeData n : nodes) {
            if(n.getId() == node.getId()) { continue; }
            if(doesNodeStore(data,n)) { sendNode(data, n);}
            else { sendNode(ref, n);}
        }
    }

    public void addDataItem(DataItem data) {
        if(doesNodeStore(data,node)) {
            dataItems.add(data);
        }
        else{
            dataItems.add((DataItem) data);
        }
    }

    public void addFilePart(FilePartRef part) { 
        FilePartRef ref = part;
        if(part instanceof FilePart) { fileParts.add((FilePart) ref); }
        else { fileParts.add(ref); }
    }

    public void addAndSendFilePart(FilePart filePart) {
        fileParts.add(filePart);
        for(NodeData n : nodes) {
            if(n.getId() == node.getId()) { continue; }
            if(doesNodeStore(filePart,n)) { sendNode(filePart, n);}
            else { sendNode((FilePartRef) filePart, n); }
        }
    }

    public boolean doIStoreFilePart(FilePartRef part) {
        if(part instanceof FilePart) { return true; }
        return false;
    }

    public void getFileParts(FileItem item) {
        FileManager files = new FileManager();
        FilePart[] fileParts = new FilePart[(int) item.getNumFileParts()];
        byte[] data = new byte[(int) item.getFileSize()];
        int pos = 0;
        for(int i=0;i<fileParts.length;i++) {
            FilePartRef part = getFilePart(item.getFileParts()[i]);
            if(doIStoreFilePart(part)) { fileParts[i] = (FilePart) part; }
            //if(true) { fileParts[i] = (FilePart) part; }
            else {
                FilePart filePart = askForFilePart(part);
                if(getFilePart(filePart.getId()).getChecksum().equals(getChecksumValue(filePart.getData()))) {
                    System.out.println("ERROR");
                }
                fileParts[i] = filePart;
                try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
        Arrays.sort(fileParts, Comparator.comparingInt(FilePart::getIndex));
        int counter=0;
        for(int i=0;i<fileParts.length;i++) {
            byte[] partData = fileParts[i].getData();
            for(int j=0;j<partData.length;j++) {
                data[counter] = partData[j];
                counter++;
            }
        }
        files.writeFile(node.getName(), item.getName(), data);
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

    public boolean doesNodeStore(DataItem data, NodeData testNode) {
        for(NodeData n : data.getStoredOn()) {
            if(n.getId() == testNode.getId()) { return true; }
        }
        return false;
    }

    public SendData receiveData() {
        byte[] receive = new byte[65535];
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);
        try { dsReceive.receive(receivePacket); } catch(IOException e) { System.out.println("IO error receiving data"); e.printStackTrace(); }

        Object obj = deserialiseData(receivePacket);

        if(!isSafeData(obj)) { return null; }
        SendData data = (SendData) obj;
        if(didISend(data)) return null;
        receive = new byte[65535];

        return data;
    }

    public boolean didISend(SendData data) { if(data.getOriginalNode().getId() == node.getId()) { return true; } else return false; }
    
    public void setupNode(NodeData send) {
        SetupData s = new SetupData(node, dataItems, nodes);
        sendNode(s, send);
    }

    public ArrayList<NodeData> newNodesToStoreOn() {  
        int number = 3;
        if(nodes.size() < number) { return nodes; }
        else {
            ArrayList<NodeData> returnNodes = new ArrayList<NodeData>();
            returnNodes.add(node);
            number = (nodes.size()/4) + (nodes.size() % 4);
            int index;
            for(int i=1;i<number;i++) {
                while(true) {
                    index = ThreadLocalRandom.current().nextInt(0, nodes.size());
                    if(nodes.get(index).getId() == node.getId()) { continue; } else { break; }
                }
                returnNodes.add(nodes.get(index));
            }
            return returnNodes;
        }
    }

    public void setupNetwork(InetAddress ip, int port, ArrayList<DataItem> addItems, ArrayList<NodeData> addNodes) {
        dataItems.addAll(addItems); 
        nodes.addAll(addNodes); 
        //new indexes
        for(int i=0; i<dataItems.size();i++) { dataItems.get(i).setIndex(i); }
        SetupData s = new SetupData(node, dataItems, nodes);
        sendNetwork(s);
    }

    public void addNewNode(HelloMessage data) {
        NodeData add = data.getOriginalNode();
        nodes.add(add);
    }

    public void askToJoinNetwork(NodeData askNode) {        
        sendNode(new SetupDataRequest(node), askNode);
        //loop this for valid
        SendData data = null;
        while(true) {
            System.out.println("waiting on all messages");
            data = receiveData();
            if(data == null) { continue; }
            if(data instanceof SetupData) { break; }
        }

        SetupData s = ((SetupData) data);
        s.getNodes().add(node);
        setup(s); 
    }

    public DataItem getDataItem(int index) {
        if(index > dataItems.size()-1) { return null; }
        DataItem item = dataItems.get(index);
        if(doesNodeStore(dataItems.get(index),node)) { 
            if(item instanceof FileItem) { getFileParts((FileItem) item); }
            return dataItems.get(index); 
        }
        else { 
            item = askForData(index); 
            if(item instanceof FileItem) { 
                getFileParts((FileItem) item); 
            }
            return item;
        }
    }

    public void sendDataItem(NodeData to, int index) {
        DataRequestReply reply = new DataRequestReply(node, dataItems.get(index));
        sendNode(reply, to);
    }

    public void sendFilePart(NodeData to, long id) {
        FilePartRequestReply reply = new FilePartRequestReply(node, getFilePart(id));
        sendNode(reply, to);
    }

    public DataItem askForData(int dataIndex) {
        DataRequest request = new DataRequest(node, dataIndex);
        NodeData askNode = null;
        while(true) {
            ArrayList<NodeData> storedOn = dataItems.get(dataIndex).getStoredOn();
            int index = ThreadLocalRandom.current().nextInt(0, storedOn.size());
            askNode = nodes.get(index);
            if(askNode.getId() == node.getId()) { continue; }
            break;
        }
        sendNode(request, askNode);

        SendData data = null;
        while(true) { 
            data = receiveData();
            if(data instanceof DataRequestReply) { break; }
        }
        DataItem dataItem = ((DataRequestReply) data).getDataItem();
        if(dataItem == null) { return null; }
        if(dataItem.getIndex() == dataIndex) { return dataItem; }
        else return null;
    }

    public FilePart askForFilePart(FilePartRef filePartRef) {
        FilePartRequest request = new FilePartRequest(node, filePartRef.getId());
        NodeData askNode = null;
        while(true) {
            ArrayList<NodeData> storedOn = getFilePart(filePartRef.getId()).getStoredOn();
            int index = ThreadLocalRandom.current().nextInt(0, storedOn.size());
            askNode = nodes.get(index);
            if(askNode.getId() == node.getId()) { continue; }
            break;
        }
        sendNode(request, askNode);

        SendData data = null;
        while(true) { 
            data = receiveData();
            if(data instanceof FilePartRequestReply) { break; }
        }
        FilePartRef filePart = ((FilePartRequestReply) data).getFilePart();
        if(filePart == null) { return null; }
        return (FilePart) filePart;
    }

    public FilePartRef getFilePart(long id) {
        for(FilePartRef p : fileParts) { if(p.getId() == id) return p; }
        return null;
    }

    public ArrayList<DataItem> getDataItems() {
        return dataItems;
    }

    public ArrayList<NodeData> getNodes() {
        return nodes;
    }

    public void removeNode(NodeData removeNode) {
        if(isInNetwork(removeNode)) { return; }
        long removeId = removeNode.getId();
        for(int i=0; i<nodes.size();i++) {
            if (nodes.get(i).getId() == removeId){
                nodes.remove(i);
                break;
            }
        }
        for(DataItem item : dataItems) {
            if(item.getClass() == DataItem.class) { continue; }
            ArrayList<NodeData> storedOn = item.getStoredOn();
            for(NodeData n : storedOn) if(n.getId() == node.getId()) {
                item.removeNodeFromStoredOn(node.getId());
            }
        }
        for(FilePartRef part : fileParts) {
            if(part.getClass() == FilePart.class) { continue; }
            ArrayList<NodeData> storedOn = part.getStoredOn();
            for(NodeData n : storedOn) if(n.getId() == node.getId()) {
                part.removeNodeFromStoredOn(node.getId());
            }
        }
        System.out.println("node not found with this id");
    }

    public void removeNodeFromNetwork(NodeData removeNode) {
        NodeRemovalMessage remove = new NodeRemovalMessage(node, removeNode);
        sendNetwork(remove);
        removeNode(removeNode);
    }

    public void shutdownNode() {
        GoodbyeMessage goodbyeMessage = new GoodbyeMessage(node);
        sendNetwork(goodbyeMessage);

        dsReceive.close();
		dsSend.close();
    }

    public void redistributeData(DataItem item) { 
        ArrayList<NodeData> storedOn = item.getStoredOn();
        for(NodeData n : storedOn) if(n.getId() == node.getId()) {
            item.removeNodeFromStoredOn(n.getId());
        }

    }

    public boolean isInNetwork(NodeData sendNode) {
        for(NodeData node : nodes) {
            if(node.getId() == sendNode.getId()) { return true; }
        }
        return false;
    }

    public void resetData() {
        nodes = new ArrayList<NodeData>();
        nodes.add(node);
        dataItems = new ArrayList<DataItem>();
        fileParts = new ArrayList<FilePartRef>();
    }

    public UnpauseMessage waitForUnpause() {
        SendData data = receiveData();
        if(data == null) { return null; }

        //If unpause message item
        if(data instanceof HelloMessage) { 
            HelloMessage hello = (HelloMessage) data;
            addNewNode(hello);
            return null;
        }
        if(data instanceof GoodbyeMessage) { 
            if(isInNetwork(data.getOriginalNode())) { return null; }
            GoodbyeMessage goodbye = (GoodbyeMessage) data;
            removeNode(goodbye.getOriginalNode());
            return null;
        }
        if(data instanceof SetupData) { 
            if(isInNetwork(data.getOriginalNode())) { return null; }
            SetupData setup = (SetupData) data;
            populateArrayLists(setup);
            return null;
        }
        if(data instanceof RebuildRequest ) {
            if(!isInNetwork(data.getOriginalNode())) { return null; }
            RebuildRequest r = (RebuildRequest) data;
            SetupData setup = new SetupData(node, dataItems, nodes);
            sendNode(setup, r.getOriginalNode());
            return null;
        }
        if(data instanceof ValidationRequest ) {
            /*
            ValidationRequest r = (ValidationRequest) data;
            ValidationData d = new ValidationData(node, getChecksums());
            sendNode(d, r.getOriginalNode());
            return null;
            */
            return null;
        }
        if(data instanceof PingMessage) {
            PingMessageReply ping = new PingMessageReply(node);
            sendNode(ping, data.getOriginalNode());
        }
        if(!(data instanceof UnpauseMessage)) { return null; }
        return (UnpauseMessage) data;
    }

    public String[] getChecksums() {
        String checksums[] = new String[dataItems.size()];
        for(int i=0;i<checksums.length;i++) { checksums[i] = dataItems.get(i).getChecksum(); }
        return checksums;
    }

    public void shutdownNetwork() {
        ShutdownMessage shutdownMessage = new ShutdownMessage(node);
        sendNetwork(shutdownMessage);
    }

    public void pauseNetwork() {
        PauseMessage pause = new PauseMessage(node);
        sendNetwork(pause);
    }

    public int getDataItemsLength() { return dataItems.size(); }

    public int getNodesLength() { return nodes.size(); }

    public void unpauseNetwork() {
        UnpauseMessage unpause = new UnpauseMessage(node);
        sendNetwork(unpause);
    }

    public void pingRandomNode() {
        NodeData randNode = getAskNode();
		System.out.println("pinging "+randNode.getName());
        sendNode(new PingMessage(node), randNode);
        pinging = randNode;
    }

    public NodeData responded() { if(pinging == null) { return null; } else { return pinging; } } 

    public void pingNode(long id) {
        for(NodeData n : nodes) {
            if(n.getId() == id) { sendNode(new PingMessage(node), n); return; }
        }
    }

    public void pingReplied() { pinging = null; }

    public NodeData getAskNode() {
        int index;
        NodeData askNode = null;
        while(true) { 
            index = ThreadLocalRandom.current().nextInt(0, nodes.size());
            askNode  = nodes.get(index);
            if(node.getId() == askNode.getId()) { continue; }
            break;
        }
        return askNode;
    }

    public boolean validateRandomNode() {
        NodeData askNode = getAskNode();
        ValidationRequest validationRequest = new ValidationRequest(node);
        sendNode(validationRequest, askNode);

        ValidationData validationData = null;
        while(true) {
            SendData data = receiveData();
            if(data == null) { continue; }
            if(data instanceof ValidationData) { validationData = (ValidationData) data; break; }
        }
        String[] compare = validationData.getChecksums();
        if(compare == null) { return true;}
        if(isDataSame(compare)) { return true; }
        return false;
    }

    public void validateAllData() {
        int counter = 1;
        NodeData askNode = getAskNode();
        ValidationRequest validationRequest = null;
        ValidationData validationData = null;
        for(int i=0;i<nodes.size();i++) {
            if(nodes.get(0).getId() == node.getId()) { continue; }
            askNode  = nodes.get(i);
            sendNode(validationRequest, askNode);

            validationData = null;
            while(true) {
                SendData data = receiveData();
                if(data == null) { continue; }
                if(data instanceof ValidationData) { validationData = (ValidationData) data; break; }
            }
            String[] compare = validationData.getChecksums();
            if(isDataSame(compare)) { counter++; }
        }
        if(counter > nodes.size()/2) { sendNetwork(new SetupData(node, dataItems, nodes));}
        else askForSetup();
    }

    public void askForSetup() {
        NodeData askNode = getAskNode();
        
        sendNode(new SetupDataRequest(node), askNode);
        
        SetupData setup = null;
        while(true) {
            SendData data = receiveData();
            if(data == null) { continue; }
            if(data instanceof SetupData) { setup = (SetupData) data; break; }
        }
        populateArrayLists(setup);;
    }

    public boolean isDataSame(String[] checksums) {
        if(checksums.length < dataItems.size()) { return false; }
        String[] compare = getChecksums();
        for(int i=0;i<checksums.length;i++) {
            if(checksums[i] != compare[i]) { return false; }
        }
        return true;
    }

    public void rebuildDataItems() {
        dataItems = null;

        int index = -1;
        NodeData n = null;
        while(true) {
            index = ThreadLocalRandom.current().nextInt(0,nodes.size());
            n = nodes.get(index);
            if(n.getId() != node.getId()) { break; }
        }
        RebuildRequest rebuild = new RebuildRequest(node);

        sendNode(rebuild, n);

        while(true) {
            SendData data = receiveData();
            if(data == null) { continue; }
            if(data instanceof SetupData) { populateArrayLists((SetupData)data); break; }
        }
    }

    public boolean isSafeData(Object obj) {
        switch(obj.getClass().toString().split("pdc.model.")[1]) {
            case "DataItem":
                return true;
            case "StringItem":
                return true;
            case "NumericalItem":
                return true;
            case "FileItem":
                return true;
            case "FilePart":
                return true;
            case "ServiceItem$SetupData":
                return true;
            case "ServiceItem$HelloMessage":
                return true;
            case "ServiceItem$GoodbyeMessage":
                return true;
            case "ServiceItem$ShutdownMessage":
                return true;
            case "ServiceItem$ConnectData":
                return true;
            case "ServiceItem$PauseMessage":
                return true;
            case "ServiceItem$UnpauseMessage":
                return true;
            case "ServiceItem$RebuildRequest":
                return true;
            case "ServiceItem$ValidationRequest":
                return true;
            case "ServiceItem$ValidationData":
                return true;
            case "ServiceItem$DataRequest":
                return true;
            case "ServiceItem$DataRequestReply":
                return true;
            case "ServiceItem$FilePartRequest":
                return true;
            case "ServiceItem$FilePartRequestReply":
                return true;
            case "ServiceItem$PingMessage":
                return true;
            case "ServiceItem$PingMessageReply":
                return true;
            case "ServiceItem$NodeRemovalMessage":
                return true;
            case "ServiceItem$SetupDataRequest":
                return true;
            default:
                System.out.println("weird data received");
                return false;
        }
    }

    public Object deserialiseData(DatagramPacket pac) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pac.getData());
        ObjectInputStream objectInputStream = null;
        try { objectInputStream = new ObjectInputStream(byteArrayInputStream); } catch(IOException e) { System.out.println("IO error creating OIS"); e.printStackTrace();}
        Object data = null;
        try { data = objectInputStream.readObject(); } 
        catch(IOException e) { System.out.println("IO error reading the object from bytes"); e.printStackTrace(); } 
        catch(ClassNotFoundException e) { System.out.println("Class not found for data"); e.printStackTrace(); }
        try { byteArrayInputStream.close(); objectInputStream.close(); } catch(IOException e) { System.out.println("IO error closing streams"); e.printStackTrace(); }
        return data;
    }

    public byte[] serialiseData(Object send) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream  = null;
        try { objectOutputStream = new ObjectOutputStream(byteArrayOutputStream); } catch(IOException e) { System.out.println("IO error creating OOS"); e.printStackTrace(); }
        try { objectOutputStream.writeObject(send); objectOutputStream.flush(); } catch(IOException e) { System.out.println("IO error writing object to bytes"); e.printStackTrace(); }
        byte[] ret = byteArrayOutputStream.toByteArray();
        try { byteArrayOutputStream.close(); objectOutputStream.close(); } catch(IOException e) { System.out.println("IO error closing streams"); e.printStackTrace(); }
        return ret;
    }
}

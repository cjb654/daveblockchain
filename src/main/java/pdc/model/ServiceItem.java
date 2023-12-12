package pdc.model;

import java.util.ArrayList;

public class ServiceItem {

    public static class SetupData extends SendData {
        
        private ArrayList<DataItem> dataItems;
        private ArrayList<NodeData> nodes;

        public SetupData(NodeData originalNode, ArrayList<DataItem> dataItems, ArrayList<NodeData> nodes) {
            this.originalNode = originalNode;
            this.dataItems = dataItems;
            this.nodes = nodes;
        }

        public ArrayList<DataItem> getData() { return dataItems; }
        public ArrayList<NodeData> getNodes(){ return nodes; }
    }

    public static class SetupDataFiles extends SendData {
        
        private ArrayList<DataItem> dataItems;
        private ArrayList<NodeData> nodes;
        private ArrayList<FilePartRef> fileParts;

        public SetupDataFiles(NodeData originalNode, ArrayList<DataItem> dataItems, ArrayList<NodeData> nodes, ArrayList<FilePartRef> fileParts) {
            this.originalNode = originalNode;
            this.dataItems = dataItems;
            this.nodes = nodes;
            this.fileParts = fileParts;
        }

        public ArrayList<DataItem> getData() { return dataItems; }
        public ArrayList<NodeData> getNodes(){ return nodes; }
        public ArrayList<FilePartRef> getFileParts() { return fileParts; }
    }      

    public static class HelloMessage extends SendData {
        public HelloMessage(NodeData originalNode) { this.originalNode = originalNode; }
    }

    public static class GoodbyeMessage extends SendData {
        public GoodbyeMessage(NodeData originalNode) { this.originalNode = originalNode; }
    }

    public static class ShutdownMessage extends SendData {
        public ShutdownMessage(NodeData originalNode) { this.originalNode = originalNode; }
    }

    public static class ConnectData extends SendData {
        private ArrayList<DataItem> dataItems;
        private ArrayList<NodeData> nodes;
        private ArrayList<FilePartRef> fileParts;

        public ConnectData(NodeData originalNode, ArrayList<DataItem> dataItems, ArrayList<NodeData> nodes, ArrayList<FilePartRef> fileParts) {
            this.originalNode = originalNode;
            this.dataItems = dataItems;
            this.nodes = nodes;
            this.fileParts = fileParts;
        }

        public ArrayList<DataItem> getData() { return dataItems; }
        public ArrayList<NodeData> getNodes(){ return nodes; }
        public ArrayList<FilePartRef> getFileParts() { return fileParts; }
    }

    public static class PauseMessage extends SendData {
        public PauseMessage(NodeData originalNode) {
            this.originalNode = originalNode;
        }
    }

    public static class UnpauseMessage extends SendData {
        public UnpauseMessage(NodeData originalNode) {
            this.originalNode = originalNode;
        }
    }

    public static class RebuildRequest extends SendData {
        public RebuildRequest(NodeData originalNode) {
            this.originalNode = originalNode;
        }
    }

    public static class ValidationRequest extends SendData {
        public ValidationRequest(NodeData originalNode) {
            this.originalNode = originalNode;
        }
    }

    public static class ValidationData extends SendData {
        private String[] checksums;

        public ValidationData(NodeData originalNode, String[] checksums) {
            this.originalNode = originalNode;
            this.checksums = checksums;
        }
        public String[] getChecksums() { return checksums; }
    }

    public static class DataRequest extends SendData {
        private int dataIndex;

        public DataRequest(NodeData originalNode, int dataIndex) {
            this.originalNode = originalNode;
            this.dataIndex = dataIndex;
        }
        public int getDataIndex() { return dataIndex; }
    }

    public static class DataRequestReply extends SendData {
        private DataItem data;

        public DataRequestReply(NodeData originalNode, DataItem data) {
            this.originalNode = originalNode;
            this.data = data;
        }
        public DataItem getDataItem() { return data; }
    }

    public static class FilePartRequest extends SendData {
        private long id;

        public FilePartRequest(NodeData originalNode, long id) {
            this.originalNode = originalNode;
            this.id = id;
        }
        public long getId() { return id; }
    }

    public static class FilePartRequestReply extends SendData {
        private FilePartRef filePart;

        public FilePartRequestReply(NodeData originalNode, FilePartRef filePart) {
            this.originalNode = originalNode;
            this.filePart = filePart;
        }
        public FilePartRef getFilePart() { return filePart; }
    }

    public static class PingMessage extends SendData {
        public PingMessage(NodeData originalNode) { this.originalNode = originalNode;}
    }


    public static class PingMessageReply extends SendData {
        public PingMessageReply(NodeData originalNode) { this.originalNode = originalNode;}
    }

    public static class NodeRemovalMessage extends SendData {
        private NodeData remove;
        public NodeRemovalMessage(NodeData originalNode, NodeData remove) { this.originalNode = originalNode; this.remove = remove; }
        public NodeData getRemoveNode() { return remove; }
    }

    public static class SetupDataRequest extends SendData {
        public SetupDataRequest(NodeData originalNode) { this.originalNode = originalNode; }
    }
}

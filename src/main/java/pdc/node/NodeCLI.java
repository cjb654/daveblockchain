package pdc.node;

import java.util.ArrayList;

public class NodeCLI {
    private Node node;
    private NodeGUI gui;
    private ArrayList<String> commandHistory;

    public NodeCLI(Node node) {
        this.node = node;
        commandHistory = new ArrayList<String>();
    }

    public NodeCLI(Node node, NodeGUI gui) {
        this.node = node;
        this.gui = gui;
        commandHistory = new ArrayList<String>();
    }
    
    public ArrayList<String> getCommandHistory() { return commandHistory; }

    public boolean ifGUI() { if(gui == null) { return false; } else { return true; }}

    public void processCommand(String msg) {
        String[] commandEntry = msg.split(" ");
		String command = commandEntry[0];
		commandHistory.add(msg);
        gui.WriteCommandOutput(msg);

        if(command.equalsIgnoreCase("help")) { if(ifGUI()) { gui.printHelp(commandEntry[1]); } return;}
		if(command.equalsIgnoreCase("history")) { node.printHistory(); return; }
		if(command.equalsIgnoreCase("data")) { node.dataCommand(msg); return; }
		if(command.equalsIgnoreCase("node")) { node.nodeCommand(msg); return; }
		if(command.equalsIgnoreCase("network")) { node.networkCommand(msg); return; }
		if(ifGUI()) { gui.WriteErrorOutput("Wrong syntax - send 'help' for instructions"); }
	}
}

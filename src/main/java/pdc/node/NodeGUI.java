package pdc.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class NodeGUI extends JFrame {
    private Node node;

	JTextPane textPane = new JTextPane();
	JTextField input = new JTextField();
	JScrollPane output = new JScrollPane(textPane);
	JButton button = new JButton("Send");
	DefaultCaret caret = (DefaultCaret) textPane.getCaret();
	StyledDocument doc = textPane.getStyledDocument();
	Style regularStyle;
	Style commandStyle;
	Style errorStyle;

    public NodeGUI(Node node) {
        this.node = node;

		this.setTitle("Node Terminal");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(600, 400);
        this.setLayout(new BorderLayout());

		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        textPane.setEditable(false);
        this.add(output, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        input = new JTextField();
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                node.processInput();
            }
        });

        button = new JButton("Send");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                node.processInput();
            }
        });

        bottomPanel.add(input, BorderLayout.CENTER);
        bottomPanel.add(button, BorderLayout.EAST);
        this.add(bottomPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		regularStyle = doc.addStyle("regular", null);
		commandStyle = doc.addStyle("command", null);
		errorStyle = doc.addStyle("error", null);
		StyleConstants.setForeground(regularStyle, Color.BLACK);
        StyleConstants.setForeground(commandStyle, new Color(0,64,0));
		StyleConstants.setForeground(errorStyle, Color.RED);
		StyleConstants.setItalic(commandStyle, true);

		StyleContext context = new StyleContext();
		Style defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setLineSpacing(defaultStyle, 1.0f);
		textPane.setParagraphAttributes(defaultStyle, true);

		setLocationRelativeTo(null);
		toFront();	
		setVisible(true);
    }

    public String getInput() {
        return input.getText();
    }

    public void clearInput() {
        input.setText("");
    }

    public void WriteOutput(String msg)
	{
		try { doc.insertString(doc.getLength(), msg+"\n", regularStyle); } catch (BadLocationException e) { e.printStackTrace(); }
	}

    public void WriteErrorOutput(String msg)
	{
		try { doc.insertString(doc.getLength(), msg+"\n", errorStyle); } catch (BadLocationException e) { e.printStackTrace(); }
	}

    public void WriteCommandOutput(String msg)
	{
		try { doc.insertString(doc.getLength(), msg+"\n", commandStyle); } catch (BadLocationException e) { e.printStackTrace(); }
	}

    public void printHelp(String type) {
		String line = "---------------------------------------------------------------------";
		//for(int i=0;i<this.getWidth();i++) {line+='-';}
		boolean data = false; if(type.equalsIgnoreCase("data")) {data=true;}
		boolean node = false; if(type.equalsIgnoreCase("node")) {node=true;}
		boolean network = false; if(type.equalsIgnoreCase("network")) {network=true;}
		if(type.equalsIgnoreCase("all")) {data=true;node=true;network=true;}
        WriteOutput(line);
        WriteOutput(" Node Command Help         ");
        WriteOutput(line);
        WriteOutput(" COMMAND --- DESCRIPTION ");
        WriteOutput("help [all|data|node|network] --- View these instructions again");
		WriteOutput("history --- View the command history");
		if(data) {
			WriteOutput("data get [X] --- Retrieve an item at index [X]");
			WriteOutput("data get all --- Retrieve all items");
			WriteOutput("data get all [text|number|file|external] --- Retrieve all items of type [type]");
			WriteOutput("data add [text|number|file|external] [name] [text|number|file path|url] --- Add a new item of type [type] called [name]");
			WriteOutput("data search id [id] --- Find the item with the id [id]");
			WriteOutput("data search name [name] --- Find the item with the name [name]");
			WriteOutput("data search message [message] --- Find the item with the message [message]");
			WriteOutput("data generate [type] [amount] --- Generate [amount] new data items of type [type]");
		}
		if(node) {
			WriteOutput("node get all --- Get all nodes");
			WriteOutput("node get [id] --- Get the node with ID [id]");
			WriteOutput("node ask [ip] [port] --- Send a setup request to node and wait for a connection");
			WriteOutput("node add [ip] [port] --- Add a new awaiting node at [ip]:[port]");
			WriteOutput("node rebuild --- Refresh all items on the node");
			WriteOutput("node down --- Shutdown this node");
			WriteOutput("node wobbly --- Simulate a disconnecting node for 60 seconds");
			WriteOutput("node ping [id] --- Ping the node with ID [id]");
		}
		if(network) {
			WriteOutput("network down --- Shutdown the network"); 
			WriteOutput("network connect [ip] [port] --- Start connection to a network at [ip]:[port]"); //confirmation on both atomic pause for consistency
		}
        WriteOutput(line);
    }
}

package pdc.node;

import pdc.model.*;
import pdc.model.ServiceItem.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node {

	private NodeGUI gui = new NodeGUI(this);
	private NodeCLI cli = new NodeCLI(this, gui);
	private NodeData aboutMe;
    private DataSerialisation messaging;
	
	private volatile boolean quit = false;
	private volatile boolean paused = false;
	private boolean autoaccept = false;
	private ConnectData connect;

	private final int dataValidationTime = 1;
	private final int pingTime = 60;


    public Node(String name, InetAddress ip, int port) throws IOException, SocketException {
		aboutMe = new NodeData(name, ip, port);
        messaging = new DataSerialisation(aboutMe);
    }

	public void startDataValidationCheck() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> validateData(), dataValidationTime, dataValidationTime, TimeUnit.MINUTES);
	}

	public void startPinging() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> pingRandomNode(), pingTime, pingTime, TimeUnit.SECONDS);
	}

	public void pingRandomNode() {
		if(paused) { return; }
		if(messaging.getNodesLength() == 1) { return; }
		NodeData previous = messaging.responded();
		if(previous != null) { unresponsiveNode(previous); }
		messaging.pingRandomNode();
	}

	public void validateData() {
		if(paused) { return; }
		pauseNetwork();

		if(!messaging.validateRandomNode()) {
			gui.WriteErrorOutput("Data not consistent between this node and another - This will be resolved...");
			messaging.validateAllData();
		}

		unpauseNetwork();
	}

	public void processInput() {
		String msg = gui.getInput();
		gui.clearInput();
		cli.processCommand(msg);
	}

    public void run() 
	{
		//startPinging();
		startDataValidationCheck();
		gui.printHelp("all");
        while(!quit)
		{
			if(paused) { continue; }
			try
			{
				SendData data = messaging.receiveData();
				if(data == null) { continue; }
				if(data instanceof FilePartRef) {
					Thread newThread = new Thread(() -> processFilePart(data));
					newThread.start();
				}
				else if(data instanceof DataItem) { 
					Thread newThread = new Thread(() -> receiveDataItem((DataItem) data));
					newThread.start();
				}
				else if(data.getClass().getEnclosingClass() == ServiceItem.class) {
					Thread newThread = new Thread(() -> processServiceItem(data));
					newThread.start();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				messaging.shutdownNode();
				quit = true;
			}
		}
    }

	public void processServiceItem(SendData data) {
        switch(data.getClass().toString().split("pdc.model.")[1]) {
            //new node not for me
            case "ServiceItem$SetupData":
				if(paused) {messaging.sendNode(data, aboutMe);}
                return;
            case "ServiceItem$HelloMessage":
                HelloMessage hdata = (HelloMessage) data;
                messaging.addNewNode(hdata);
                return;
            case "ServiceItem$GoodbyeData":
				processGoodbyeData((GoodbyeMessage) data);
                return;
            case "ServiceItem$ShutdownData":
				shutdown(false);
                return;
            case "ServiceItem$ConnectData":
				receiveConnectData((ConnectData) data);
                return;
            case "ServiceItem$PauseMessage":
				pauseNode();
                return;
            case "ServiceItem$UnpauseMessage":
                return;
            case "ServiceItem$RebuildRequest":
				sendSetupData((RebuildRequest) data);
                return;
			case "ServiceItem$ValidationRequest":
				sendValidationData((ValidationRequest) data);
                return;
			case "ServiceItem$ValidationData":
				messaging.sendNode((ValidationData) data, aboutMe);
                return;
			case "ServiceItem$DataRequest":
				sendDataItem((DataRequest) data);
				return;
			case "ServiceItem$DataRequestReply":
				messaging.sendNode((DataRequestReply) data, aboutMe);
				return;
			case "ServiceItem$FilePartRequest":
				sendFilePart((FilePartRequest) data);
				return;
			case "ServiceItem$FilePartRequestReply":
				messaging.sendNode((FilePartRequestReply) data, aboutMe);
				return;
			case "ServiceItem$PingMessage":
				sendPingReply((PingMessage) data);
				return;
			case "ServiceItem$PingMessageReply":
				PingMessageReply reply  = (PingMessageReply) data;
				messaging.pingReplied();
				gui.WriteOutput("Node '"+reply.getOriginalNode().getName()+"' alive at: "+reply.getOriginalNode().getIp().toString()+":"+reply.getOriginalNode().getPort());
				return;
			case "ServiceItem$NodeRemovalMessage":
				NodeRemovalMessage remove = (NodeRemovalMessage) data;
				if(remove.getRemoveNode().getId() == aboutMe.getId()) {
					gui.WriteErrorOutput("You did not respond to a ping so you have been removed from the network");
					messaging.resetData();
				} else messaging.removeNode(remove.getRemoveNode());
			case "ServiceItem$SetupDataRequest":
				SetupDataRequest request  = (SetupDataRequest) data;
				NodeData sender = request.getOriginalNode();
				if(autoaccept) { addNode(sender.getIp(), Integer.toString(sender.getPort())); return; }
				gui.WriteOutput("Node join request received from '"+sender.getName()+"'. IP: "+sender.getIp().toString()+", Port: "+sender.getPort());
				gui.WriteOutput("Enter 'node add [ip] [port]' to add this node to the network");
				return;
            default:
                return;
        }
	}

	public void processFilePart(SendData data) {
		messaging.addFilePart((FilePartRef) data);
	}

	public void unresponsiveNode(NodeData badNode) {
		gui.WriteErrorOutput("Unresponsive Node:'"+badNode.getName()+"', ID: "+Long.toString(badNode.getId())+", Address:  "+badNode.getIp().toString()+":"+badNode.getPort());
		messaging.removeNode(badNode);
	}

	public void sendPingReply(PingMessage ping) {
		PingMessageReply reply = new PingMessageReply(aboutMe);
		messaging.sendNode(reply, ping.getOriginalNode());
	}

	public void processGoodbyeData(GoodbyeMessage bye) {
		messaging.removeNode(bye.getOriginalNode());
	}

	public void receiveConnectData(ConnectData c) {
		if(messaging.isInNetwork(c.getOriginalNode())) { return; }
		printConnectionRequest(c);
		connect = c;
	}

	public void sendFilePart(FilePartRequest request) {
		NodeData sender = request.getOriginalNode();
		long id = request.getId();

		messaging.sendFilePart(sender, id);
	}

	public void receiveDataItem(DataItem dataItem) {
		if(paused) { return; }
		messaging.addDataItem(dataItem);
		gui.WriteOutput(dataItem.toReceivedPrint());
	}

	public void shutdown(boolean me) {
		if(me) {
			System.out.println("shutting down");
			messaging.shutdownNode();
		}
		else { 
			System.out.println("Shutdown message received");
		}
		System.exit(0);
	}

	public void printConnectionRequest(ConnectData c) {
		NodeData sender = c.getOriginalNode();
		System.out.println("Network connection rqeuest received from '"+sender.getName()+"'. IP: "+sender.getIp().toString()+", Port: "+sender.getPort());
		gui.WriteOutput("Network connection request received from '"+sender.getName()+"'. IP: "+sender.getIp().toString()+", Port: "+Integer.toString(sender.getPort()));
		gui.WriteOutput("Enter 'network connect' in 60s to accept the connection request");
		gui.WriteOutput("If you wish to request to connect to this network after timeout, enter 'network connect [ip] [port]'");
	}

	public void sendDataItem(DataRequest request) {
		NodeData sender = request.getOriginalNode();
		int index = request.getDataIndex();

		messaging.sendDataItem(sender, index);
	}

	public void printAllDataItems(String type) {
		ArrayList<DataItem> dataItems = messaging.getDataItems();
		for (DataItem s : dataItems) {
			if(type.equals("all") || type.equals(s.getType())) { gui.WriteOutput(s.toPrint()); }
		}
	}

	public void printData(int index) {
		DataItem s = messaging.getDataItem(index);
		//if(s.getClass() == DataItem.class)
		if(s == null) { gui.WriteErrorOutput("There was no data found at the index: "+index); return;}
		gui.WriteOutput(s.toPrintDetailed());
	}

	public void addAndSendData(String[] args) {
		String name = args[3];
		DataItem newData;
		ArrayList<NodeData> nodesToStoreOn = messaging.newNodesToStoreOn();
		try {
			FileManager files = new FileManager();
			switch(args[2]) {
				case "text":
					String msg = arrayToString(args).split("data add text "+name+" ")[1];
					newData = new StringItem(messaging.getNextIndex(), name, aboutMe, nodesToStoreOn, msg);
					break;
				case "number":
					String numberString = arrayToString(args).split("data add number "+name+" ")[1];
					BigDecimal num = new BigDecimal(numberString);
					newData = new NumericalItem(messaging.getNextIndex(), name, aboutMe, nodesToStoreOn, num);
					break;
				case "file":
					newData = addNewFile(name, nodesToStoreOn, arrayToString(args).split("data add file "+name+" ")[1]);
					break;
				case "external":
					String urlString = arrayToString(args).split("data add external "+name+" ")[1];
					URL url = null;
					try { url = new URL(urlString); } catch(IOException e) { System.out.println("IO error getting URL"); e.printStackTrace(); }
					byte[] urlData = files.getURLFile(aboutMe.getName(), name, url);
					newData = createNewFileItem(name, nodesToStoreOn, urlData);
					break;
				default:
					gui.WriteErrorOutput("Incorrect args - send 'help data' for instructions for use");
					return;		
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			gui.WriteErrorOutput("Incorrect args - send 'help data' for instructions for use");
			return;
		}
		messaging.addAndSendDataItem(newData);
		
		gui.WriteOutput("New item: '"+newData.getName()+"' has succesfully stored at index: "+newData.getIndex()+" with ID: "+newData.getId());
	}

	public FileItem addNewFile(String name, ArrayList<NodeData> nodesToStoreOn, String filePath) {
		FileManager files = new FileManager();
		byte[] data = files.readFile(filePath);
		return createNewFileItem(name, nodesToStoreOn, data);
	}

	public FileItem createNewFileItem(String name, ArrayList<NodeData> nodesToStoreOn, byte[] data) {
		int packetSize = 30000;
		long[] fileParts = new long[((int) Math.ceil((double) (data.length/packetSize)))+1];
		for(int i=0;i<fileParts.length;i++) {
			int start = i * packetSize;
            int end = Math.min(start + packetSize, data.length);
			FilePart filePart = new FilePart(aboutMe, nodesToStoreOn, i, Arrays.copyOfRange(data, start, end));
			fileParts[i] = filePart.getId();
			messaging.addAndSendFilePart(filePart);
			try { Thread.sleep(250); } catch (InterruptedException e) { e.printStackTrace(); }
		}
		FileItem fileItem = new FileItem(messaging.getNextIndex(), name, aboutMe, nodesToStoreOn, fileParts, data);
		return fileItem;
	}

	public String arrayToString(String[] array) {
		return Arrays.toString(array).replace(",","").replace("]","").replace("[","");
	}

	public void pauseNode() {
		paused=true;
		gui.WriteCommandOutput("pause signal sent");
		while(true) {
			SendData data = messaging.receiveData();
			if(data==null) {continue;}
			if(data instanceof HelloMessage) { 
				Thread newThread = new Thread(() -> messaging.addNewNode((HelloMessage) data));
				newThread.start();
			}
			else if(data instanceof GoodbyeMessage) { 
				NodeData bye = ((GoodbyeMessage) data).getOriginalNode();
				Thread newThread = new Thread(() -> messaging.removeNode(bye));
				newThread.start();
			}
			else if(data instanceof SetupData) { 
				Thread newThread = new Thread(() -> messaging.populateArrayLists((SetupData) data));
				newThread.start();
			}
			else if(data instanceof RebuildRequest) {
				RebuildRequest r = (RebuildRequest) data;
				Thread newThread = new Thread(() -> messaging.sendNode((SetupData) data, r.getOriginalNode()));
				newThread.start();
			}
			else if(data instanceof ValidationRequest) {
				ValidationRequest r = (ValidationRequest) data;
				ValidationData d = new ValidationData(aboutMe, null);
				Thread newThread = new Thread(() -> messaging.sendNode(d, r.getOriginalNode()));
				newThread.start();
			}
			else if(data instanceof PingMessage) {
				sendPingReply((PingMessage) data);
			}
			else if(data instanceof UnpauseMessage) { break; }
		}
		paused=false;
	}
	
	public void printHistory() {
		ArrayList<String> commandHistory = cli.getCommandHistory();
		for(int i=commandHistory.size()-1;i>=0;i--) { gui.WriteCommandOutput(commandHistory.get(i)); }
	}

	public void dataCommand(String command) {
		String[] commandArgs = command.split(" ");
		if(commandArgs[1].equalsIgnoreCase("add")) {
			addAndSendData(commandArgs);
			return;
		}
		if(commandArgs[1].equalsIgnoreCase("get")){
			if(commandArgs.length < 3) { gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("data"); return; }
			if(commandArgs[2].equalsIgnoreCase("all")) { 
				String t = "all";
				if(commandArgs.length == 4) { t = commandArgs[3]; }
				printAllDataItems(t); 
				return;
			} 
			printData(Integer.parseInt(commandArgs[2]));
			return;
		};
		if(commandArgs[1].equalsIgnoreCase("search")) {
			if(commandArgs.length != 4) { gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("data"); return; }
			if(commandArgs[2].equalsIgnoreCase("id")) { findDataById(Long.parseLong(command.split("data get id ")[1])); return; }
			if(commandArgs[2].equalsIgnoreCase("name")) { findDataByName(command.split("data get id ")[1]); return; }
			if(commandArgs[2].equalsIgnoreCase("number")) { findNumberData(command.split("data search number ")[1]); return; }
			if(commandArgs[2].equalsIgnoreCase("message")) { findTextData(command.split("data get message ")[1]); return; }
		}
		if(commandArgs[1].equalsIgnoreCase("generate")) {
			if(commandArgs.length != 4) { gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("data"); return; }
			generateData(commandArgs[2], commandArgs[3]);
		}
	}

	public void nodeCommand(String command) {
		String[] commandArgs = command.split(" ");
		if(commandArgs[1].equalsIgnoreCase("down")) { shutdown(true); }
		if(commandArgs[1].equalsIgnoreCase("wobbly")) { simulateNodeDown(); }
		if(commandArgs[1].equalsIgnoreCase("ask")) { 
			if(commandArgs.length != 4) { gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("node"); }
			askToJoin(commandArgs[2], commandArgs[3]); }
		if(commandArgs[1].equalsIgnoreCase("add")) {
			if(commandArgs.length != 4) { gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("node"); }
			else { addNode(commandArgs[2],commandArgs[3]); }
		}
		else if(commandArgs[1].equalsIgnoreCase("get")) {
			if(commandArgs[2].equalsIgnoreCase("all")) { printNodes(); }
			else if(commandArgs[2].equalsIgnoreCase("me")) { printNode(Long.toString(aboutMe.getId())); }
			else { printNode(commandArgs[2]); }
		}
		else if(commandArgs[1].equalsIgnoreCase("rebuild")) {
			rebuildNode();
		}
		else if(commandArgs[1].equalsIgnoreCase("ping")) {
			messaging.pingNode(Long.parseLong(commandArgs[2]));
		}else if(commandArgs[1].equalsIgnoreCase("autoaccept")) {
			autoaccept = !autoaccept;
		}
		//if(commandArgs[1].equalsIgnoreCase("search"));
		else {gui.WriteErrorOutput("Wrong syntax"); gui.printHelp("node"); }
	}

	public void networkCommand(String command) {
		String[] commandArgs = command.split(" ");
		if(commandArgs[1].equalsIgnoreCase("down")) {
			messaging.shutdownNetwork();
			System.out.println("network shutting down");
			System.exit(1);
		}
		if(commandArgs[1].equalsIgnoreCase("connect")) {
			
			//if accepting
			if(connect != null) {
				//if responding to connection request
				if(commandArgs.length == 2) {
					joinOpenConnection();
					connect = null;
					return;
				}
				else {
					gui.WriteErrorOutput("Wrong syntax"); 
					gui.printHelp("network"); 
					return; 
				}
			}
			else if(commandArgs.length == 4) {
				startNewNetworkConnection(commandArgs[2], commandArgs[3]);
				return;
			}
			else {
				gui.WriteErrorOutput("Wrong syntax"); 
				gui.printHelp("network"); 
				return; 
			}
		}
	}

	public void simulateNodeDown() {
		paused=true;
		try { Thread.sleep(60000); } catch (InterruptedException e) { System.out.println("Interupted Sleeping"); e.printStackTrace(); }
		paused=false;
	}

	public InetAddress stringToIp(String ipString) {
		InetAddress ip = null;
		try { ip = InetAddress.getByName(ipString); } catch (UnknownHostException e) { System.out.println("Unknown Host error String -> IP"); e.printStackTrace(); }
		return ip;
	}

	public void askToJoin(String ipString, String portString) {
		paused = true;
		InetAddress ip = stringToIp(ipString);
		int port = Integer.parseInt(portString);
		NodeData askNode = new NodeData("join request", ip, port);
		messaging.askToJoinNetwork(askNode);
		paused = false;
	}

	public void rebuildNode() {
		gui.WriteErrorOutput("Network paused while rebuilding node");
		pauseNetwork();
		
		messaging.rebuildDataItems();

		unpauseNetwork();
		gui.WriteOutput("Node rebuilt - network unpaused");
	}

	public void sendSetupData(RebuildRequest r) {
		NodeData sendNode = r.getOriginalNode();
		messaging.setupNode(sendNode);
	}

	public void sendValidationData(ValidationRequest request) {
		ValidationData v = new ValidationData(aboutMe, messaging.getChecksums());
		messaging.sendNode(v, request.getOriginalNode());
	}

	public void pauseNetwork() {
		paused=true;
		messaging.pauseNetwork();
	}

	public void unpauseNetwork() {
		paused=false;
		messaging.unpauseNetwork();
	}

	public void startNewNetworkConnection(String ipString, String portString) {

		pauseNetwork();
        AtomicBoolean complete = new AtomicBoolean(false);

		int port = Integer.parseInt(portString);
		InetAddress ip = stringToIp(ipString);

		Runnable connecting = () -> { 
			messaging.connectNetworks(ip, port);
			complete.set(true);;
		};
				
		ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(connecting);

		Thread newThread = new Thread(() -> {
			try {
				future.get(60, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
			}
		});
		newThread.start();
		while(!complete.get()){}

        executor.shutdown();
		
		unpauseNetwork();
	}

	public void joinOpenConnection() {

		pauseNetwork(); 

		InetAddress ip = connect.getOriginalNode().getIp();
		int port = connect.getOriginalNode().getPort();

		messaging.setupNetwork(ip, port, connect.getData(), connect.getNodes());
		connect = null;
		unpauseNetwork();
	}

	public void findDataByName(String name) {
		ArrayList<DataItem> dataItems = messaging.getDataItems();
		for(DataItem m : dataItems) {
			if(m.getName() == name) { 
				gui.WriteOutput(m.toPrint());
			}
		}
		gui.WriteErrorOutput("No data with name '"+name+ "'' was found");
	}

	public void findNumberData(String msg) {
		ArrayList<DataItem> dataItems = messaging.getDataItems();
		for(DataItem m : dataItems) {
			if(!m.getType().equals("number")) { continue; }
			NumericalItem number = (NumericalItem) m;
			if(number.getNumber() == new BigDecimal(msg)) { 
				gui.WriteOutput(m.toPrint());
			}
		}
		gui.WriteErrorOutput("No number data with value of'"+msg+ "'' was found");
	}
	
	public void findTextData(String msg) {
		ArrayList<DataItem> dataItems = messaging.getDataItems();
		for(DataItem m : dataItems) {
			if(!m.getType().equals("text")) { continue; }
			StringItem text = (StringItem) m;
			if(text.getMessage().contains(msg)) { 
				gui.WriteOutput(m.toPrint());
			}
		}
		gui.WriteErrorOutput("No text data with message including the text '"+msg+"'' was found");
	}

	public void findDataById(long id) {
		ArrayList<DataItem> messages = messaging.getDataItems();
		for(DataItem m : messages) {
			if(m.getId() == id) {
				gui.WriteOutput(m.toPrint());
				return;
			}
		}
		gui.WriteOutput("No data with ID: "+Long.toString(id)+ " was found ");
	}

	public void printNodes() {
		ArrayList<NodeData> nodes = messaging.getNodes();
		for(NodeData n : nodes) {
			gui.WriteOutput("Node: '"+n.getName()+"' is on port: "+n.getPort()+". Its ID is: "+Long.toString(n.getId()));
		}
	}

	public void printNode(String node) {
		long id = Long.parseLong(node);
		ArrayList<NodeData> nodes = messaging.getNodes();
		for(NodeData n : nodes) {
			if(n.getId() == id ) {
				gui.WriteOutput("Node: '"+n.getName()+"' is on port: "+n.getPort()+". Its ID is: "+Long.toString(n.getId()));
				return ;
			}
		}
		gui.WriteOutput("No node with ID: "+Long.toString(id)+ " was found ");
	}

	
	public void generateData(String type, String amountString) {
		int amount = Integer.parseInt(amountString);
		DataGenerator generator = new DataGenerator();
		String[] args = new String[] { "data", "add", type, "name", "replace" };
		for(int i=0;i<amount;i++) {
			args[2] = type; args[3] = generator.generateTextData(10);
			if(type.equals("text")) { args[4] = generator.generateTextData(50); addAndSendData(args); }
			else if(type.equals("number")) { args[4] = generator.generateNumberData(); addAndSendData(args); }
			else { gui.WriteErrorOutput("Incorrect Syntax"); gui.printHelp("data"); return; }
		}
	}


	public void addNode(String ipString , String portString) {

		InetAddress ip = stringToIp(ipString);
		int portNum = Integer.parseInt(portString);
		ArrayList<NodeData> nodes = messaging.getNodes();
		for( NodeData node : nodes) {
			if( node.getIp() == ip && node.getPort() == portNum) {
				gui.WriteOutput("Node already connected with Address "+ipString+":"+portString);
				return;
			}	
		}
		NodeData sendNode = new NodeData("send setup data", ip, portNum);
		messaging.setupNode(sendNode);
	}

	public void addNode(InetAddress ip , String portString) {

		int portNum = Integer.parseInt(portString);
		ArrayList<NodeData> nodes = messaging.getNodes();
		for( NodeData node : nodes) {
			if( node.getIp() == ip && node.getPort() == portNum) {
				gui.WriteOutput("Node already connected with Address "+ip.toString()+":"+portString);
				return;
			}	
		}
		NodeData sendNode = new NodeData("send setup data", ip, portNum);
		messaging.setupNode(sendNode);
	}
}

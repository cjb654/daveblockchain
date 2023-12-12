package pdc.controller;

import pdc.node.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class NodeManager extends Thread {

	public static void main(String[] args) throws IOException {
					
		Scanner sc = new Scanner(System.in);
		
		//quiet opetion for receiving
		//spin up x nodes

		
		while(true) {
			help();
			String in = sc.nextLine();
			String[] myArgs = in.split(" ");
			if (myArgs.length == 0)
			{
				System.out.println("Wrong syntax");
				help();
				continue;
			}
			if(myArgs[0].equals("exit")) {
				System.out.println("Goodbye!");
				break;
			}
			if(myArgs[0].equals("1") || myArgs[0].equals("2") || myArgs[0].equals("3")) {
				try {
					InetAddress ip = InetAddress.getByName("localhost");
					int port = 3838;
					if(myArgs[0].equals("2")) {port=4848;}
					if(myArgs[0].equals("3")) {port=5858;}
					final Node node = new Node(("NODE-"+myArgs[0]), ip, port);
					Thread nodeThread = new Thread(() -> node.run());
					nodeThread.start();
					Files.createDirectories(Paths.get("/nodes/node_"+myArgs[0]));
				}
					catch(SocketException e) {System.out.println("trying new port");continue;}
			}
			else if(myArgs[0].equals("spin")) {
				try {
					int[] ports = new int[]{3939,4040,4141,4242, 4343,4444,4545,4646,4747};
					InetAddress ip = InetAddress.getByName("localhost");
					for(int i=0;i<Integer.parseInt(myArgs[1]);i++) {
						int port = ports[i];
						final Node node = new Node(("NODE-"+i+3), ip, port);
						Thread nodeThread = new Thread(() -> node.run());
						nodeThread.start();
						Files.createDirectories(Paths.get("/nodes/node_"+myArgs[0]+"_"+i+3));
					}
				}
					catch(SocketException e) {System.out.println("trying new port");continue;}
			}
			else if (myArgs[2].equals("random")) {
				InetAddress ip = InetAddress.getByName(myArgs[1]);
				int port = ThreadLocalRandom.current().nextInt(1, 65535);
				while(true) {
					try {
						
						final Node node = new Node(myArgs[0],ip, port);
						System.out.println("Port:"+Integer.toString(port));
						Thread nodeThread = new Thread(() -> node.run());
						nodeThread.start();
						Files.createDirectories(Paths.get("/nodes/random_"+port));
						break;
					}
					catch(SocketException e) {System.out.println("trying new port");continue;}
				}
			}
			else {
				try {
					InetAddress ip = InetAddress.getByName(myArgs[1]);
					boolean join = true;
					if(myArgs[3] == "network") { join = false; }
					final Node node = new Node(myArgs[0], ip, Integer.parseInt(myArgs[2]));
					Thread nodeThread = new Thread(() -> node.run());
					nodeThread.start();
				}
					catch(SocketException e) { System.out.println("trying new port"); continue; }
			}
			
		}

		sc.close();
		System.exit(1);
	}

	public static void help()
	{
		System.out.println("Usage: command name [ip address] [port number]|random");
	}
        
}

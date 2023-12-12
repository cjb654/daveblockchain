package pdc.controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

import pdc.node.Node;

public class NodeLoad {

    public static void main(String[] args) {

        if(args[0].equals("spin"))  {
            try {
                int[] ports = new int[]{3939,4040,4141,4242, 4343,4444,4545,4646,4747};
                InetAddress ip = InetAddress.getByName("localhost");
                for(int i=0;i<Integer.parseInt(args[1]);i++) {
                    int port = ports[i];
                    final Node node = new Node(("NODE-"+i+3), ip, port);
                    Thread nodeThread = new Thread(() -> node.run());
                    nodeThread.start();
                    Files.createDirectories(Paths.get("/nodes/node_"+args[0]+"_"+i+3));
                }
            }
            catch(SocketException e) { e.printStackTrace(); }
            catch(UnknownHostException e) { e.printStackTrace(); }
            catch(IOException e) { e.printStackTrace(); }
        }
        else {
            try {
                InetAddress ip = InetAddress.getByName(args[1]);
                int port = Integer.parseInt(args[2]);
                final Node node = new Node(args[0], ip, port);
                Thread nodeThread = new Thread(() -> node.run());
                nodeThread.start();
                Files.createDirectories(Paths.get("/nodes/node_"+args[0]));
            }
            catch(SocketException e) { e.printStackTrace();}
            catch(UnknownHostException e) { e.printStackTrace(); }
            catch(IOException e) { e.printStackTrace(); }
        }
    }
}

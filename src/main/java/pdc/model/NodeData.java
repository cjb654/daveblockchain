package pdc.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.time.Instant;

public class NodeData implements Serializable{
        private long id;
        private String name;
        private InetAddress ip;
        private int port;

        public NodeData( String n, InetAddress i, int p) {
            id = ( Instant.now().getEpochSecond() ) * (int) n.charAt(0);
            name = n;
            ip = i;
            port = p;
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public void setName(String newName) {name = newName;}
        public InetAddress getIp() { return ip; }
        public int getPort() { return port; }
    }
package com.espipablo.p2p.model;

public class PeerData {
	public byte[] id;
	public String ip;
	public String port;
	public int numPeer;

	public static final String ID_NAME = "id";
	public static final String IP_NAME = "ip";
	public static final String PORT_NAME = "port";
	public static final String NUM_PEER = "peer";
}

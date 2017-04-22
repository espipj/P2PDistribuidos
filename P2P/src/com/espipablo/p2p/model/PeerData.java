package com.espipablo.p2p.model;

import org.json.JSONObject;

import com.espipablo.p2p.controller.Util;

public class PeerData {
	public byte[] id;
	public String ip;
	public String port;
	public int numPeer;
	
	public PeerData(JSONObject obj) {
		this.id = obj.getString(PeerData.ID_NAME).getBytes();
		this.ip = obj.getString(PeerData.IP_NAME);
		this.port = obj.getString(PeerData.PORT_NAME);
		this.numPeer = obj.getInt(PeerData.NUM_PEER);
	}
	
	public PeerData() {
	}
	
	public JSONObject getAsJSON() {
		JSONObject obj = new JSONObject();
		obj.put(PeerData.ID_NAME, Util.byteToString(this.id));
		obj.put(PeerData.IP_NAME, this.ip);
		obj.put(PeerData.PORT_NAME, this.port);
		obj.put(PeerData.NUM_PEER, this.numPeer);
		return obj;
	}

	public static final String ID_NAME = "id";
	public static final String IP_NAME = "ip";
	public static final String PORT_NAME = "port";
	public static final String NUM_PEER = "peer";
}

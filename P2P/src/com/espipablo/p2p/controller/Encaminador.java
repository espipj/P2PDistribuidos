package com.espipablo.p2p.controller;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.validation.constraints.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.espipablo.p2p.model.PeerData;

public class Encaminador {
	
	protected boolean validated;
	protected PeerData[][] table;
	protected int[] tableCount;
	protected byte[] id;
	protected String ip;
	protected String port;
	protected int numPeer;
	
	protected static final int NUM_BITS = 160;
	protected static final int TRIES = 5;
	protected static final int ALFA = 10;
	
	public Encaminador(String ip, @NotNull String port, int toPeer, String myIp, String myPort, int numPeer) throws SocketException, UnknownHostException {
		table = new PeerData[Encaminador.NUM_BITS][Encaminador.ALFA];
		tableCount = new int[Encaminador.NUM_BITS];
		for (int i=0, length = tableCount.length; i < length; i++) {
			tableCount[i] = 0;
		}
		
		this.ip = myIp;
		this.port = myPort;
		this.numPeer = numPeer;
		
		if (ip == null) {
			this.validated = true;
		}
		
    	NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    	String macAddress;
    	StringBuilder sb = new StringBuilder();
    	byte[] mac = network.getHardwareAddress();
    	for (byte b: mac) {
    		sb.append(String.format("%02X", b));
    	}
    	macAddress = Util.sha1(sb.toString());
    	this.id = macAddress.getBytes();
		
		int i = 0;
		while (ip != null && !this.validateID(ip, port, toPeer) && i < Encaminador.TRIES) {
			i++;
			this.id = Util.sha1(Long.toString(System.currentTimeMillis())).getBytes();
		}
		
		if (!validated) {
			this.id = null;
		}
	}
	
	public boolean validateID(String ip, String port, int toPeer) {
		JSONObject jsonObj = new JSONObject(
				Util.request("http://" + ip + ":" + port + "/P2P/validateId?id="
				+ new String(this.id, StandardCharsets.UTF_8)
				+ "&ip=" + this.ip
				+ "&port=" + this.port
				+ "&numPeer=" + this.numPeer
				+ "&toPeer=" + toPeer));
		if (jsonObj.getBoolean("error") == true) {
			return false;
		}
		
		JSONArray tableJson = jsonObj.getJSONArray("table");
		for (int i=0, length = tableJson.length(); i < length; i++) {
			JSONObject tableEntry = tableJson.getJSONObject(i);
			
			PeerData peer = new PeerData();
			peer.id = tableEntry.getString(PeerData.ID_NAME).getBytes();
			peer.ip = tableEntry.getString(PeerData.IP_NAME);
			peer.port = tableEntry.getString(PeerData.PORT_NAME);
			peer.numPeer = tableEntry.getInt(PeerData.NUM_PEER);
			
			int numTable = getNumList(peer.id);
			// La tabla esta llena y no entran más
			if (numTable == -1 || this.tableCount[numTable] >= Encaminador.ALFA) {
				continue;
			}
			
			this.table[numTable][this.tableCount[numTable]++] = peer;
		}
		
		validated = true;
		return true;
	}
	
	public byte[] getId() {
		return this.id;
	}
	

    protected int getNumList(byte[] id2) {
    	byte[] result = Util.xor(this.id, id2);
    	
    	for (int i=0, j=result.length * 8 - 1, length = result.length * 8; i < length; i++, j--) {
    		if (Util.getBit(this.id, j) != Util.getBit(id2, j)) {
    			return i;
    		}
    	}
    	
    	return -1;
    }
    
    public boolean checkIfIdExists(byte[] id, String ip, String port, int numPeer) {
    	System.out.println(Arrays.toString(this.id));
    	System.out.println(Arrays.toString(id));
    	if (Arrays.equals(this.id, id)) {
    		return true;
    	}
    	
    	int numTable = getNumList(id);
    	System.out.println(numTable);
    	
    	PeerData[] list = this.table[numTable];
    	for (PeerData p: list) {
    		if (p == null) {
    			break;
    		}
    		
    		if (Arrays.equals(p.id, id)) {
    			return true;
    		}
    	}
    	
    	// Añadimos a la tabla la ID
		// La tabla esta llena y no entran más
		if (this.tableCount[numTable] >= Encaminador.ALFA) {
			System.out.println("LLENO");
			return false;
		}
		
		PeerData peer = new PeerData();
		peer.id = id;
		peer.ip = ip;
		peer.port = port;
		peer.numPeer = numPeer;
		this.table[numTable][this.tableCount[numTable]++] = peer;
    	return false;
    }
    
    public JSONArray getRouteTableAsJSON() {
    	JSONArray table = new JSONArray();

    	for (PeerData[] peerList: this.table) {
    		if (peerList == null) {
    			continue;
    		}
    		
    		for (PeerData p: peerList) {
    			if (p == null) {
    				break;
    			}
    			
    			JSONObject peer = new JSONObject();
    			peer.put(PeerData.ID_NAME, Util.byteToString(p.id));
    			peer.put(PeerData.IP_NAME, p.ip);
    			peer.put(PeerData.PORT_NAME, p.port);
    			peer.put(PeerData.NUM_PEER, p.numPeer);
    			table.put(peer);
    		}
    	}

    	return table;
    }

}

package com.espipablo.p2p.model;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.espipablo.p2p.controller.Encaminador;
import com.espipablo.p2p.controller.Util;

public class Peer {
	protected Encaminador encaminador;
	protected Map<String, String> files;
	protected byte[] id;
	protected String ip;
	protected String port;
	protected int numPeer;
	protected PeerData upper;
	protected PeerData lower;
	
	public Peer(String ip, @NotNull String port, int toPeer, String myIp, String myPort, int numPeer) throws SocketException, UnknownHostException {
		this.encaminador = new Encaminador(ip, port, toPeer, myIp, myPort, numPeer);
		this.files = new HashMap<>();
		this.id = this.encaminador.getId();
		this.upper = new PeerData();
		this.lower = new PeerData();
		
		this.ip = ip;
		this.port = port;
		this.numPeer = numPeer;
		
		this.fillFilesTable();
		Util.prettyPrintByte(upper.id);
		Util.prettyPrintByte(lower.id);
	}
	
	public Encaminador getEncaminador() {
		return this.encaminador;
	}
	
	
	public JSONArray getFilesTableAsJSON() {
		JSONArray filesArr = new JSONArray();
		files.forEach((key, file) -> {
			JSONObject fileObj = new JSONObject();
			fileObj.put("key", key);
			fileObj.put("file", file);
			filesArr.put(fileObj);
		});
		return filesArr;
	}
	
	public JSONObject getFile(String key) {
		JSONObject fileObj = new JSONObject();
		fileObj.put("key", key);
		fileObj.put("file", files.get(key));
		return fileObj;
	}
	
	public void fillFilesTable() {
		PeerData[] neightbours = this.encaminador.getNeightboursFromID(this.id);
		setUpper(neightbours[0]);
		setLower(neightbours[1]);
		
		if (this.upper.ip != null) {
			JSONArray filesArr = new JSONArray(Util.request("http://" 
					+ upper.ip
					+ ":"
					+ upper.port
					+ "/P2P/getFilesTable?toPeer="
					+ upper.numPeer));
			for (int i=0, length = filesArr.length(); i < length; i++) {
				JSONObject fileObj = filesArr.getJSONObject(i);
				if (this.encaminador.compareClosest(this.id, upper.id, fileObj.getString("key").getBytes()) < 0) {
					files.put(fileObj.getString("key"), fileObj.getString("file"));
				}
			}
		}

		if (this.lower.ip != null) {
			JSONArray filesArr = new JSONArray(Util.request("http://" 
					+ lower.ip
					+ ":"
					+ lower.port
					+ "/P2P/getFilesTable?toPeer="
					+ lower.numPeer));
			for (int i=0, length = filesArr.length(); i < length; i++) {
				JSONObject fileObj = filesArr.getJSONObject(i);
				if (this.encaminador.compareClosest(this.id, lower.id, fileObj.getString("key").getBytes()) < 0) {
					files.put(fileObj.getString("key"), fileObj.getString("file"));
				}
			}
		}
	}
	
	public void setUpper(String id, String ip, String port, int numPeer) {
		synchronized(this.upper) {
			this.upper.id = id.getBytes();
			this.upper.ip = ip;
			this.upper.port = port;
			this.upper.numPeer = numPeer;
		}
	}
	
	public void setUpper(String id, String ip, String port, int numPeer, boolean callback) {
		this.setUpper(id, ip, port, numPeer);
		if (callback) {
			Util.request("http://" 
					+ this.upper.ip
					+ ":"
					+ this.upper.port
					+ "/P2P/setUpper?toPeer="
					+ this.upper.numPeer
					+ "&id="
					+ Util.byteToString(this.id)
					+ "&ip="
					+ this.ip
					+ "&port="
					+ this.port
					+ "&numPeer"
					+ this.numPeer);
		}
	}
	
	public void setUpper(PeerData peer) {
		if (peer == null) {
			this.upper.ip = null;
			return;
		}
		
		this.setUpper(Util.byteToString(peer.id), peer.ip, peer.port, peer.numPeer);
		Util.request("http://" 
				+ this.upper.ip
				+ ":"
				+ this.upper.port
				+ "/P2P/setUpper?toPeer="
				+ this.upper.numPeer
				+ "&id="
				+ Util.byteToString(this.id)
				+ "&ip="
				+ this.ip
				+ "&port="
				+ this.port
				+ "&numPeer"
				+ this.numPeer);
	}
	
	public void setLower(String id, String ip, String port, int numPeer) {
		synchronized(this.lower) {
			this.lower.id = id.getBytes();
			this.lower.ip = ip;
			this.lower.port = port;
			this.lower.numPeer = numPeer;
		}
	}
	
	public void setLower(String id, String ip, String port, int numPeer, boolean callback) {
		this.setLower(id, ip, port, numPeer);
		if (callback) {
			Util.request("http://" 
					+ this.lower.ip
					+ ":"
					+ this.lower.port
					+ "/P2P/setLower?toPeer="
					+ this.lower.numPeer
					+ "&id="
					+ Util.byteToString(this.id)
					+ "&ip="
					+ this.ip
					+ "&port="
					+ this.port
					+ "&numPeer"
					+ this.numPeer);
		}
	}
	
	public void setLower(PeerData peer) {
		if (peer == null) {
			this.lower.ip = null;
			return;
		}
		
		this.setLower(Util.byteToString(peer.id), peer.ip, peer.port, peer.numPeer);
		Util.request("http://" 
				+ this.lower.ip
				+ ":"
				+ this.lower.port
				+ "/P2P/setLower?toPeer="
				+ this.lower.numPeer
				+ "&id="
				+ Util.byteToString(this.id)
				+ "&ip="
				+ this.ip
				+ "&port="
				+ this.port
				+ "&numPeer"
				+ this.numPeer);
	}
	
	public void putFile(String file) {
		String key = Util.sha1(file);
		System.out.println(file);
		System.out.println(key);
		
		// 		Instead of looking for upper and lower, first, let's try to send it to the closest node to the id that we have in our routing table
		// (in case is closer than us). If we are closer then we will go for the upper-lower method
		PeerData closest = null;
		if (this.lower.ip != null && this.encaminador.compareClosest(upper.id, this.lower.id, key.getBytes()) < 1) {
			closest = this.lower;
		} else if (this.upper.ip != null) {
			closest = this.upper;
		}
		
		if (closest == null || this.encaminador.compareClosest(this.id, closest.id, key.getBytes()) < 1) {
			this.files.put(key, file);
		} else {
			// We don't care about the result, file is being put by another node
			Util.request("http://" 
					+ closest.ip
					+ ":"
					+ closest.port
					+ "/P2P/put?toPeer="
					+ closest.numPeer
					+ "&file="
					+ file);
		}
		System.out.println(this.files);
	}
	
}

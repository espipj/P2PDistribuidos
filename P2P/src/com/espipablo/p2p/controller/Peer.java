package com.espipablo.p2p.controller;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.espipablo.p2p.model.PeerData;

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
		
		this.ip = myIp;
		this.port = myPort;
		this.numPeer = numPeer;
		
		this.fillFilesTable();
		System.out.println("I'm: ");
		Util.prettyPrintByte(this.id);
		/*System.out.println("UPPER");
		Util.prettyPrintByte(upper.id);
		System.out.println("LOWER");
		Util.prettyPrintByte(lower.id);
		System.out.println(files);*/
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
	
	public JSONObject getFile(String fileName, boolean notInit) {
		String key = Util.sha1(fileName);
		String file = files.get(key);
		
		if (file == null && !notInit) {
			PeerData peer = this.encaminador.getClosestPeer(key.getBytes(), System.currentTimeMillis());
			System.out.println("Key must be at peer: " + Util.byteToString(peer.id) + "|| " + peer.ip + ":" + peer.port + "/?=" + peer.numPeer);
			return new JSONObject(Util.request("http://"
							+ peer.ip
							+ ":"
							+ peer.port
							+ "/P2P/get?toPeer="
							+ peer.numPeer
							+ "&file="
							+ fileName
							+ "&notInit="
							+ true)
					);
		} else if (file == null) {
			JSONObject fileObj = new JSONObject();
			fileObj.put("key", 0);
			fileObj.put("file", 0);
			fileObj.put("from", Util.getPrettyPrintString(this.id));
			fileObj.put("error", true);
			return fileObj;
		} else {
			JSONObject fileObj = new JSONObject();
			fileObj.put("key", key);
			fileObj.put("file", file);
			fileObj.put("from", Util.getPrettyPrintString(this.id));
			fileObj.put("error", false);
			return fileObj;
		}
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
		System.out.println(this.numPeer + "Upper saved: " + id + "|| " + ip + ":" + port + "/?=" + numPeer);
	}
	
	public void setUpper(String id, String ip, String port, int numPeer, boolean callback) {
		this.setUpper(id, ip, port, numPeer);
		if (callback) {
			Util.request("http://" 
					+ this.upper.ip
					+ ":"
					+ this.upper.port
					+ "/P2P/setLower?toPeer="
					+ this.upper.numPeer
					+ "&id="
					+ Util.byteToString(this.id)
					+ "&ip="
					+ this.ip
					+ "&port="
					+ this.port
					+ "&numPeer="
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
				+ "/P2P/setLower?toPeer="
				+ this.upper.numPeer
				+ "&id="
				+ Util.byteToString(this.id)
				+ "&ip="
				+ this.ip
				+ "&port="
				+ this.port
				+ "&numPeer="
				+ this.numPeer);
	}
	
	public void setLower(String id, String ip, String port, int numPeer) {
		synchronized(this.lower) {
			this.lower.id = id.getBytes();
			this.lower.ip = ip;
			this.lower.port = port;
			this.lower.numPeer = numPeer;
			System.out.println(this.numPeer + "Lower saved: " + id + "|| " + ip + ":" + port + "/?=" + numPeer);
		}
	}
	
	public void setLower(String id, String ip, String port, int numPeer, boolean callback) {
		this.setLower(id, ip, port, numPeer);
		if (callback) {
			Util.request("http://" 
					+ this.lower.ip
					+ ":"
					+ this.lower.port
					+ "/P2P/setUpper?toPeer="
					+ this.lower.numPeer
					+ "&id="
					+ Util.byteToString(this.id)
					+ "&ip="
					+ this.ip
					+ "&port="
					+ this.port
					+ "&numPeer="
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
				+ "/P2P/setUpper?toPeer="
				+ this.lower.numPeer
				+ "&id="
				+ Util.byteToString(this.id)
				+ "&ip="
				+ this.ip
				+ "&port="
				+ this.port
				+ "&numPeer="
				+ this.numPeer);
	}
	
	public void putFile(String file) {
		String key = Util.sha1(file);
		System.out.println(file);
		Util.prettyPrintByte(key.getBytes());
		System.out.println("Trying to save: ");
		System.out.println("I'm: ");
		Util.prettyPrintByte(this.id);
		System.out.println("UPPER");
		Util.prettyPrintByte(upper.id);
		System.out.println("LOWER");
		Util.prettyPrintByte(lower.id);
		System.out.println(files);
		
		// 		Instead of looking for upper and lower, first, let's try to send it to the closest node to the id that we have in our routing table
		// (in case is closer than us). If we are closer then we will go for the upper-lower method
		PeerData closest = null;
		if (this.lower.ip != null && this.upper.ip != null) {
			if (this.encaminador.compareClosest(this.upper.id, this.lower.id, key.getBytes()) < 1) {
				closest = this.upper;
			} else {
				closest = this.lower;
			}
		} else if (this.upper.ip != null) {
			closest = this.upper;
		} else if (this.lower.ip != null) {
			closest = this.lower;
		}
		
		if (closest == null || this.encaminador.compareClosest(this.id, closest.id, key.getBytes()) < 1) {
			this.files.put(key, file);
		} else {
			// We don't care about the result, file is being put by another node
			System.out.println("I'm: " + this.numPeer + " asking: " + Util.byteToString(closest.id) + "|| " + closest.ip + ":" + closest.port + "/?=" + closest.numPeer);
			Util.request("http://" 
					+ closest.ip
					+ ":"
					+ closest.port
					+ "/P2P/put?toPeer="
					+ closest.numPeer
					+ "&file="
					+ file);
		}
	}
	
}

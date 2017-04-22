package com.espipablo.p2p.controller;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;

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
	
	protected LinkedList<String> receivedRequests;
	
	protected static final int NUM_BITS = 160;
	protected static final int TRIES = 5;
	protected static final int K = 10;
	protected static final int ALFA = 10;
	
	public Encaminador(String ip, @NotNull String port, int toPeer, String myIp, String myPort, int numPeer) throws SocketException, UnknownHostException {
		table = new PeerData[Encaminador.NUM_BITS][Encaminador.K];
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
		
		this.receivedRequests = new LinkedList<>();
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
		
		JSONObject tableEntry = jsonObj.getJSONObject("peer");
		PeerData peer = new PeerData();
		peer.id = tableEntry.getString(PeerData.ID_NAME).getBytes();
		peer.ip = tableEntry.getString(PeerData.IP_NAME);
		peer.port = tableEntry.getString(PeerData.PORT_NAME);
		peer.numPeer = tableEntry.getInt(PeerData.NUM_PEER);
		int numTable = getNumList(peer.id);
		this.table[getNumList(peer.id)][this.tableCount[numTable]++] = peer;
		
		JSONArray tableJson = jsonObj.getJSONArray("table");
		for (int i=0, length = tableJson.length(); i < length; i++) {
			tableEntry = tableJson.getJSONObject(i);
			
			peer = new PeerData();
			peer.id = tableEntry.getString(PeerData.ID_NAME).getBytes();
			peer.ip = tableEntry.getString(PeerData.IP_NAME);
			peer.port = tableEntry.getString(PeerData.PORT_NAME);
			peer.numPeer = tableEntry.getInt(PeerData.NUM_PEER);
			
			numTable = getNumList(peer.id);
			// La tabla esta llena y no entran más
			if (numTable == -1 || this.tableCount[numTable] >= Encaminador.K) {
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
		
		for (int i=0, j=7, length = result.length * 8; i < length; i++, j--) {
			if (Util.getBit(this.id, j) != Util.getBit(id2, j)) {
				return i;
			}

			if (j % 8 == 0) {
				j += 16;
			}
		}
		
		return -1;
	}
	
	public boolean checkIfIdExists(byte[] id, String ip, String port, int numPeer) {
		if (Arrays.equals(this.id, id)) {
			return true;
		}
		
		int numTable = getNumList(id);
		
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
		if (this.tableCount[numTable] >= Encaminador.K) {
			System.out.println("LLENO");
			return false;
		}
		
		PeerData peer = new PeerData();
		peer.id = id;
		peer.ip = ip;
		peer.port = port;
		peer.numPeer = numPeer;
		System.out.println("NEW PEER: " + Util.byteToString(peer.id) + "|| " + peer.ip + ":" + peer.port + "/?=" + peer.numPeer);
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

	public int compareDistances(byte[] id1, byte [] id2) {		
		for (int i=0, j=7, length = id1.length * 8; i < length; i++, j--) {
			if (Util.getBit(id1, j) != Util.getBit(id2, j)) {
				if (Util.getBit(id2, j) == 1) {
					return -1;
				}
				return 1;
			}
			
			if (j % 8 == 0) {
				j += 16;
			}
		}
		
		return 0;
	}
	
	public int compareClosest(byte[] alt1, byte [] alt2, byte[] objective) {
		return compareDistances(Util.xor(alt1, objective), Util.xor(alt2, objective));
	}
	
	protected LinkedList<PeerData> getClosestNodes(byte[] objective, int limit) {
		LinkedList<PeerData> nodes = new LinkedList<>();
		int added = 0;
		// This flag will be false if no more nodes are available (We didn't reach ALFA)
		boolean addedThisRound = true;
		
		byte[] furtherBuilder = new byte[objective.length];
		for (int i=0, length = objective.length; i < length; i++) {
			furtherBuilder[i] = (byte) 0b11111111;
		}
		byte[] further = Util.xor(objective, furtherBuilder);
		
		while (addedThisRound && added < limit) {
			addedThisRound = false;
			
			byte[] nearest = further;
			PeerData chosenPeer = null;
			for (PeerData[] peerList: table) {
				if (peerList == null) {
					continue;
				}

				for (PeerData peer: peerList) {
					if (peer == null) {
						continue;
					}

					/*Util.prettyPrintByte(peer.id);
					Util.prettyPrintByte(nearest);
					Util.prettyPrintByte(objective);
					System.out.println(compareClosest(peer.id, nearest, objective));*/
					if (compareClosest(peer.id, nearest, objective) <= 0 && nodes.indexOf(peer) == -1) {
						chosenPeer = peer;
						nearest = peer.id;
					}
				}
			}
			
			if (chosenPeer != null && nodes.indexOf(chosenPeer) == -1) {
				nodes.add(chosenPeer);
				added++;
				addedThisRound = true;
			}
		}
		
		return nodes;
	}

	protected LinkedList<PeerData> getClosestNodes(byte[] objective, int limit, LinkedList<PeerData> peers) {
		LinkedList<PeerData> nodes = new LinkedList<>();
		int added = 0;
		// This flag will be false if no more nodes are available (We didn't reach LIMIT)
		boolean addedThisRound = true;
		
		byte[] furtherBuilder = new byte[objective.length];
		for (int i=0, length = objective.length; i < length; i++) {
			furtherBuilder[i] = (byte) 0b11111111;
		}
		byte[] further = Util.xor(objective, furtherBuilder);
		
		while (addedThisRound && added < limit) {
			addedThisRound = false;
			
			byte[] nearest = further;
			PeerData chosenPeer = null;
			for (PeerData peer: peers) {
				
				//System.out.println("Analizando a: " + peer.numPeer);
				if (compareClosest(peer.id, nearest, objective) <= 0 && nodes.indexOf(peer) == -1) {
					chosenPeer = peer;
					nearest = peer.id;
				}
			}
			
			if (chosenPeer != null && nodes.indexOf(chosenPeer) == -1) {
				nodes.add(chosenPeer);
				added++;
				addedThisRound = true;
			}
		}
		
		return nodes;
	}
	
	protected boolean isNewListBetter(LinkedList<PeerData> oldList, LinkedList<PeerData> newList, byte[] objective) {
		if (newList.size() < 1) {
			return false;
		}
		
		PeerData closestOld = getClosestNodes(objective, 1, oldList).get(0);
		PeerData closestNew = getClosestNodes(objective, 1, newList).get(0);
		
		return compareClosest(closestNew.id, closestOld.id, objective) < 0 ? true : false;
	}

	public PeerData getPeer(byte[] id, long time) {
		int numList = getNumList(id);
		if (numList == -1) {
			PeerData peer = new PeerData();
			peer.id = this.id;
			peer.ip = this.ip;
			peer.port = this.port;
			peer.numPeer = this.numPeer;
			return peer;
		}
		
		for (PeerData peer: table[numList]) {
			if (peer == null) {
				continue;
			}
			
			if (Arrays.equals(peer.id, id)) {
				return peer;
			}
		}
		
		// We don't have PeerData locally. We've to ask other members
		LinkedList<PeerData> peers = getClosestNodes(id, Encaminador.ALFA);
		LinkedList<PeerData> newPeers = getClosestNodes(id, Encaminador.ALFA);
		System.out.println("ASKING");
		System.out.println("INIT: " + peers);
		for (PeerData peer: peers) {
			JSONArray array = new JSONArray(
					Util.request("http://"
							+ peer.ip
							+ ":"
							+ peer.port
							+ "/P2P/checkPeer?toPeer="
							+ peer.numPeer + "&id="
							+ Util.byteToString(id)
							+ "&time="
							+ time)
					);
			for (int i=0, length = array.length(); i < length; i++) {
				newPeers.add(new PeerData(array.getJSONObject(i)));
			}
		}
		peers = getClosestNodes(id, Encaminador.K, newPeers);
		
		int tries = 1;
		while(tries < Encaminador.NUM_BITS) {
			//System.out.println(peers);
			time = System.currentTimeMillis();
			for (PeerData peer: peers) {
				// I don't ask myself
				if (Arrays.equals(this.id, peer.id)) {
					continue;
				}
				
				JSONArray array = new JSONArray(
						Util.request("http://" 
								+ peer.ip
								+ ":"
								+ peer.port
								+ "/P2P/checkPeer?toPeer="
								+ peer.numPeer
								+ "&id="
								+ Util.byteToString(id)
								+ "&time="
								+ time)
						);
				for (int i=0, length = array.length(); i < length; i++) {
					newPeers.add(new PeerData(array.getJSONObject(i)));
				}
			}
			
			tries++;
			
			if (!isNewListBetter(peers, newPeers, id)) {
				break;
			}
			peers = getClosestNodes(id, Encaminador.K, newPeers);
		}
		
		for (PeerData peer: newPeers) {
			if (Arrays.equals(peer.id, id)) {
				return peer;
			}
		}
		
		// Peer non-existent
		return null;
	}
	
	public LinkedList<PeerData> checkPeer(byte[] id, long time) {
		if (receivedRequests.indexOf(Util.byteToString(id) + time) != -1) {
			return new LinkedList<PeerData>();
		}
		receivedRequests.add(Util.byteToString(id) + time);
		
		return getClosestNodes(id, Encaminador.K);
	}

}

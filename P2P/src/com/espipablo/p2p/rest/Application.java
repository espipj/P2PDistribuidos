package com.espipablo.p2p.rest;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import com.espipablo.p2p.model.Peer;
import com.espipablo.p2p.model.PeerData;

@Path("/")
@Singleton
public class Application {
	
	private Peer[] peers;
	private static final int TOTAL_PEERS = 3;
	private String ip;
	private String port;
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/inicializar")
	// http://localhost:8080/P2P/inicializar?ip=localhost&port=8080
	public String inicializarP2P(@QueryParam(value="ip") String ip, @QueryParam(value="port") String port) {
		this.peers = new Peer[TOTAL_PEERS];
		this.ip = ip;
		this.port = port;
		return "INICIALIZADO";
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/Peer")
	// http://localhost:8080/P2P/Peer?port=8080&peer=0&toPeer=0
	// http://localhost:8080/P2P/Peer?ip=localhost&port=8080&numPeer=1&toPeer=0
	// http://localhost:8080/P2P/Peer?ip=localhost&port=8080&numPeer=2&toPeer=0
	public String inicializar(
			@QueryParam(value="ip") String ip,
			@QueryParam(value="port") String port,
			@QueryParam(value="toPeer") int toPeer,
			@QueryParam(value="numPeer") int numPeer) throws SocketException, UnknownHostException {
		
		if (numPeer >= TOTAL_PEERS) {
			return "ERROR NUMPEER OUT OF BOUNDS";
		}
		
		this.peers[numPeer] = new Peer(ip, port, toPeer, this.ip, this.port, numPeer);
		return "PEER INICIALIZADO";
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/validateId")
	public String validate(
			@QueryParam(value="id") String id,
			@QueryParam(value="ip") String ip,
			@QueryParam(value="port") String port,
			@QueryParam(value="numPeer") int numPeer,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		if (this.peers[toPeer].getEncaminador().checkIfIdExists(id.getBytes(), ip, port, numPeer)) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("error", true);
			return jsonObject.toString();
		}
		
		PeerData peer = new PeerData();
		peer.id = this.peers[toPeer].getEncaminador().getId();
		peer.ip = this.ip;
		peer.port = this.port;
		peer.numPeer = toPeer;
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("error", false);
		jsonObject.put("table", this.peers[toPeer].getEncaminador().getRouteTableAsJSON());
		jsonObject.put("peer", peer.getAsJSON());
		return jsonObject.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/checkPeer")
	public String checkPeer(
			@QueryParam(value="id") String id,
			@QueryParam(value="time") long time,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}

		LinkedList<PeerData> peers = this.peers[toPeer].getEncaminador().checkPeer(id.getBytes(), time);
		JSONArray peersArr = new JSONArray();
		for (PeerData peer: peers) {
			peersArr.put(peer.getAsJSON());
		}
		return peersArr.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/checkUpperPeer")
	public String checkUpperPeer(
			@QueryParam(value="id") String id,
			@QueryParam(value="time") long time,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}

		LinkedList<PeerData> peers = this.peers[toPeer].getEncaminador().checkUpperPeer(id.getBytes(), time);
		JSONArray peersArr = new JSONArray();
		for (PeerData peer: peers) {
			peersArr.put(peer.getAsJSON());
		}
		return peersArr.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/checkLowerPeer")
	public String checkLowerPeer(
			@QueryParam(value="id") String id,
			@QueryParam(value="time") long time,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}

		LinkedList<PeerData> peers = this.peers[toPeer].getEncaminador().checkLowerPeer(id.getBytes(), time);
		JSONArray peersArr = new JSONArray();
		for (PeerData peer: peers) {
			peersArr.put(peer.getAsJSON());
		}
		return peersArr.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getPeer")
	// http://localhost:8080/P2P/getPeer?id=5057939219e015ad7022959c4a571680400585a8&toPeer=0
	public String getPeer(
			@QueryParam(value="id") String id,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		PeerData peer = this.peers[toPeer].getEncaminador().getPeer(id.getBytes(), System.currentTimeMillis());
		JSONObject jsonObject = new JSONObject();
		if (peer == null) {
			Object nullObj = null;
			jsonObject.put("error", true);
			jsonObject.put("peer", nullObj);
		} else {
			jsonObject.put("error", false);
			jsonObject.put("peer", peer.getAsJSON());
		}
		return jsonObject.toString();
	}
	
	// http://localhost:8080/P2P/getFilesTable?toPeer=0
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getFilesTable")
	public String validate(
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		return this.peers[toPeer].getFilesTableAsJSON().toString();
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/setUpper")
	public String setUpper(
			@QueryParam(value="id") String id,
			@QueryParam(value="ip") String ip,
			@QueryParam(value="port") String port,
			@QueryParam(value="numPeer") int numPeer,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		this.peers[toPeer].setUpper(id, ip, port, numPeer, false);
		return "DONE";
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/setLower")
	public String setLower(
			@QueryParam(value="id") String id,
			@QueryParam(value="ip") String ip,
			@QueryParam(value="port") String port,
			@QueryParam(value="numPeer") int numPeer,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		this.peers[toPeer].setLower(id, ip, port, numPeer, false);
		return "DONE";
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/put")
	// http://localhost:8080/P2P/put?file=file1.txt&toPeer=0
	public String setLower(
			@QueryParam(value="file") String file,
			@QueryParam(value="toPeer") int toPeer) {
		
		if (toPeer >= TOTAL_PEERS) {
			return "ERROR TOPEER OUT OF BOUNDS";
		}
		
		this.peers[toPeer].putFile(file);
		System.out.println(this.peers[0].getFilesTableAsJSON().toString());
		System.out.println(this.peers[1].getFilesTableAsJSON().toString());
		System.out.println(this.peers[2].getFilesTableAsJSON().toString());
		return "DONE";
	}

}

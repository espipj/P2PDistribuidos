package com.espipablo.p2p.rest;

import java.net.SocketException;
import java.net.UnknownHostException;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import com.espipablo.p2p.model.Peer;

@Path("/")
@Singleton
public class Application {
	
	private Peer[] peers;
	private static final int TOTAL_PEERS = 3;
	private String ip;
	private String port;

	/*@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/getNode")
	public String getNode(@QueryParam(value="id") String id) {
		// Buscar cualquier nodo con mi id
		byte[] bytes = id.getBytes();
		
		return "";
	}*/
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/Peer")
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
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/inicializar")
	public String inicializarP2P(@QueryParam(value="ip") String ip, @QueryParam(value="port") String port) {
		this.peers = new Peer[TOTAL_PEERS];
		this.ip = ip;
		this.port = port;
		return "INICIALIZADO";
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
		
		if (this.peers[toPeer].getEncaminador().checkIfIdExists(id.getBytes(), ip, port, numPeer)) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("error", true);
			return jsonObject.toString();
		}
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("error", false);
		jsonObject.put("table", this.peers[toPeer].getEncaminador().getRouteTableAsJSON());
		System.out.println(jsonObject.toString());
		return jsonObject.toString();
	}

}

package com.espipablo.p2p.model;

import java.net.SocketException;
import java.net.UnknownHostException;

import javax.validation.constraints.NotNull;

import com.espipablo.p2p.controller.Encaminador;

public class Peer {
	protected Encaminador encaminador;
	protected byte[] id;
	
	public Peer(String ip, @NotNull String port, int toPeer, String myIp, String myPort, int numPeer) throws SocketException, UnknownHostException {
		this.encaminador = new Encaminador(ip, port, toPeer, myIp, myPort, numPeer);
		this.id = this.encaminador.getId();
	}
	
	public Encaminador getEncaminador() {
		return this.encaminador;
	}
}

package com.espipablo.p2p.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Util {
	
	public static byte[] xor(byte[] bytes1, byte[] bytes2) {
		byte[] result = new byte[bytes1.length];
		
		if (bytes1.length != bytes2.length) {
			return null;
		}
		
		int i=0;
		for (byte b1: bytes1) {
			result[i] = (byte) (b1 ^ bytes2[i++]);
		}
		return result;
	}
	
	public static String request(String urlS) {
		URL url = null;
		try {
			url = new URL(urlS);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		try {
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			String result = "";
			while ((output = br.readLine()) != null) {
				result += output;
			}

			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}

		conn.disconnect();
		return "";
	}
	
	// Clase encargada de lanzar peticiones http en un hilo nuevo
	public static class RequestThread extends Thread {
		private String url;
		
		public RequestThread(String s) {
			this.url = s;
		}
		
		@Override
		public void run() {
			Util.request(this.url);
		}
		
	}
	
	/* Retorna un hash a partir de un tipo y un texto */
	// http://programacionextrema.com/2015/10/28/encriptar-en-md5-y-sha1-con-java/
	private static String getHash(String txt, String hashType) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance(hashType);
			byte[] array = md.digest(txt.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
						.substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
 
	/* Retorna un hash SHA1 a partir de un texto */
	public static String sha1(String txt) {
		return Util.getHash(txt, "SHA1");
	}
	
	public static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}
	
	// http://stackoverflow.com/questions/18931283/checking-individual-bits-in-a-byte-array-in-java
	public static int getBit(byte[] arr, int bit) {
		int index = bit / 8;  // Get the index of the array for the byte with this bit
		int bitPosition = bit % 8;  // Position of this bit in a byte
		
		return arr[index] >> (7-bitPosition) & 1;
	}
	
	public static String byteToString(byte[] b) {
		if (b == null) {
			return null;
		}
		
		return new String(b, StandardCharsets.UTF_8);
	}
	
	public static String getPrettyPrintString(byte[] byteArr) {
		if (byteArr == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (Byte b: byteArr) {
			sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
		}
		return sb.toString(); // 10000001
	}
	
	public static void prettyPrintByte(byte[] byteArr) {
		if (byteArr == null) {
			return;
		}
		System.out.println(Util.getPrettyPrintString(byteArr)); // 10000001
	}
	
	public static byte[] addToByte(byte[] byteArr) {
		boolean oneMore = true;
		byte[] result = new byte[byteArr.length];
		StringBuilder sb = new StringBuilder();
		for (int i=byteArr.length * 8 - 1, j = byteArr.length - 1; i >= 0; i--) {
			int bit = getBit(byteArr, i);
			if (oneMore) {
				if (bit == 1) {
					sb.append(0);
				} else {
					sb.append(1);
					oneMore = false;
				}
			} else {
				sb.append(bit);
			}
			
			if (sb.length() % 8 == 0) {
				result[j] = stringToBinaryEval(sb.reverse().toString());
				//System.out.println(sb.reverse().toString());
				sb.setLength(0);
				j--;
			}
		}
		
		return result;
	}
	
	public static byte[] removeToByte(byte[] byteArr) {
		boolean oneLess = true;
		byte[] result = new byte[byteArr.length];
		StringBuilder sb = new StringBuilder();
		for (int i=byteArr.length * 8 - 1, j = byteArr.length - 1; i >= 0; i--) {
			int bit = getBit(byteArr, i);
			if (oneLess) {
				if (bit == 1) {
					sb.append(0);
					oneLess = false;
				} else {
					sb.append(1);
				}
			} else {
				sb.append(bit);
			}
			
			if (sb.length() % 8 == 0) {
				result[j] = stringToBinaryEval(sb.reverse().toString());
				//System.out.println(sb.reverse().toString());
				sb.setLength(0);
				j--;
			}
		}
		
		return result;
	}
	
	public static byte stringToBinaryEval(String s) {
		return (byte) Integer.parseInt(s, 2);
	}
	
	public static byte[] getMaxByte(int length) {
		byte[] furtherBuilder = new byte[length];
		for (int i=0; i < length; i++) {
			furtherBuilder[i] = (byte) 0b11111111;
		}
		return furtherBuilder;
	}
	
	public static byte[] getMinByte(int length) {
		byte[] minBuilder = new byte[length];
		for (int i=0; i < length; i++) {
			minBuilder[i] = (byte) 0b00000000;
		}
		return minBuilder;
	}
	
	public static byte[] negate(byte[] b) {
		byte[] further = Util.getMaxByte(b.length);
		return Util.xor(b, further);
	}
	
	// http://stackoverflow.com/questions/43376533/byte-array-subtraction
	public static byte[] add(byte[] data1, byte[] data2) {
		byte[] result = new byte[data1.length];
		for (int i = data1.length - 1, overflow = 0; i >= 0; i--) {
			int v = (data1[i] & 0xff) + (data2[i] & 0xff) + overflow;
			result[i] = (byte) v;
			overflow = v >>> 8;
		}
		return result;
	}
	
	// http://stackoverflow.com/questions/43376533/byte-array-subtraction
	public static byte[] substract(byte[] data1, byte[] data2) {
		data2 = Util.negate(data2);
		byte[] result = new byte[data1.length];
		for (int i = data1.length - 1, overflow = 0; i >= 0; i--) {
			int v = (data1[i] & 0xff) + (data2[i] & 0xff) + overflow;
			result[i] = (byte) v;
			overflow = v >>> 8;
		}
		return result;
	}
	
	public static byte[] getFurther(byte[] data1) {
		byte[] difMinByte = Util.substract(data1, Util.getMinByte(data1.length));
		byte[] difMaxByte = Util.substract(Util.getMaxByte(data1.length), data1);
		if (Util.compareDistances(difMinByte, difMaxByte) < 0) {
			return difMaxByte;
		}
		return difMinByte;
	}

	public static int compareDistances(byte[] id1, byte [] id2) {		
		for (int i=0, length = id1.length * 8; i < length; i++) {
			if (Util.getBit(id1, i) != Util.getBit(id2, i)) {
				if (Util.getBit(id1, i) == 0) {
					return -1;
				}
				return 1;
			}
		}
		
		return 0;
	}
	
}

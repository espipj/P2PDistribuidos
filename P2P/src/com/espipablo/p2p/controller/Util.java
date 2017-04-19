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

        return arr[index] >> bitPosition & 1;
    }
    
    public static String byteToString(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
    
	
}

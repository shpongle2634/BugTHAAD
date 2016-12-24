package com.thhad.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;

import com.google.gson.JsonArray;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/*
 * 
 * 어플 요청 리스너
 * 1. 버그로그
 * 
 * 2. 아두이노 제어 
 *  On Off
 *  진돗개 발령 ( 난사 )
 * */
public class AppListenThread implements Runnable{

	ServerSocket httpserver=null;
	Socket androidsocket =null,arduinosocket=null;
	Lock lock = new ReentrantLock();

	public AppListenThread(ServerSocket httpserver, Socket arduino){
		this.httpserver=httpserver;
		this.arduinosocket =arduino;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {			
			while(true){
				char[] cbuf= new char[1024];
				// local 8080
				androidsocket=httpserver.accept();
				String cmd;
				System.out.println("App Request");

				InputStreamReader is = new InputStreamReader(androidsocket.getInputStream());
				int len= is.read(cbuf);
				cmd=new String(Arrays.copyOf(cbuf, len));				

				//모바일 요청 분기
				
				//1. 버그리포트
				if(cmd.equals("buglist")){
					System.out.println("buglist");
					Thread att= new Thread(new sendBugLog());
					att.start();
				}
				
				//전원 켜기 끄기
				else if(cmd.equals("onoff") && arduinosocket!=null){
					System.out.println("onoff");
					Thread tt= new Thread(new OnOff(arduinosocket));
					tt.start();

				}
				//진돗개 발령
				else if(cmd.equals("jindotgae")&& arduinosocket!=null){
					System.out.println("jindotgae");
					Thread tt= new Thread(new Jindotgae(arduinosocket));
					tt.start();
				}	


				System.out.println("done");


			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	//MongoDB에서 데이터를 불러와 모바일로 리턴
	private class sendBugLog implements Runnable{

		MongoCollection<Document> collection=null;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			MongoClientURI uri  = new MongoClientURI("mongodb://localhost:27017"); 
			JsonArray result=new JsonArray();
			MongoClient client = new MongoClient(uri);
			MongoDatabase database= client.getDatabase("embeded");		
			collection = database.getCollection("buglog");

			for (Document cur : collection.find()) {
				//			    System.out.println(cur.toJson());
				if(result.size()<15)
					result.add(cur.toJson());
			}

			try {
				OutputStreamWriter out= new OutputStreamWriter(androidsocket.getOutputStream());
				out.write(result.toString());
				System.out.println("send");
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(result.toString());
			System.out.println(result.toString().length());
			client.close();
		}
	}

	
	//아두이노로 패킷을 보내 전원 제어
	private class OnOff implements Runnable{

		Socket arduino=null;
		private char[] ONOFF_PACKET= {0xA0,0x0A,0x00,0x00,0x01,0x01,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x0A,0xA0};
		//4,5,6
		public OnOff(Socket arduinosocket) {
			// TODO Auto-generated constructor stub
			arduino=arduinosocket;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {

				lock.lock();
				OutputStream out=arduino.getOutputStream();
				for(int i =0; i<17 ; i++)
					out.write(ONOFF_PACKET[i]&0xff);
				out.flush();
				lock.unlock();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//진돗개 발령시 아두이노에 진돗개 플래그 패킷 전송
	private class Jindotgae implements Runnable{

		Socket arduino=null;
		private char[] JINDOTGAE_PACKET= {0xA0,0x0A,0x00,0x00,0x01,0x00,0x00,0x01,0x01,0x01,0x00,0x00,0x00,0x00,0x04,0x0A,0xA0};
		//4,7,8,9
		public Jindotgae(Socket arduinosocket) {
			// TODO Auto-generated constructor stub
			arduino=arduinosocket;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {
				lock.lock();
				OutputStream out=arduino.getOutputStream();
				for(int i =0; i<17 ; i++)
					out.write(JINDOTGAE_PACKET[i]&0xff);
				out.flush();
				lock.unlock();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

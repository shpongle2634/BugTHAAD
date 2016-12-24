package com.thhad.server;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Thhad_Center implements Runnable{
	private char[] START_PACKET= {0xA0,0x0A,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x0A,0xA0};
	private int detect=0;
	private Socket arduino;

	//아두이노와 통신하는 스레드 
	public Thhad_Center(Socket arduinosocket){
			this.arduino = arduinosocket;
	}
	

	//사드 센터 역할
	/*
	 * 1. 스레드 핸들러는 아두이노로 부터 Connection을 accept 하고 
	 * 2. 초음파 센서로부터의 벌레 탐지 거리와 벌레 탐지 플래그를 담은 패킷을 지속적으로 리턴받는다 (0.3초마다).
	 * 3. 패킷이 2회 연속으로 벌레를 탐지했다는 플래그가 1이면, 서버에 벌레를 발견했다는 로그를 남긴다.(Mongo DB 연동)
	 * 
	 * 4. 모바일요청을 Listen한다. (스레드 구현)
	 * 5. 모바일에서 벌레 탐지 로그를 요청시, 벌레를 탐지한로그를 리턴
	 * 6. 원격제어 요청시 1) 임베디드 ON/OFF  2)진돗개 발령 /해제에 해당하는 데이터를 아두이노에 전송한다.
	 * */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean packet_started=false;
		byte[] buf= new byte[4096];
		char[] packet_buffer= new char[20];
		int packet_pt=0;
		try {
//			Lock lock = new ReentrantLock();
			OutputStream senddata=arduino.getOutputStream();
			for(int i =0; i<17; i++)	
				senddata.write(START_PACKET[i]&0xff);
			//senddata.flush();
			
			System.out.println("Send Start");
			while(true){
				
				InputStream getdata= arduino.getInputStream();
				int read=0;
				if((read=getdata.read(buf))<0){
					continue;
				}

				if(read == 0) break;
				//아두이노 센서 패킷 수신부
				for(int i =0; i< read; i++){
					packet_buffer[packet_pt++]= (char) (buf[i]&0xff);
					//					System.out.print(buf[i]);
					if(packet_started){
						if(packet_buffer[packet_pt-2]==0x0A&&packet_buffer[packet_pt-1]==0xA0&&packet_pt==17){
							printPacket(packet_buffer);
							packet_pt=0;
							packet_started=false;
						}


						if(packet_pt>=20){
							packet_pt=0; packet_started=false;
							System.out.println("buffer overflow");
						}


					}else {
						if(packet_pt>=2){
							if(packet_buffer[0] == 0xA0 && packet_buffer[1] == 0x0A){
								packet_started =true;
							}else{
								packet_pt=0;
								System.out.println("StartPacket Error");
							}

						}
					}
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	private void printPacket(char[] packet){
		int id= packet[3];
		int distance = (packet[4]<<8)|packet[5];
		int flag= packet[6];
		char csc=  packet[14];
		char sum = 0;

		for(int i=2; i<14;i++){
			sum+=packet[i];
		}
		if(sum==csc){
			System.out.println("ID : " + id +" Distance : "+distance +" Detect : "+flag);
			if(flag==1) {
				detect++;
				if(detect == 3){//2회 연속으로 detect flag가 1인 경우 벌레 발견으로 판정. 
					SimpleDateFormat sf= new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
					String date= sf.format(new Date());
					System.out.println("벌레 탐지! : " + date);
					//DB에 저장
//					Thread applistener = new Thread(new InsertDBThread(id,date)); //MongoDB로 로그 기록
//					applistener.start();
				}
			}else detect=0;
		}
	}
	
	// 서버 메인
	/*
	 * 메인은 모바일을 위한 포트 8080 소켓서버와
	 * 아두이노를 위한 7777포트 소켓 서버 2개를 운영한다. 각각은 스레드로 운영하며, 각 packet 통신을 Read, write하는데
	 * 스레드로 구현한다.
	 * */
	public static void main(String[] args) {
		Socket arduinosocket= null;
		ServerSocket server= null, httpserver=null;
		try {
			httpserver = new ServerSocket(8080);
			server= new ServerSocket(7777);
			System.out.println("Server On");
			
			arduinosocket= server.accept(); //아두이노 통신 후에, 모바일이 접속 가능.
			if(arduinosocket !=null){
			Thread tt=new Thread(new AppListenThread(httpserver,arduinosocket));//App 리스너
			tt.start();
			System.out.println("ADK Connected");
			Thread t = new Thread(new Thhad_Center(arduinosocket));		 //Arduino 리스너
			t.start();
			t.join();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(arduinosocket != null) try { arduinosocket.close(); } catch(IOException e) {} 
			if(server != null) try { server.close(); } catch(IOException e) {} 
			if(httpserver != null) try { httpserver.close(); } catch(IOException e) {} 
		}
	}

}

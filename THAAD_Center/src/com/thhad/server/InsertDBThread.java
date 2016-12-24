package com.thhad.server;



import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class InsertDBThread implements Runnable{

	MongoCollection<Document> collection=null;
	String date;
	int id;
	public InsertDBThread(int id,String date) {
		// TODO Auto-generated constructor stub
//		this.server= httpserver;
		this.id=id;
		this.date=date;
	}

	
	//MongoDB에 UID와 로그를 기록한다
	@Override
	public void run() {
		// TODO Auto-generated method stub
		MongoClientURI uri  = new MongoClientURI("mongodb://localhost:27017"); 

		MongoClient client = new MongoClient(uri);
		MongoDatabase database= client.getDatabase("embeded");	//embeded 데이터로부터 로드	
		collection = database.getCollection("buglog");
		
		Document doc =new Document("user", id ).append("date", date);
		collection.insertOne(doc);
		
		client.close();

	}

}

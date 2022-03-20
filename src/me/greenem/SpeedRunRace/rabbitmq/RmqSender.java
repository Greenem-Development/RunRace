package me.greenem.SpeedRunRace.rabbitmq;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RmqSender {
	private static volatile RmqSender INSTANCE;
	
	public static RmqSender getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new RmqSender();
		}
		return INSTANCE;
	}
	
	private String queueName = null;
	private ConnectionFactory factory = null;
	private Connection connection = null;
	private Channel channel = null;
	private boolean hostIsSet = false;
	public boolean ready = false;
	
	public RmqSender() {
		factory = new ConnectionFactory();
		factory.setHost("localhost");
//		if(queueName!=null) {
//			ready = true;
//		}
	}
	
	public void setHost(){
		factory.setHost("localhost");
		hostIsSet = true;
	}
	
	public String getQueueName(){
		return queueName;
	}
	
	public void setQueueName(String queueName){
		this.queueName = queueName;
		onReady();
		try {
			channel.queueDeclare(queueName, false, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onReady() {
		ready = true;
		try {
			connection = factory.newConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			e1.printStackTrace();
		}
		try {
			channel = connection.createChannel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stopConnection() {
		try {
			connection.close();
			channel.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	public int send(String message) {
		if(!ready) {
			return -1;
		}
		try {
			channel.basicPublish("", queueName, null, message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sent \"" + message + "\" to " + queueName);
		return 0;
	}
}

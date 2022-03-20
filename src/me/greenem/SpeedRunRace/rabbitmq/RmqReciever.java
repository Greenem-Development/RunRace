package me.greenem.SpeedRunRace.rabbitmq;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import me.greenem.SpeedRunRace.MainFileSRR;
import me.greenem.SpeedRunRace.events.RabbitMqMessageEvent;
import me.greenem.SpeedRunRace.other.Sync;

public class RmqReciever implements Listener {
	private static volatile RmqReciever INSTANCE;
	
	public static RmqReciever getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new RmqReciever();
		}
		return INSTANCE;
	}
	
	private String queueName = null;
	private ConnectionFactory factory = null;
	private Connection connection = null;
	private Channel channel = null;
	private boolean hostIsSet = false;
	public boolean ready = false;
	
	public RmqReciever() {
		factory = new ConnectionFactory();
//		if(queueName!=null) {
//			ready = true;
//		}
		factory.setHost("localhost");
		hostIsSet = true;
	}
	
	public void setHost(){
		factory.setHost("localhost");
		hostIsSet = true;
	}
	
	public void setQueueName(String queueName){
		this.queueName = queueName;
		if(factory!=null && hostIsSet) {
			ready = true;
			onReady();
		}
	}
	
	public void onReady() {
		try {
			connection = factory.newConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			e1.printStackTrace();
		}
		try {
			channel = connection.createChannel();
			if(queueName!=null) {
				channel.queueDeclare(queueName, false, false, false, null);
			}
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
	
	public void start() {
	    System.out.println("Waiting for messages on " + queueName);
	    
	    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
	        String message = new String(delivery.getBody(), "UTF-8");
	        System.out.println("Received \"" + message + "\"");
	        parseMessage(message);
	    };
	    
	    CancelCallback cancelCallback = (String s) -> {
	    	System.out.println("canceled: " + s);
	    };
	    
	    try {
			channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
//	    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
//	    
//	    System.out.println(threadSet.toString());
	}
	
	private synchronized void parseMessage(String s) {
//		Sync.getInstance().call(s);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("SpeedRunRace"), new Runnable() {
			@Override
			public void run() {
				RabbitMqMessageEvent event = new RabbitMqMessageEvent(s);
			    Bukkit.getPluginManager().callEvent(event);
			}
		});
		
	}
}

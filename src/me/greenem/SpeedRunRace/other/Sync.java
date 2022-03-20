package me.greenem.SpeedRunRace.other;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import me.greenem.SpeedRunRace.events.RabbitMqMessageEvent;

public class Sync implements Listener {
	private static volatile Sync INSTANCE;
	
	public static Sync getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new Sync();
		}
		return INSTANCE;
	}
	
	public synchronized void call(String s){
		RabbitMqMessageEvent event = new RabbitMqMessageEvent(s);
	    Bukkit.getPluginManager().callEvent(event);
	}
	
}

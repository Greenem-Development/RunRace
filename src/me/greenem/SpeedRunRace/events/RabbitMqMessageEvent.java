package me.greenem.SpeedRunRace.events;

import org.bukkit.event.Cancellable;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RabbitMqMessageEvent extends Event {
	
	private String str;
	 
	public RabbitMqMessageEvent(String str) {
	    this.str = str;
	}
	 
	public String getStr() {
	    return this.str;
	}
	
	private static final HandlerList handlers = new HandlerList();
	 
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	 
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}

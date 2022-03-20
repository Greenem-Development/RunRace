package me.greenem.SpeedRunRace;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import me.greenem.SpeedRunRace.events.RabbitMqMessageEvent;
import me.greenem.SpeedRunRace.other.Sync;
import me.greenem.SpeedRunRace.rabbitmq.RmqReciever;
import me.greenem.SpeedRunRace.rabbitmq.RmqSender;

//import net.minecraft.server.v1_8_R3.PlayerSelector;

public class MainFileSRR extends JavaPlugin implements Listener {
	private Logger log = getLogger();
	private long lastSent = 0L;
	private long delay = 50L;
	private boolean enabled = false;
	private RmqReciever rmqReciever = new RmqReciever();
	private RmqSender rmqSender = new RmqSender();
	public static Map<String, ArmorStand> texts = new HashMap<>();
	boolean first = true;
	
	private static final String QUEUE_NAME = "srr_main";
	
	@Override
	public void onDisable() {
		//rmqReciever.stopConnection();
		//rmqSender.stopConnection();
		killAllArmorstands();
		log.info(ChatColor.RED + "SpeedRunRace plugin has been disabled.");
	}

	@Override
	public void onEnable() {
		log.info(ChatColor.GREEN + "SpeedRunRace plugin has been enabled!");
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(RmqReciever.getInstance(), this);
		getServer().getPluginManager().registerEvents(Sync.getInstance(), this);
//		if(rmqReciever==null) {
//			rmqReciever = new RmqReciever();
			rmqReciever.setQueueName(QUEUE_NAME);
			rmqReciever.start();
//		if(RmqSender==null) {
//			rmqSender = new RmqSender();
			rmqSender.setQueueName(QUEUE_NAME);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
//		if(first) {
//			rmqSender.send("first");
//			first = false;
//		}
		Player p = null;
		if(sender instanceof Player) {
			p = (Player) sender;
		}
		//Plugin plugin = getServer().getPluginManager().getPlugin("");
		if(cmd.getName().equalsIgnoreCase("srrstart")){
			if(!enabled) {
				enabled = true;
				sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "The game has been started!");
			}
			else {
				sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "The game has ALREADY been started!");
			}
		}
		else if(cmd.getName().equalsIgnoreCase("srrstop")){
			
			if(enabled) {
				enabled = false;
				killAllArmorstands();
				sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "The game has been stopped!");
			}
			else {
				sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "The game has ALREADY been stopped!");
			}
		}
		else if(cmd.getName().equalsIgnoreCase("srrlist")){
			rmqSender.send("list");
			String s = "$;list;";
			int size = getServer().getOnlinePlayers().size();
			int i = 1;
			if(size>0) {
				for (Player player : getServer().getOnlinePlayers()) {
					s+=p.getName();
					if(i<size) {
						s+=", ";
					}
					i++;
				}
			}
			rmqSender.send(s);
		}
		
//		if(cmd.getName().equalsIgnoreCase("srrsetplayer")){
//			boolean access = false;
//			if (!(sender instanceof Player)) {
//				if (sender instanceof ConsoleCommandSender) {
//					access = true;
//					//log.info(ChatColor.RED + "This comand is only for players.");
//				}
//			}
//			else {
//				if (player==null) {
//					access = true;
//				}
//			}
//			
//			if(access) {
//				
//			}
//		}
		return false;
	}
	
	public void killAllArmorstands() {
		for (ArmorStand a : texts.values()) {
			if(a!=null) {
				a.setHealth(0);
				texts.remove(a);
			}
		}
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		sendLocation(e.getPlayer());
	}
	
	public void sendLocation(Player p) {
		if(enabled) {
			if(rmqReciever==null || rmqSender==null) {
				log.info(rmqReciever + "" + rmqSender);
				return;
			}
			
			long nowTime = Calendar.getInstance().getTimeInMillis();
			if (nowTime - lastSent < delay) {
				return;
			}
			else {
				lastSent = nowTime;
			}

			//Player p = e.getPlayer();
			
			rmqSender.send(p.getName() + ";" + p.getLocation().getX() + ";" + p.getLocation().getY() + ";" + p.getLocation().getZ() + ";" + p.getWorld().getName());
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		if(enabled) {
			rmqSender.send(e.getPlayer().getName() + ";left");
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		sendLocation(e.getPlayer());
	}
	
	@EventHandler
	public void RabbitMqMessageEvent(RabbitMqMessageEvent e) {
		String[] args = e.getStr().split(";");
		//System.out.println(args.length);
		if(args.length==1) {
			if(args[0].equals("start")) {
				enabled = true;
			}
			else if(args[0].equals("stop")) {
				enabled = false;
				killAllArmorstands();
			}
			else if(args[0].equals("list")) {
				String s = "$;list;";
				int size = getServer().getOnlinePlayers().size();
				int i = 1;
				if(size>0) {
					for (Player p : getServer().getOnlinePlayers()) {
						s+=p.getName();
						if(i<size) {
							s+=", ";
						}
						i++;
					}
				}
				rmqSender.send(s);
			}
		}
		else if(args.length==5) {
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {                               //uncomment!
				if(p.getName().equals(args[0])) {
					return;
				}
			}
			//if(Bukkit.getServer().getOnlinePlayers().contains(args))
//			System.out.println("TEXTS: " + ChatColor.GOLD + MainFileSRR.texts.toString());
			if(MainFileSRR.texts.containsKey(args[0])) {
				World w = Bukkit.getWorld(args[4]);
				MainFileSRR.texts.get(args[0]).teleport(new Location(w, Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3])));
			}
			else {
				World w = Bukkit.getWorld(args[4]);
				ArmorStand stand = (ArmorStand) w.spawnEntity(new Location(w, Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3])).add(0, -0.2, 0), EntityType.ARMOR_STAND);
				stand.setInvisible(true);
				stand.setInvulnerable(true);
				stand.setGravity(false);
				stand.setCustomName(args[0]);
				stand.setCustomNameVisible(true);
				MainFileSRR.texts.put(args[0], stand);
			}
		}
		else if(args.length==2) {
			String pName = args[0];
			if(args[1].equals("left") && MainFileSRR.texts.containsKey(pName)) {
				MainFileSRR.texts.get(pName).setHealth(0);
				MainFileSRR.texts.remove(pName);
			}
		}
		else if(args.length==3) {
			if(args[0].equals("$")) {
				if(args[1].equals("list")) {
					getServer().broadcastMessage(args[2]);
				}
			}
		}
	}
}
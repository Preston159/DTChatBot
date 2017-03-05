package com.preston159.dtbot;

import org.jibble.pircbot.*;

import de.btobastian.javacord.entities.Channel;

public class IrcBot extends PircBot {
	
	String name;
	String sID;
	
	public IrcBot(String user, String serverID) {
		this.setName(user);
		name = user;
		sID = serverID;
	}
	
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if(sender.equalsIgnoreCase(name))
			return;
		mReceive(channel, sender, message);
	}
	
	public void mReceive(String channel, String sender, String message) {
		if(!sender.equalsIgnoreCase(Main.TWITCH_USERNAME))
			Main.sendMessage(((Channel) Main.servers.get(sID)[Main.dChannel]), sender + ": " + message);
	}
	
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		
	}
	
	public void switchChannel(String from, String to) {
		if(from != null && from.substring(0, 1).equals("#")) {
			this.sendRawLineViaQueue("PART " + from);
		}
		this.joinChannel(to);
	}
	
}

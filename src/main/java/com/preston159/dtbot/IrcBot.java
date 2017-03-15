/**
 * Copyright (C) 2017 Preston Petrie
 * 
 * This file is part of DTChatBot.
 * 
 * DTChatBot is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DTChatBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 **/

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
		if(!sender.equalsIgnoreCase(Auth.TWITCH_USERNAME))
			Main.sendMessage(((Channel) Main.servers.get(sID)[Main.dChannel]), "**" + Main.escape(sender) + "**: " + Main.escape(message));
	}
	
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		
	}
	
	public void switchChannel(String from, String to) {
		if(from != null && from.substring(0, 1).equals("#")) {
			this.sendRawLineViaQueue("PART " + from);
		}
		if(to != null) {
			this.joinChannel(to);
		}
	}
	
}

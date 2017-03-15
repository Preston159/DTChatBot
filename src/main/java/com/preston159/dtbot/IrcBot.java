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
	
	/**
	 * The username used by the <code>IrcBot</code>
	 */
	String name;
	/**
	 * The ID of the Discord server to which this <code>IrcBot</code> is connected
	 */
	String sID;
	
	public IrcBot(String user, String serverID) {
		this.setName(user);
		name = user;
		sID = serverID;
	}
	
	/**
	 * Run when the <code>IrcBot</code> receives a message on its channel and sends this message on the Discord channel
	 */
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if(sender.equalsIgnoreCase(name))
			return;
		Main.sendMessage(((Channel) Main.servers.get(sID)[Main.dChannel]), "**" + Main.escape(sender) + "**: " + Main.escape(message));
	}
	
	/**
	 * Run when the <code>IrcBot</code> receives a private message
	 */
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		
	}
	
	/**
	 * Switches the Twitch channel to which the <code>IrcBot</code> is connected
	 * @param from	The Twitch channel from which the <code>IrcBot</code> is disconnecting
	 * @param to	The Twitch channel to which the <code>IrcBot</code> is connecting
	 * Channels must begin with '#'
	 */
	public void switchChannel(String from, String to) {
		if(from != null && from.substring(0, 1).equals("#")) {
			this.sendRawLineViaQueue("PART " + from);
		}
		if(to != null) {
			this.joinChannel(to);
		}
	}
	
}

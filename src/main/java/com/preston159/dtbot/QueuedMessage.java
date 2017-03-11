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

import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;

public class QueuedMessage {
	
	public String message;
	public Channel channel;
	public Server server;
	
	public QueuedMessage(String _message, Channel _channel, Server _server) {
		message = _message;
		channel = _channel;
		server = _server;
	}
	
	public QueuedMessage(String _message, Channel _channel) {
		message = _message;
		channel = _channel;
		server = channel.getServer();
	}
	
}

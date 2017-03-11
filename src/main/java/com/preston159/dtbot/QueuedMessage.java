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

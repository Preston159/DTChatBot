package com.preston159.dtbot;

import java.io.IOException;
import java.util.HashMap;

import org.jibble.pircbot.IrcException;

import com.preston159.dtbot.IrcBot;
import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.listener.message.MessageCreateListener;

public class Main {
	
	static final String DISCORD_USERNAME = "dtchatbot";
	static final String DISCORD_API_TOKEN = "<redacted>";
	public static final String TWITCH_USERNAME = "dtchatbot";
	static final String TWITCH_OAUTH = "<redacted>";
	
	public static final int sID = 0;
	public static final int tChannel = 1;
	public static final int dChannel = 2;
	public static final int ircBot = 3;
	public static final int reqRole = 4;
	
	public static HashMap<String, Object[]> servers = new HashMap<String, Object[]>();
	
	public static DiscordAPI api = null;
	
	public static void main(String[] args) {
		api = Javacord.getApi(DISCORD_API_TOKEN, true);
		api.connect(new FutureCallback<DiscordAPI>() {
			
			public void onSuccess(final DiscordAPI api) {
				api.registerListener(new MessageCreateListener() {
					public void onMessageCreate(DiscordAPI api, Message message) {
						if(message.getContent().length() < 3 || !message.getContent().substring(0, 3).equalsIgnoreCase("dt ")) {
							return;
						}
						String[] messageA = message.getContent().substring(3).toLowerCase().split(" ");
						if(messageA.length == 0)
							return;
						boolean hasRole = false;
						final Channel channel = message.getChannelReceiver();
						Server server = channel.getServer();
						if(!servers.containsKey(server.getId())) {
							hasRole = true;
						} else if(servers.get(server.getId())[reqRole] != null) {
							String roleID = (String) servers.get(server.getId())[reqRole];
							for(Role r : message.getAuthor().getRoles(server)) {
								if(r.getId().equalsIgnoreCase(roleID))
									hasRole = true;
							}
						} else {
							hasRole = true;
						}
						if(messageA[0].equals("setchannel")) BLOCK: {
							if(!hasRole) {
								sendMessage(channel, "You don't have permission to do that, " + message.getAuthor().getName());
								break BLOCK;
							}
							if(messageA.length == 1) {
								sendMessage(channel, "Discord channel set to #" + channel.toString());
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), null, channel);
								}
								servers.get(message.getChannelReceiver().getServer().getId())[dChannel] = channel;
							} else {
								String twitchChannel = "#" + messageA[1].toLowerCase();
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), twitchChannel, null);
								}
								String serverID = message.getChannelReceiver().getServer().getId();
								String oldChannel = (String) servers.get(serverID)[tChannel];
								servers.get(serverID)[tChannel] = twitchChannel;
								((IrcBot) servers.get(serverID)[ircBot]).switchChannel(oldChannel, twitchChannel);
								if(oldChannel != null) {
									sendMessage(channel, "Twitch twitchChannel changed from " + oldChannel.substring(1)
											+ " to " + twitchChannel.substring(1));
								} else {
									sendMessage(channel, "Twitch twitchChannel set to " + twitchChannel.substring(1));
								}
							}
						} else if(messageA[0].equals("about")) {
							Runnable task = () -> {
								sendMessage(channel, "I am a bot which relays Twitch chat to a Discord channel\n" +
										"I am currently in beta, so please be nice\n" +
										"I currently don't save data, so if I'm restarted or crash you will have to reset me\n" +
										"To set the required role to change my settings, use `dt setrole <role name>`\n" +
										"Currently, you can set the required role to one you aren't in, so be careful!\n" +
										"To set the Discord channel you want me to relay chat to, use `dt setchannel` in that channel\n" +
										"To set the Twitch chat you want me to relay from, use `dt setchannel <username>`");

							};
							Thread thread = new Thread(task);
							thread.start();
						} else if(messageA[0].equals("setrole")) BLOCK: {
							if(!hasRole) {
								sendMessage(channel, "You don't have permission to do that, " + message.getAuthor().getName());
								break BLOCK;
							}
							if(messageA.length < 2) {
								servers.get(server.getId())[reqRole] = null;
								sendMessage(channel, "Role requirement removed");
								break BLOCK;
							}
							String newRole = messageA[1];
							for(Role r : server.getRoles()) {
								if(r.getName().equalsIgnoreCase(newRole)) {
									servers.get(server.getId())[reqRole] = r.getId();
									sendMessage(channel, "Required role changed to " + r.getName());
									break;
								}
							}
						}
					/*	else if(!message.getAuthor().getName().equalsIgnoreCase(DISCORD_USERNAME) &&
								message.getContent().substring(0, 1) != "!") {
							String serverID = message.getChannelReceiver().getServer().getId();
							((IrcBot) servers.get(serverID)[ircBot])
									sendMessage(channel, (String) servers.get(serverID)[tChannel], message.getAuthor().getName() + ": " + message.getContent());
						}	*/	//allow bot to relay from Discord channel to Twitch channel, disabled due to rate limiting
					}
				});
				
				
				
			}

			public void onFailure(Throwable t) {
				t.printStackTrace();
			}
			
		});
	}
	
	public static IrcBot createBot(String serverID) {
		IrcBot bot = new IrcBot(TWITCH_USERNAME, serverID);
		bot.setVerbose(false);
		try {
			bot.connect("irc.twitch.tv", 6667, TWITCH_OAUTH);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
		bot.setMessageDelay(2000l);
		return bot;
	}
	
	private static void addServer(Object... server) {
		Object[] record = new Object[5];
		record[0] = server[0];
		record[1] = server[1];
		record[2] = server[2];
		record[3] = server.length == 3 ? createBot((String) record[0]) : server[3];
		record[4] = null;
		servers.put((String) record[0], record);
	}
	
	public static void sendMessage(Channel channel, String message) {
		channel.sendMessage("\u200B" + message);
	}
}

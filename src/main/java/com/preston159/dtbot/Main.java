package com.preston159.dtbot;

import java.io.IOException;
import java.util.HashMap;

import org.jibble.pircbot.IrcException;

import com.preston159.dtbot.IrcBot;
import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.message.Message;
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
	
	public static HashMap<String, Object[]> servers = new HashMap<String, Object[]>();
	
	public static DiscordAPI api = null;
	
	public static void main(String[] args) {
		api = Javacord.getApi(DISCORD_API_TOKEN, true);
		api.connect(new FutureCallback<DiscordAPI>() {
			
			public void onSuccess(final DiscordAPI api) {
				api.registerListener(new MessageCreateListener() {
					public void onMessageCreate(DiscordAPI api, Message message) {
						String[] messageA = message.getContent().toLowerCase().split(" ");
						if(messageA.length == 0)
							return;
						if(messageA[0].equals("!setchannel")) {
							if(messageA.length == 1) {
								Channel channel = message.getChannelReceiver();
								message.getChannelReceiver().sendMessage("Discord channel set to #" + channel.toString());
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), null, channel);
								}
								servers.get(message.getChannelReceiver().getServer().getId())[dChannel] = channel;
							} else {
								String channel = "#" + messageA[1].toLowerCase();
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), channel, null);
								}
								String serverID = message.getChannelReceiver().getServer().getId();
								String oldChannel = (String) servers.get(serverID)[tChannel];
								servers.get(serverID)[tChannel] = channel;
								((IrcBot) servers.get(serverID)[ircBot]).switchChannel(oldChannel, channel);
								if(oldChannel != null) {
									message.getChannelReceiver().sendMessage("Twitch channel changed from " + oldChannel.substring(1)
											+ " to " + channel.substring(1));
								} else {
									message.getChannelReceiver().sendMessage("Twitch channel set to " + channel.substring(1));
								}
							}
						} else if(messageA[0].equals("!about")) {
							final Channel channel = message.getChannelReceiver();
							Runnable task = () -> {
								channel.sendMessage("I am a bot which relays Twitch chat to a Discord channel\n" +
										"I am currently in beta, so please be nice\n" +
										"I currently don't save data, so if I'm restarted or crash you will have to reset me\n" +
										"There's currently no way to restrict my usage, so anyone on your server can change my settings\n" +
										"To set the Discord channel you want me to relay chat to, use `!setchannel`\n" +
										"To set the Twitch chat you want me to relay from, use `!setchannel <username>`");

							};
							Thread thread = new Thread(task);
							thread.start();
						}
					/*	else if(!message.getAuthor().getName().equalsIgnoreCase(DISCORD_USERNAME) &&
								message.getContent().substring(0, 1) != "!") {
							String serverID = message.getChannelReceiver().getServer().getId();
							((IrcBot) servers.get(serverID)[ircBot])
									.sendMessage((String) servers.get(serverID)[tChannel], message.getAuthor().getName() + ": " + message.getContent());
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
		Object[] record = new Object[4];
		record[0] = server[0];
		record[1] = server[1];
		record[2] = server[2];
		record[3] = server.length == 3 ? createBot((String) record[0]) : server[3];
		servers.put((String) record[0], record);
	}
}

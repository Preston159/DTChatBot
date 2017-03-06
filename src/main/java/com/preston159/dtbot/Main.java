package com.preston159.dtbot;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jibble.pircbot.IrcException;

import com.preston159.dtbot.IrcBot;
import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
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
	
	static HashMap<String, Long> debugTime = new HashMap<String, Long>();
	static HashMap<String, Long> aboutTime = new HashMap<String, Long>();
	
	public static DiscordAPI api = null;
	
	public static void main(String[] args) {
		api = Javacord.getApi(DISCORD_API_TOKEN, true);
		api.connect(new FutureCallback<DiscordAPI>() {
			
			public void onSuccess(final DiscordAPI api) {
				FileManager.loadAll();
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
								sendMessage(channel, "Discord channel set to #" + channel.getName());
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), null, channel);
								}
								servers.get(message.getChannelReceiver().getServer().getId())[dChannel] = channel;
							} else {
								String twitchChannel = null;
								if(!messageA[1].equalsIgnoreCase("none")) {
									twitchChannel = "#" + messageA[1].toLowerCase();
								}
								if(!servers.containsKey(message.getChannelReceiver().getServer().getId())) {
									addServer(message.getChannelReceiver().getServer().getId(), twitchChannel, null);
								}
								String serverID = message.getChannelReceiver().getServer().getId();
								String oldChannel = (String) servers.get(serverID)[tChannel];
								servers.get(serverID)[tChannel] = twitchChannel;
								((IrcBot) servers.get(serverID)[ircBot]).switchChannel(oldChannel, twitchChannel);
								if(twitchChannel == null) {
									twitchChannel = "#none";
								}
								if(oldChannel != null) {
									sendMessage(channel, "Twitch channel changed from " + oldChannel.substring(1)
											+ " to " + twitchChannel.substring(1));
								} else {
									sendMessage(channel, "Twitch channel set to " + twitchChannel.substring(1));
								}
							}
						} else if(messageA[0].equals("about")) {
							Long time = System.currentTimeMillis() / 1000L;
							Long newTime = time + new Integer(60 * 10).longValue();
							boolean run = false;
							if(!aboutTime.containsKey(server.getId())) {
								run = true;
							} else {
								run = time > aboutTime.get(server.getId());
								if(run)
									System.out.println(time + ">" + aboutTime.get(server.getId()));
								else
									System.out.println(time + "<" + aboutTime.get(server.getId()));
							}
							if(run) {
								Runnable task = () -> {
									sendMessage(channel, "I am a bot which relays Twitch chat to a Discord channel\n" +
											"I am currently in beta, so please be nice\n" +
											"To set the required role to change my settings, use `dt setrole <role name>`\n" +
											"Currently, you can set the required role to one you aren't in, so be careful!\n" +
											"To set the Discord channel you want me to relay chat to, use `dt setchannel` in that channel\n" +
											"To set the Twitch chat you want me to relay from, use `dt setchannel <username>`\n" +
                                            "To disconnect from Twitch chat, use `dt setchannel none`\n" +
											"To disable my relaying, use `dt setchannel none`\n" +
											"You can add me to your server here: https://discordapp.com/oauth2/authorize?client_id=287319485675864064&scope=bot&permissions=0\n" +
											"My owner is Preston159, and you can find my source here: https://github.com/Preston159/DTChatBot");

								};
								Thread thread = new Thread(task);
								thread.start();
								aboutTime.put(server.getId(), newTime);
							}
							message.getAuthor().sendMessage("I am a bot which relays Twitch chat to a Discord channel\n" +
									"I am currently in beta, so please be nice\n" +
									"To set the required role to change my settings, use `dt setrole <role name>`\n" +
									"Currently, you can set the required role to one you aren't in, so be careful!\n" +
									"To set the Discord channel you want me to relay chat to, use `dt setchannel` in that channel\n" +
									"To set the Twitch chat you want me to relay from, use `dt setchannel <username>`\n" +
									"You can add me to your server here: https://discordapp.com/oauth2/authorize?client_id=287319485675864064&scope=bot&permissions=0\n" +
									"My owner is Preston159, and you can find my source here: https://github.com/Preston159/DTChatBot");
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
						} else if(messageA[0].equals("clear")) {
							//work in progress
							int num = 100;
							if(messageA.length > 1) {
								try {
									num = Integer.valueOf(messageA[1]);
								} catch(NumberFormatException e) { }
							}
							if(num > 100)
								num = 100;
							Future<MessageHistory> fmh = channel.getMessageHistory(num);
							Runnable task = () -> {
								MessageHistory mh = null;
								try {
									mh = fmh.get();
								} catch (InterruptedException | ExecutionException e) {
									sendMessage(channel, "Unable to delete messages");
								}
								if(mh == null)
									return;
								Collection<Message> messages = mh.getMessages();
								channel.bulkDelete(messages.toArray(new Message[messages.size()]));
								System.out.println(messages);
							};
							Thread thread = new Thread(task);
							thread.start();
						} else if(messageA[0].equals("savestate")) {
							User author = message.getAuthor();
							if(author.getName().equals("Preston159") && author.getDiscriminator().equals("6030")) {
								FileManager.saveAll();
								sendMessage(channel, "Servers saved");
							}
						} else if(messageA[0].equals("debug")) {
							Long time = System.currentTimeMillis() / 1000L;
							Long newTime = time + new Integer(60 * 10).longValue();
							boolean run = false;
							if(!debugTime.containsKey(server.getId())) {
								run = true;
							} else {
								run = time > debugTime.get(server.getId());
							}
							if(run) {
								sendMessage(channel, server.getId() + ":" + channel.getId() + "\n" +
										"This command will be disabled for 10 minutes to prevent spamming");
								debugTime.put(server.getId(), newTime);
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
	
	public static void addServer(Object... server) {
		Object[] record = new Object[5];
		record[0] = server[0];
		record[1] = server[1];
		record[2] = server[2];
		record[3] = server.length == 3 ? createBot((String) record[0]) : server[3];
		record[4] = null;
		servers.put((String) record[0], record);
	}
	
	public static void reloadServer(Object[] server, String role) {
		addServer(server);
		if(!role.equals("null"))
			servers.get((String) server[0])[reqRole] = role;
		if(!((String) server[tChannel]).equals("null")) {
			((IrcBot) servers.get((String) server[0])[ircBot]).switchChannel(null, (String) server[tChannel]);
		}
	}
	
	public static void sendMessage(Channel channel, String message) {
		channel.sendMessage("\u200B" + message);
	}
	
	public static String escape(String string) {
		string = string.replace("\\", "\\\\").replace("*", "\\*").replace("~", "\\~").replace("_", "\\_");
		return string;
	}
}

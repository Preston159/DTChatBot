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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import de.btobastian.javacord.utils.LoggerUtil;

public class Main {
	
	/**
	 * The array location of the server's ID in <code>servers</code>
	 * Type: <code>String</code>
	 */
	public static final int sID = 0;
	/**
	 * The array location of the server's Twitch channel in <code>servers</code>
	 * Type: <code>String</code>
	 */
	public static final int tChannel = 1;
	/**
	 * The array location of the server's Discord channel in <code>servers</code>
	 * Type: <code>Channel</code>
	 */
	public static final int dChannel = 2;
	/**
	 * The array location of the server's <code>IrcBot</code> in <code>servers</code>
	 * Type: <code>IrcBot</code>
	 */
	public static final int ircBot = 3;
	/**
	 * The array location of the server's required role in <code>servers</code>
	 * Type: <code>String</code>
	 */
	public static final int reqRole = 4;
	/**
	 * The array location of the server's Twitch account, if not the default
	 * Type: <code>String</code> or <code>null</code>
	 */
	public static final int twitchAcct = 5;
	/**
	 * The array location of the server's Twitch account Oauth key, if not the default
	 * Type: <code>String</code> or <code>null</code>
	 */
	public static final int twitchOauth = 6;
	
	/**
	 * A <code>HashMap</code> containing the information needed for Discord server and Twitch IRC communication
	 */
	public static volatile HashMap<String, Object[]> servers = new HashMap<String, Object[]>();
	/**
	 * The number of servers to which the bot is currently connected
	 */
	static int numServers = 0;
	/**
	 * The total number of users on the servers to which the bot is currently connected
	 */
	static int numUsers = 0;
	/**
	 * The Unix timestamp at which the bot connected
	 */
	static long startTime = 0;
	
	/**
	 * A <code>HashMap</code> containing the time at which the "about" command can next be run on each server
	 */
	static HashMap<String, Long> aboutTime = new HashMap<String, Long>();
	
	/**
	 * A <code>HashMap</code> mapping a user's ID to the server for which a Twitch oauth is being awaited
	 */
	static HashMap<String, String> awaitingOauth = new HashMap<String, String>();
	
	/**
	 * The prefix for running bot commands
	 * Can be any length and does not have to end in a space character
	 */
	private static final String commandPrefix = "dt ";
	
	/**
	 * The <code>DiscordAPI</code> through which the bot is connected
	 */
	public static DiscordAPI api = null;
	
	public static void main(String[] args) {
		api = Javacord.getApi(Auth.DISCORD_API_TOKEN, true);
		api.connect(new FutureCallback<DiscordAPI>() {
			
			public void onSuccess(final DiscordAPI api) {
				startTime = System.currentTimeMillis() / 1000l;
				LoggerUtil.setDebug(false);
				FileManager.loadAll();
				MessageQueue.start();
				Runnable gameTask = () -> {
					int count = 0;
					while(true) {
						api.setGame("dt help");
						if(--count < 0) {
							int servers = 0;
							Collection<User> userList = new ArrayList<User>();
							for(Server s : api.getServers()) {
								userList.addAll(s.getMembers());
								servers++;
							}
							numUsers = userList.size();
							for(User u : userList) {
								if(u.isBot())
									numUsers--; //don't count bots
							}
							numServers = servers;
							count = 5;
							//TODO: fix saving servers right after loading
							FileManager.saveAll();
						}
						try {
							TimeUnit.SECONDS.sleep(60);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				};
				Thread gameThread = new Thread(gameTask);
				gameThread.start();
				
				api.registerListener(new MessageCreateListener() {
					public void onMessageCreate(DiscordAPI api, Message message) {
						final Channel channel = message.getChannelReceiver();
						
						if(message.isPrivateMessage()) {
							String userId = message.getAuthor().getId();
							if(awaitingOauth.containsKey(userId)) {
								String serverId = awaitingOauth.get(userId);
								Object[] s = servers.get(awaitingOauth.get(userId));
								Thread createBot = new Thread(() -> {
									String oauth = message.getContent();
									IrcBot bot = null;
									String user = (String) s[twitchAcct];
									if(user == null)
										return;
									bot = createBot(serverId, user, oauth);
									try {
										TimeUnit.SECONDS.sleep(5);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									if(!bot.isConnected()) {
										message.getAuthor().sendMessage("Failed to login using specified Twitch credentials");
										bot.dispose();
									} else {
										message.getAuthor().sendMessage("Twitch login successful");
										s[twitchOauth] = oauth;
										((IrcBot) s[ircBot]).dispose();
										bot.switchChannel(null, (String) s[tChannel]);
										s[ircBot] = bot;
									}
								});
								createBot.start();
								awaitingOauth.remove(userId);
							}
							return;
						}
						
						Server server = channel.getServer();
						
						if(message.getContent().length() < commandPrefix.length() ||
								!message.getContent().substring(0, commandPrefix.length()).equalsIgnoreCase(commandPrefix)) {
							if(!message.getAuthor().getName().equalsIgnoreCase(Auth.DISCORD_USERNAME)) {
								if(((IrcBot) servers.get(server.getId())[ircBot]).getName().equals(Auth.TWITCH_USERNAME)) {
									return;
								}
								String serverID = message.getChannelReceiver().getServer().getId();
								((IrcBot) servers.get(serverID)[ircBot]).
										sendMessage((String) servers.get(serverID)[tChannel], message.getAuthor().getName() + ": " + message.getContent());
							}
							return;
						}
						
						String[] messageA = message.getContent().substring(commandPrefix.length()).toLowerCase().split(" ");
						if(messageA.length == 0)
							return;
						boolean hasRole = false;
						if(!servers.containsKey(server.getId()) || servers.get(server.getId())[reqRole] == null) {
							try {
								hasRole = message.getAuthor().equals(server.getOwner().get());
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						} else if(servers.get(server.getId())[reqRole] != null) {
							String roleID = (String) servers.get(server.getId())[reqRole];
							Role role = server.getRoleById(roleID);
							if(role == null) {
								servers.get(server.getId())[reqRole] = null;
								sendMessage(channel, "The administrative role has been deleted; only the owner of this server can now change my settings.");
								try {
									hasRole = message.getAuthor().equals(server.getOwner().get());
								} catch (InterruptedException | ExecutionException e) {
									e.printStackTrace();
								}
							} else {
								try {
									hasRole = message.getAuthor().getRoles(server).contains(role) || message.getAuthor().equals(server.getOwner().get());
								} catch (InterruptedException | ExecutionException e) {
									e.printStackTrace();
								}
							}
						}
						if(messageA[0].equals("setchannel")) BLOCK: {
							//TODO: change permission error behavior
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
						} else if(messageA[0].equals("about") || messageA[0].equals("help")) {
							Long time = System.currentTimeMillis() / 1000l;
							Long newTime = time + new Integer(60 * 10).longValue();
							boolean run = false;
							if(!aboutTime.containsKey(server.getId())) {
								run = true;
							} else {
								run = time > aboutTime.get(server.getId());
							}
							String uptime = Util.time(startTime);
							//TODO: separate info and help commands
							String aboutMessage = "I am a bot which relays Twitch chat to a Discord channel\n" +
									"I am currently in beta, so please be nice\n" +
									"To set the required role to change my settings, use `dt setrole <role name>`\n" +
									"Currently, you can set the required role to one you aren't in, so be careful!\n" +
									"To set the Discord channel you want me to relay chat to, use `dt setchannel` in that channel\n" +
									"To set the Twitch chat you want me to relay from, use `dt setchannel <username>`\n" +
									"To disconnect from Twitch chat, use `dt setchannel none`\n" +
									"To connect me to your own Twitch account and allow message forwarding from Discord to Twitch, use `dt twitch login <twitch username>`\n" +
									"You can add me to your server here: https://discordapp.com/oauth2/authorize?client_id=287319485675864064&scope=bot&permissions=0\n" +
									"My owner is Preston159#6030, and you can find my source here: https://github.com/Preston159/DTChatBot\n" +
									"I am currently in use on `" + numServers + "` servers for a total of `" + numUsers + "` users\n" +
									"Uptime: " + uptime;
							if(run) {
								Runnable task = () -> {
									sendMessage(channel, aboutMessage);

								};
								Thread thread = new Thread(task);
								thread.start();
								aboutTime.put(server.getId(), newTime);
							}
							message.getAuthor().sendMessage(aboutMessage);
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
						} else if(messageA[0].equals("clear")) BLOCK: {
							//work in progress
							int num = 100;
							if(messageA.length > 1) {
								try {
									num = Integer.valueOf(messageA[1]);
								} catch(NumberFormatException e) {
									break BLOCK;
								}
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
							Runnable task = () -> {
								User owner;
								try {
									owner = server.getOwner().get();
								} catch (InterruptedException | ExecutionException e) {
									return;
								}
								if(message.getAuthor().equals(owner)) {
									owner.sendMessage(server.getId() + ":" + channel.getId());
								} else {
									message.getAuthor().sendMessage("Only the owner of the server can run this command.");
								}
							};
							Thread thread = new Thread(task);
							thread.start();
							
						} else if(messageA[0].equals("twitch")) BLOCK: {
							if(!hasRole) {
								break BLOCK;
							}
							Object[] s = servers.get(server.getId());
							if(messageA.length == 1) {
								IrcBot bot = (IrcBot) s[ircBot];
								if(!bot.isConnected()) {
									sendMessage(channel, "Currently not connected to Twitch.  Use `" + commandPrefix + "twitch default` to reconnect");
								} else {
									sendMessage(channel, "Currently connected to Twitch with account " + bot.getName() + " on channel " + 
											((String) s[tChannel]).substring(1));
								}
							} else if(messageA[1].equals("default")) {
								if((s[twitchAcct] == null || s[twitchOauth] == null) && ((IrcBot) servers.get(server.getId())[ircBot]).isConnected()) {
									break BLOCK;
								}
								((IrcBot) s[ircBot]).dispose();
								s[twitchAcct] = null;
								s[twitchOauth] = null;
								s[ircBot] = createBot(server.getId());
								((IrcBot) s[ircBot]).switchChannel(null, (String) s[tChannel]);
							} else if(messageA[1].equals("login")) {
								if(messageA.length != 3) {
									sendMessage(channel, "Usage: `" + commandPrefix + "twitch login <twitch username>`");
									break BLOCK;
								}
								s[twitchAcct] = messageA[2].toLowerCase();
								message.getAuthor().sendMessage("Reply to this message with the oauth key for " + messageA[2].toLowerCase() + "\n" + 
										"You can get an oauth key here: https://twitchapps.com/tmi/");
								awaitingOauth.put(message.getAuthor().getId(), server.getId());
							}
						}
						
					}
				});
				
				
				
			}

			public void onFailure(Throwable t) {
				t.printStackTrace();
			}
			
		});
	}
	
	public static IrcBot createBot(String serverID) {
		IrcBot bot = new IrcBot(Auth.TWITCH_USERNAME, serverID);
		bot.setVerbose(false);
		try {
			bot.connect("irc.twitch.tv", 6667, Auth.TWITCH_OAUTH);
		} catch (IOException e) {
			e.printStackTrace();
		} catch(IrcException e) {
			return null;
		}
		bot.setMessageDelay(2000l);
		return bot;
	}
	
	public static IrcBot createBot(String serverID, String username, String oauth) {
		IrcBot bot = new IrcBot(username, serverID);
		bot.setVerbose(false);
		try {
			bot.connect("irc.twitch.tv", 6667, oauth);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
		bot.setMessageDelay(2000l);
		return bot;
	}
	
	public static void addServer(Object... server) {
		Object[] record = new Object[7];
		record[0] = server[0];
		record[1] = server[1];
		record[2] = server[2];
		record[3] = server.length == 3 ? createBot((String) record[0]) : server[3];
		record[4] = null;
		record[5] = null;
		record[6] = null;
		servers.put((String) record[0], record);
	}
	
	public static void reloadServer(Object[] server, String role, String twitch, String oauth) {
		addServer(server);
		Object[] serverA = servers.get((String) server[0]);
		if(!role.equals("null"))
			serverA[reqRole] = role;
		if(server[tChannel] != null && !((String) server[tChannel]).equals("null")) {
			((IrcBot) serverA[ircBot]).switchChannel(null, (String) server[tChannel]);
		}
		if(!twitch.equals("null") && !oauth.equals("null")) {
			serverA[twitchAcct] = twitch;
			serverA[twitchOauth] = oauth;
		}
	}
	
	public static void sendMessage(Channel channel, String message) {
		if(!MessageQueue.messageQueue.containsKey(channel.getServer())) {
			MessageQueue.messageQueue.put(channel.getServer(), new ArrayList<QueuedMessage>());
		}
		MessageQueue.messageQueue.get(channel.getServer()).add(new QueuedMessage("\u200B" + message, channel));
	}
	
	public static String escape(String string) {
		string = string.replace("\\", "\\\\").replace("*", "\\*").replace("~", "\\~").replace("_", "\\_");
		return string;
	}
}

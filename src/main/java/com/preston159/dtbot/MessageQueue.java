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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.btobastian.javacord.entities.Server;

public class MessageQueue {
	
	/**
	 * The queue of messages to be sent out
	 */
	public static volatile HashMap<Server, ArrayList<QueuedMessage>> messageQueue = new HashMap<Server, ArrayList<QueuedMessage>>();
	/**
	 * The queue of messages which have been ratelimited and need to be sent out in the future
	 */
	public static volatile HashMap<Server, ArrayList<QueuedMessage>> failedQueue = new HashMap<Server, ArrayList<QueuedMessage>>();
	
	/**
	 * Starts the threads for dealing with queued messages
	 */
	public static void start() {
		Runnable messageQueueTask = () -> {
			//TODO: task in task for run every second
			while(true) {
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for(Server server : messageQueue.keySet()) {
					boolean success = true;
					if(!messageQueue.containsKey(server) || messageQueue.get(server).isEmpty())
						continue;
					QueuedMessage message = messageQueue.get(server).get(0);
					try {
						message.channel.sendMessage(message.message).get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch(ExecutionException e) {
						success = false;
					}
					if(success)
						messageQueue.get(server).remove(0);
					else {
						if(!failedQueue.containsKey(server)) {
							failedQueue.put(server, new ArrayList<QueuedMessage>());
						}
						for(QueuedMessage qm : messageQueue.get(server)) {
							failedQueue.get(server).add(qm);
						}
						messageQueue.put(server, new ArrayList<QueuedMessage>());
					}
				}
			}
		};
		Runnable failedMessageQueueTask = () -> {
			while(true) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for(Server server : failedQueue.keySet()) {
					boolean success = true;
					if(!failedQueue.containsKey(server) || failedQueue.get(server).isEmpty())
						continue;
					QueuedMessage message = failedQueue.get(server).get(0);
					try {
						message.channel.sendMessage(message.message).get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch(ExecutionException e) {
						success = false;
					}
					if(success) {
						failedQueue.get(server).remove(0);
						for(QueuedMessage qm : failedQueue.get(server)) {
							messageQueue.get(server).add(qm);
						}
						failedQueue.put(server, new ArrayList<QueuedMessage>());
					}
				}
			}
		};
		Thread messageQueueThread = new Thread(messageQueueTask);
		messageQueueThread.start();
		Thread failedMessageQueueThread = new Thread(failedMessageQueueTask);
		failedMessageQueueThread.start();
	}
	
}

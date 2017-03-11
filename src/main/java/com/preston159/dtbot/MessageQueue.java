package com.preston159.dtbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.btobastian.javacord.entities.Server;

public class MessageQueue {
	
	public static volatile HashMap<Server, ArrayList<QueuedMessage>> messageQueue = new HashMap<Server, ArrayList<QueuedMessage>>();
	public static volatile HashMap<Server, ArrayList<QueuedMessage>> failedQueue = new HashMap<Server, ArrayList<QueuedMessage>>();
	
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

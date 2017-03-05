package com.preston159.dtbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import de.btobastian.javacord.entities.Channel;

public class FileManager {
	
	static File file = new File("servers.properties");
	
	public static boolean saveAll() {
		Properties p = new Properties();
		for(String s : Main.servers.keySet()) {
			Object[] server = Main.servers.get(s);
			String put = (String) server[0] + ";" +
					(String) server[1] + ";" +
					((Channel) server[2]).getId() + ";" +
					(String) server[4];
			p.setProperty(s, put);
		}
		try {
			if(!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			p.store(fos, null);
		} catch(IOException e) {
			return false;
		}
		return true;
	}
	
	public static void loadAll() {
		System.out.println("Loading servers...");
		if(!file.exists()) {
			return;
		}
		Properties p = new Properties();
		try {
			FileInputStream fis = new FileInputStream(file);
			p.load(fis);
		} catch(IOException e) { }
		for(Object o : p.keySet()) {
			String s = p.getProperty((String) o);
			String[] server = s.split(";");
			if(server.length != 4) {
				continue;
			}
			Main.reloadServer(new Object[]{server[0], server[1], Main.api.getChannelById(server[2])}, server[3]);
		}
		System.out.println("Servers loaded");
	}
	
}

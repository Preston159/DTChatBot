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
		if(!file.exists()) {
			System.out.println("No servers to load");
			return;
		}
		System.out.println("Loading servers...");
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

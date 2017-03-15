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

public class Util {
	
	public static String time(long from, long to) {
		long diff = to - from;
		long days = 0, hours = 0, minutes = 0, seconds = 0;
		days = diff / 86400l;
		diff %= 86400l;
		hours = diff / 3600l;
		diff %= 3600l;
		minutes = diff / 60l;
		diff %= 60l;
		seconds = diff;
		return (days > 0 ? days + "d" : "") + (hours > 0 ? hours + "h" : "") + (minutes > 0 ? minutes + "m" : "") + seconds + "s";
	}
	
	public static String time(long from) {
		return time(from, System.currentTimeMillis() / 1000);
	}

}

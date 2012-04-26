/*
 * CS352 Example Chat Client
 * Copyright (C) 2011-2012 Rutgers University and Robert Moore
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package edu.rutgers.cs.chat.ui;

import edu.rutgers.cs.chat.Client;

/**
 * Interface for classes that want to respond to user interface events. Includes
 * broadcast (general) chat messages, private chat messages, and shutdown requests.
 * @author Robert Moore
 *
 */
public interface UserInputListener {

	/**
	 * Called when a broadcast (general) chat message has been entered by
	 * the user.
	 * @param message the chat message that was entered.
	 */
	public void broadcastChatMessage(String message);
	
	/**
	 * Called when a private chat message has been entered by the
	 * user.
	 * @param client the client to which the message should be sent.
	 * @param message the message to send.
	 */
	public void privateChatMessage(Client client, String message);
	
	/**
	 * Called when the user has requested that the local client shut down
	 * gracefully.  Remote clients should be notified of the shutdown.
	 */
	public void userRequestedShutdown();
	
}

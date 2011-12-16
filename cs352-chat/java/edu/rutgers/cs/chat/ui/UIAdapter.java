/*
 * CS352 Example Chat Client
 * Copyright (C) 2011 Rutgers University and Robert Moore
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
 * The primary interface between the ChatClient and UI components
 * of the application.  Provides methods that should be implemented
 * by any user interface class for the chat client.
 * 
 * @author Robert Moore
 *
 */
public interface UIAdapter {
	
	/**
	 * Called when a chat message is received from a remote client.
	 * @param fromClient the client that sent the message.
	 * @param timestamp the time the message was created.
	 * @param message the message that was sent.
	 */
	public void chatMessageReceived(Client fromClient, long timestamp, String message);
	
	/**
	 * Called when a chat message from the local instance 
	 * has been successfully sent to all connected clients.
	 * @param timestamp the timestamp on the sent message
	 * @param message the message that was sent.
	 */
	public void chatMessageSent(long timestamp, String message);
	
	/**
	 * Called when a chat message could not be sent to a remote
	 * client.
	 * @param client the client that failed
	 * @param message the message that could not be sent.
	 * @param reason optional message about the cause of the failure.
	 */
	public void messageNotSent(Client client, String message, String reason);
	
	/**
	 * Called when a new client has connected to the local instance.
	 * @param connectedClient the newly-connected client.
	 */
	public void clientConnected(Client connectedClient);
	
	/**
	 * Called when a remote client disconnects.  The message is an optional 
	 * message about the cause of the error.
	 * @param disconnectedClient the client that was disconnected.
	 * @param reason optional message about the cause of the disconnect.
	 */
	public void clientDisconnected(Client disconnectedClient, String reason);
	
}

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

package edu.rutgers.cs.chat.messaging;

import edu.rutgers.cs.chat.Client;


/**
 * Interface for classes that want to respond to messages from remote chat clients.
 * Keep-Alive messages are not passed to listeners.
 * @author Robert Moore
 *
 */
public interface MessageListener {

	/**
	 * Called when a broadcast chat message is received from a remote client.
	 * @param client the client that sent the chat message.
	 * @param message the message that was sent
	 */
	public void chatMessageArrived(final Client client, final ChatMessage message);
	
	/**
   * Called when a private chat message is received from a remote client.
   * @param client the client that sent the chat message.
   * @param message the message that was sent
   */
  public void privateChatMessageArrived(final Client client, final PrivateChatMessage message);
	
	/**
	 * Called when a client exchange message is received from a remote client.
	 * @param client the client that sent the exchange message.
	 * @param message the exchanged client information
	 */
	public void clientMessageArrived(final Client client, final ClientExchangeMessage message);
	
	/**
	 * Called when a client sends a disconnect message.  No response should be returned
	 * to the client, as the local client can assume that the socket is already closed.
	 * @param client the client that sent the disconnect message.
	 */
	public void disconnectMessageArrived(final Client client);
	
}

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

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message used to advertise client information between chat clients. 
 * 
 * @author Robert Moore
 *
 */
public class ClientExchangeMessage extends AbstractMessage {
	
	/**
	 * Logger for this class.
	 */
	private static final Logger log = Logger.getLogger(ClientExchangeMessage.class.getName());
	
	static {
		log.setLevel(Level.ALL);
	}
	
	/**
	 * The IP address of the client.
	 */
	protected final String ipAddress;
	
	/**
	 * The listen port of the client.
	 */
	protected final int port;
	
	/**
	 * The expected username of the client.
	 */
	protected final String username;
	
	/**
	 * Creates a new ClientMessage with the provided IP address, port, and username.
	 * @param ipAddress the hostname or IP address of the client.
	 * @param port the listen port of the client.
	 * @param username the username of the client.
	 * @throws UnsupportedEncodingException if the username cannot be encoded in UTF-16 big endian
	 */
	public ClientExchangeMessage(final String ipAddress, final int port, final String username) throws UnsupportedEncodingException
	{
		super(7+username.getBytes("UTF-16BE").length, AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE);
		this.ipAddress = ipAddress;
		this.port = port;
		this.username = username;
		log.finest("Created " + this);
	}
	
	/**
	 * Returns the IP address or hostname of the client.
	 * @return the IP address or hostname of the client.
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * Returns the listen port of the client.
	 * @return the listen port of the client.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the username of the client.
	 * @return the username of the client.
	 */
	public String getUsername() {
		return this.username;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append(super.toString()).append(' ').append(this.username).append('@').append(this.ipAddress).append(':').append(this.port);
		
		return sb.toString();
	}
}

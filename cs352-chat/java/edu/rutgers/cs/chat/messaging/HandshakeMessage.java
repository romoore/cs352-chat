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
 
/**
 * Representation of a Handshake message exchanged between chat clients.  The
 * handshake consists of a protocol string, listen port for incoming connections,
 * and the username of the sending client.
 * @author Robert Moore
 *
 */
public class HandshakeMessage extends AbstractMessage {

	/**
	 * The protocol string exchanged between chat clients. Currently "352 chat".
	 */
	public static final String PROTOCOL_STRING = "352 chat";
	
	/**
	 * The username for the sending chat client.
	 */
	protected final String username;
	
	/**
	 * The port on which the sending client is listening for incoming connections.
	 */
	protected final int listenPort;
	
	/**
	 * Returns the listen port for this handshake message.
	 * @return the listen port for this handshake message.
	 */
	public int getListenPort() {
		return this.listenPort;
	}

	/**
	 * Creates a new handshake message with the specified username and listen port.
	 * @param username the username of the sending client.
	 * @param listenPort the listen port of the sending client.
	 * @throws UnsupportedEncodingException if protocol string or username cannot be encoded in UTF-16 big endian.
	 */
	public HandshakeMessage(final String username, final int listenPort) throws UnsupportedEncodingException
	{
		super(3+HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE").length+username.getBytes("UTF-16BE").length, AbstractMessage.TYPE_HANDSHAKE_MESSAGE);
		this.username = username;
		this.listenPort = listenPort;
	}
	
	/**
	 * Returns the username contained in this handshake message.
	 * @return the username for this handshake message.
	 */
	public String getUsername() {
		return this.username;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append(super.toString()).append(',').append(PROTOCOL_STRING).append(',').append(this.username).append(',').append(this.listenPort);
		
		return sb.toString();
	}
	
}

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

package edu.rutgers.cs.chat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.rutgers.cs.chat.messaging.AbstractMessage;
import edu.rutgers.cs.chat.messaging.ChatMessage;
import edu.rutgers.cs.chat.messaging.ClientExchangeMessage;
import edu.rutgers.cs.chat.messaging.HandshakeMessage;
import edu.rutgers.cs.chat.messaging.MessageListener;
import edu.rutgers.cs.chat.messaging.PrivateChatMessage;


/**
 * Representation of a remote chat client connected to this client. Handles
 * reading messages from the remote client, which are passed to any registered
 * MessageListener interfaces.
 * 
 * @author Robert Moore
 * 
 */
public class Client extends Thread {

	/**
	 * The socket connected to the remote client.
	 */
	protected final Socket socket;

	/**
	 * The username of this client.
	 */
	protected String username;

	/**
	 * The IP address of this client.
	 */
	protected final String ipAddress;

	/**
	 * The listen port of this client. The value may not be known until after
	 * the handshake is received.
	 */
	protected int port = -1;

	/**
	 * The username of the local client. Used for generating handshake messages.
	 */
	protected final String localUsername;

	/**
	 * The listen port of the local client. Used for generating handshake
	 * messages.
	 */
	protected final int localPort;

	/**
	 * Collection of MessageListener interfaces that should be notified of
	 * received messages.
	 */
	protected final Collection<MessageListener> listeners = new ConcurrentLinkedQueue<MessageListener>();

	/**
	 * Flag to keep the main thread running.
	 */
	protected boolean keepRunning = true;

	/**
	 * Creates a new Client with the specified parameters. Does not connect to
	 * the remote client until {@link #connect()} is called.
	 * 
	 * @param ipAddress
	 *            the IP address or hostname of this client.
	 * @param port
	 *            the listen port of this client.
	 * @param username
	 *            the username of this client, may be null if it's not known.
	 * @param localUsername
	 *            the username of the local client.
	 * @param localPort
	 *            the listen port of the local client.
	 */
	public Client(final String ipAddress, final int port,
			final String username, final String localUsername,
			final int localPort) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.username = username;
		this.localUsername = localUsername;
		this.localPort = localPort;

		this.socket = new Socket();
	}

	/**
	 * Creates a new client from the provided socket and local username/listen
	 * port. The username for this client will not be set until the handshake
	 * message has been received.
	 * 
	 * @param socket
	 *            the socket over which this client is already connected.
	 * @param localUsername
	 *            the username of the local client.
	 * @param localPort
	 *            the listen port of the local client.
	 */
	public Client(final Socket socket, final String localUsername,
			final int localPort) {
		this.socket = socket;
		this.localUsername = localUsername;
		this.localPort = localPort;
		// Grab the actual address in case a hostname was provided
		this.ipAddress = this.socket.getInetAddress().getHostAddress();
	}

	/**
	 * Connects to this client if it is not already connected.
	 * 
	 * @throws IOException
	 *             if an IOException is thrown by this client's socket when
	 *             connecting.
	 * @see Socket#connect(java.net.SocketAddress)
	 */
	public void connect() throws IOException {
		if (this.socket != null && !this.socket.isConnected()) {
			this.socket
					.connect(new InetSocketAddress(this.ipAddress, this.port));
		}
	}

	/**
	 * Sends and receives handshake messages from this client. If this client's
	 * handshake is invalid (because the protocol string is incorrect) an error
	 * message is printed to System.err.
	 * 
	 * @return true if the handshake succeeded, else false.
	 * @see AbstractMessage#decodeMessage(java.io.InputStream)
	 */
	public synchronized boolean performHandshake() {
		HandshakeMessage sentMessage = null;
		try {
			// Try to create an outgoing handshake message
			sentMessage = new HandshakeMessage(this.localUsername,
					this.localPort);
		} catch (UnsupportedEncodingException uee) {
			System.err.println("Unable to encode handshake.");
			System.err.println(uee.getMessage());
			uee.printStackTrace(System.err);
			return false;
		}

		try {
			// Try to encode the handshake onto this client's output stream
			AbstractMessage.encodeMessage(sentMessage, this.socket
					.getOutputStream());
		} catch (IOException e) {
			System.err.println("Unable to send handshake message.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}

		AbstractMessage receivedMessage = null;
		// Keep reading messages until a HandshakeMessage is received
		// TODO: This is a bit of a hack
		do {
			try {
				receivedMessage = AbstractMessage.decodeMessage(this.socket
						.getInputStream());
				// Either no message available, or a decoding error
				if (receivedMessage == null) {
					// Allow other threads to issue before trying again
					Thread.yield();
					continue;
				}
			} catch (IOException e) {
				System.err
						.println("Unable to read handshake from remote client.");
				System.err.println(e.getMessage());
				e.printStackTrace();
				return false;
			}
			// Received the wrong type of message, handshake should always be
			// first
			if (!(receivedMessage instanceof HandshakeMessage)) {
				System.err.println("Received non-handshake message: "
						+ receivedMessage);
				return false;
			}
		}
		// Keep looping until we've received a handshake from the client
		while (receivedMessage == null);

		// Didn't know the username (probably created from a socket) so just
		// assign it.
		if (this.username == null) {
			this.username = ((HandshakeMessage) receivedMessage).getUsername();
		}
		// Verify that the username matches the expected value
		else {
			if (!this.username.equals(((HandshakeMessage) receivedMessage)
					.getUsername())) {
				System.err.println("Handshake username did not match "
						+ this.username + "<->"
						+ ((HandshakeMessage) receivedMessage).getUsername());
				return false;
			}
		}

		// Didn't know the listen port (probably created from a socket) so just
		// assign it.
		if (this.port < 0) {
			this.port = ((HandshakeMessage) receivedMessage).getListenPort();
		}
		// Verify that the listen port matches the expected value
		else if (this.port != ((HandshakeMessage) receivedMessage)
				.getListenPort()) {
			System.err.println("Handshake ports did not match " + this.port
					+ "<->"
					+ ((HandshakeMessage) receivedMessage).getListenPort());
			return false;
		}

		return true;
	}

	/**
	 * Causes this clinet to close its socket and kill any running threads it
	 * may have started.
	 */
	public void disconnect() {
		this.keepRunning = false;
		// Close the socket if it isn't already closed
		if (this.socket != null && !this.socket.isClosed()) {
			try {
				this.socket.close();
			} catch (IOException e) {
				// Ignored, since we're closing anyways
			}
		} else {
			// This shouldn't happen, but print an error message just in case
			System.err.println("Already disconnected?");
		}
	}

	/**
	 * Sends the chat message to this client.
	 * 
	 * @param message
	 *            the message to send.
	 * @throws IOException
	 *             if an IOException is thrown when writing the message.
	 */
	public synchronized void sendMessage(final String message)
			throws IOException {
		ChatMessage cMessage = new ChatMessage(System.currentTimeMillis(),
				this.localUsername, message);
		AbstractMessage.encodeMessage(cMessage, this.socket.getOutputStream());
	}
	
	/**
   * Sends the private chat message to this client.
   * 
   * @param message
   *            the message to send.
   * @throws IOException
   *             if an IOException is thrown when writing the message.
   */
  public synchronized void sendPrivateMessage(final String message)
      throws IOException {
    PrivateChatMessage cMessage = new PrivateChatMessage(System.currentTimeMillis(),
        this.localUsername, message);
    AbstractMessage.encodeMessage(cMessage, this.socket.getOutputStream());
  }

	/**
	 * Sends a client exchange message to this client.
	 * 
	 * @param otherClient
	 *            the client information to send.
	 * @throws IOException
	 *             if an IOException is thrown when writing the message.
	 */
	public synchronized void sendClient(final Client otherClient)
			throws IOException {
		ClientExchangeMessage cMessage = new ClientExchangeMessage(otherClient
				.getIpAddress(), otherClient.getPort(), otherClient
				.getUsername());
		AbstractMessage.encodeMessage(cMessage, this.socket.getOutputStream());
	}

	/**
	 * Sends a disconnect message to this client.
	 * 
	 * @throws IOException
	 *             if an IOException is thrown when writing the message.
	 */
	public synchronized void sendDisconnectMessage() throws IOException {
		AbstractMessage.encodeMessage(AbstractMessage.DISCONNECT_MESSAGE,
				this.socket.getOutputStream());
	}

	/**
	 * Sends a keep-alive message to this client.
	 * 
	 * @throws IOException
	 *             if an IOException is thrown when writing the message.
	 */
	public synchronized void sendKeepAliveMessage() throws IOException {
		AbstractMessage.encodeMessage(AbstractMessage.KEEPALIVE_MESSAGE,
				this.socket.getOutputStream());
	}

	/**
	 * Registers the specified MessageListener interface with this client.
	 * 
	 * @param listener
	 *            the MessageListener to register
	 */
	public void addMessageListener(final MessageListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Unregisters the specified MessageListener interface with this client.
	 * 
	 * @param listener
	 *            the MessageListener to unregister.
	 */
	public void removeMessageListener(final MessageListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Reads messages from this client's socket. If no message is decoded, waits
	 * 5ms and tries again.
	 */
	@Override
	public void run() {
		// Keep running until disconnect() is called
		while (this.keepRunning) {
			try {
				final AbstractMessage message = AbstractMessage
						.decodeMessage(this.socket.getInputStream());

				if (message == null) {
					// Nothing received, try again?
					try {
						Thread.sleep(5);
					} catch (InterruptedException ie) {
						// Ignore interrupts
					}
					continue;
				}
				if (message.getType() == AbstractMessage.TYPE_CHAT_MESSAGE) {
					for (MessageListener listener : Client.this.listeners) {
						listener.chatMessageArrived(Client.this,
								(ChatMessage) message);
					}
				} else if (message.getType() == AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE) {
					for (MessageListener listener : Client.this.listeners) {
						listener.clientMessageArrived(Client.this,
								(ClientExchangeMessage) message);
					}
				} else if (message.getType() == AbstractMessage.TYPE_DISCONNECT_MESSAGE) {
					for (MessageListener listener : Client.this.listeners) {
						listener.disconnectMessageArrived(Client.this);
					}

				}else if(message.getType() == AbstractMessage.TYPE_PRIVATE_CHAT_MESSAGE){
				  for (MessageListener listener : Client.this.listeners) {
            listener.privateChatMessageArrived(Client.this,(PrivateChatMessage)message);
          }
				}

			} catch (Exception e) {
				this.keepRunning = false;
				System.err.println(this
						+ ": Caught exception while reading from client.");
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
				// Announce the disconnect to the listeners

				for (MessageListener listener : Client.this.listeners) {
					listener.disconnectMessageArrived(this);
				}

			}
		}
	}

	/**
	 * Returns the username of this client.
	 * @return the username of this client.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Returns this client's socket.
	 * @return this client's socket.
	 */
	public Socket getSocket() {
		return this.socket;
	}

	/**
	 * Returns the IP address of this client.
	 * @return the IP address of this client.
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * Returns the listen port of this client.
	 * @return the listen port of this client.
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Client)
			return this.equals((Client) o);
		return super.equals(o);
	}

	/**
	 * Compares this client to another based on the IP addresses and listen port numbers.
	 * @param client the client to check for equality.
	 * @return true if both client's IP address and listen port match, else false.
	 */
	public boolean equals(Client client) {
		// if (this.ipAddress != null && this.ipAddress.equals(client.ipAddress)
		// Generate textual IP addresses for both
		try {
			InetAddress myAddress = InetAddress.getByName(this.ipAddress);
			InetAddress otherAddress = InetAddress.getByName(client.ipAddress);
			if (myAddress.equals(otherAddress) && this.port == client.port) {
				return true;
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}

	@Override
	public int hashCode() {
		// TODO: This is just a hack to make the compiler stop warning
		return this.socket.hashCode() ^ this.port;
	}

	@Override
	public String toString() {
		return (this.username == null ? "Unknown client" : this.username) + "@"
				+ this.ipAddress + ":" + this.port;
	}
}

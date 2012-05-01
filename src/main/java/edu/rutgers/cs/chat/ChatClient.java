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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.rutgers.cs.chat.messaging.ChatMessage;
import edu.rutgers.cs.chat.messaging.ClientExchangeMessage;
import edu.rutgers.cs.chat.messaging.MessageListener;
import edu.rutgers.cs.chat.messaging.PrivateChatMessage;
import edu.rutgers.cs.chat.ui.ConsoleUI;
import edu.rutgers.cs.chat.ui.GraphicalUI;
import edu.rutgers.cs.chat.ui.UIAdapter;
import edu.rutgers.cs.chat.ui.UIAdapter.UIType;
import edu.rutgers.cs.chat.ui.UserInputListener;

/**
 * Main class of the chat client. It handles incoming connections and exchanged
 * client information.
 * 
 * @author Robert Moore
 * 
 */
public class ChatClient extends Thread implements MessageListener,
		UserInputListener {

	/**
	 * Logger for this class.
	 */
	private static final Logger log = Logger.getLogger(ChatClient.class
			.getName());

	static {
		log.setLevel(Level.ALL);
		Logger root = Logger.getLogger("");
		ChatClient.replaceConsoleHandler(root, Level.WARNING);
		try {
			Handler fileHandler = new FileHandler("cs352-chat.log");
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new SimpleFormatter());
			root.addHandler(fileHandler);
		} catch (Exception e) {
			System.err.println("Unable to create log file: " + e.getMessage());
		}
	}

	/**
	 * Copyright license notification message printed at startup.
	 */
	public static final String LICENSE_NOTIFICATION = "CS352 Example Chat Client version 1.0.9\n"
			+ "Copyright (C) 2011-2012 Robert Moore and Rutgers University\n"
			+ "CS352 Example Chat Client comes with ABSOLUTELY NO WARRANTY.\n"
			+ "This is free software, and you are welcome to redistribute it\n"
			+ "under certain conditions; see the included file LICENSE for details.";

	/**
	 * Port number for incoming connections.
	 */
	protected final int listenPort;

	/**
	 * Local username. This is sent along with chat messages.
	 */
	protected final String username;

	/**
	 * Socket for accepting incoming connections.
	 */
	protected ServerSocket listenSocket;

	/**
	 * Flag to shutdown the client.
	 */
	protected boolean keepRunning = true;

	/**
	 * List of currently-connected clients.
	 */
	protected final Collection<Client> clients = new ConcurrentLinkedQueue<Client>();

	/**
	 * Thread pool for handling incoming connections and new client information.
	 */
	protected final ExecutorService workers = Executors.newCachedThreadPool();

	/**
	 * Basic console-based user interface.
	 */
	protected UIAdapter userInterface;

	/**
	 * Parse command-line arguments and start a single instance of the
	 * ChatClient class.
	 * 
	 * @param args
	 *            local port, username, remote host (optional), remote port
	 *            (optional)
	 */
	public static void main(String[] args) {

		// Empty line for formatting
		System.out.println();
		// Print license info
		System.out.println(ChatClient.LICENSE_NOTIFICATION);
		// Empty line for formatting
		System.out.println();

		log.info(ChatClient.LICENSE_NOTIFICATION);

		// Check to make sure we have at least 2 arguments (local port,
		// username)
		if (args == null || args.length < 2) {
			printUsage();
			return;
		}

		// Set the listen port for incoming client connections
		int listenPort = Integer.MIN_VALUE;
		try {
			listenPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException nfe) {
			log.severe("Invalid or missing port number.  Client cannot start.");
			return;
		}

		// Don't even mess with ports outside userspace.
		if (listenPort <= 1024 || listenPort > 65535) {
			log.severe("Port number is outside the valid range: 1024 < port < 65535");
			return;
		}

		// Any string is fine, will be encoded in UTF-16 to clients
		String username = args[1];

		ArrayList<Client> optionalClients = new ArrayList<Client>();

		/*
		 * If the remote host and port were provided, try to connect. Shouldn't
		 * be fatal if it fails.
		 */
		UIType ui = UIType.CONSOLE;
		if (args.length >= 3) {

			for (int i = 2; i < args.length;) {
				if ("--gui".equalsIgnoreCase(args[i])) {
					ui = UIType.GRAPHICS;
					++i;
					continue;
				}
				String host = args[i++];
				// Parse the port, pass args[2] in as the remote hostname.
				int remotePort = Integer.MIN_VALUE;
				try {
					remotePort = Integer.parseInt(args[i++]);

					// Userspace ports onlys
					if (remotePort <= 1024 || remotePort > 65535) {
						log.severe("Remote port number is out of valid range, won't connect.");
					} else {
						// Don't know the remote username, so pass null
						Client newClient = new Client(host, remotePort, null,
								username, listenPort);
						optionalClients.add(newClient);
					}
				} catch (NumberFormatException nfe) {
					log.severe("Invalid port number specified for bootstrap client, won't connect.");
				}
			}
		}

		// Create the application with the listen port and username.
		ChatClient ourClient = new ChatClient(listenPort, username, ui);
		// Start the client, ensure that incoming connections will be handled if
		// we
		// add a bootstrap peer.
		ourClient.start();
		
		for(Client c : optionalClients){
			ourClient.addClient(c.getIpAddress(), c.getPort(), c.getUsername());
		}
	}

	/**
	 * Creates a new chat client listening on the specified port and with the
	 * provided username.
	 * 
	 * @param listenPort
	 *            the port number for incoming client connections.
	 * @param username
	 *            the username to send to other clients.
	 */
	public ChatClient(final int listenPort, final String username,
			final UIType uiType) {
		this.listenPort = listenPort;
		this.username = username;
		log.finer("Created new chat client on port " + this.listenPort
				+ " for user " + this.username);
		if (uiType == uiType.CONSOLE) {
			this.userInterface = new ConsoleUI();
			log.finer("Created console UI.");
		} else if (uiType == uiType.GRAPHICS) {
			this.userInterface = new GraphicalUI(this.username);
			log.finer("Created graphical UI.");
		}else{
			log.severe("Unknown UI type requested: " + uiType);
			throw new IllegalArgumentException("Unknown UI type " + uiType);
		}

		this.userInterface.addUserInputListener(this);
		log.finer("Registering for UI events from " + this.userInterface);
		if (this.userInterface instanceof ConsoleUI) {
			((ConsoleUI) this.userInterface).start();
			log.finer("Started console UI thread.");
		}
	}

	/**
	 * Called when remote clients exchange information about other clients with
	 * this client. Will check for a duplicate connection first, test it with a
	 * keep-alive, and ignore this client if the connection is viable. If the
	 * client is not matched or the old connection is closed, then it will
	 * connect to the client.
	 * 
	 * @param remoteHost
	 *            the hostname/IP address of the new client
	 * @param port
	 *            the listen port number for the new client.
	 * @param username
	 *            the username expected from the remote client.
	 */
	protected synchronized void addClient(final String remoteHost,
			final int port, final String username) {
		// Build a new client object
		Client newClient = this.makeClient(remoteHost, port, username);

		// If null, then an exception was thrown, probably couldn't resolve the
		// hostname.
		if (newClient == null) {
			return;
		}

		// Check to see if this client is already known
		Client oldClient = this.findDuplicate(newClient);

		// If we already have this client in our list, then check to make sure
		// it's still live
		if (oldClient != null) {
			if (this.testClient(oldClient)) {
				// Old client is fine, so discard the new one
				return;
			}
		}
		// Connect the socket to the remote client, discard the client on errors
		try {
			newClient.connect();
		} catch (IOException ioe) {
			log.severe("Unable to connect to " + newClient + ": "
					+ ioe.getMessage());
			return;
		}
		// Try to handshake, if it succeeds then notify the UI
		if (newClient.performHandshake()) {
			this.registerClient(newClient);
			this.clients.add(newClient);
		}

	}

	/**
	 * Registers a new client with the local client. Notifies the UI.
	 * 
	 * @param client
	 *            the client to register.
	 */
	protected void registerClient(Client client) {
		log.fine("Registering " + client);
		client.addMessageListener(this);
		client.start();
		this.userInterface.clientConnected(client);
		log.finer("Notified user interface" + this.userInterface);
	}

	/**
	 * Tests a client for liveness by sending a Keep-Alive message. If the send
	 * fails, then the client is disconnected, removed from the list of clients,
	 * and the UI is notified.
	 * 
	 * @param client
	 *            the client to test
	 * @return true if the test succeeds, else false
	 */
	protected boolean testClient(Client client) {
		try {
			client.sendKeepAliveMessage();
			return true;
		} catch (IOException ioe) {
			this.clients.remove(client);
			client.removeMessageListener(this);
			client.disconnect();
			this.userInterface.clientDisconnected(ioe.getMessage(), client);
		}
		return false;
	}

	/**
	 * Creates a new client from the supplied arguments. If an IOException is
	 * thrown, then null is returned.
	 * 
	 * @param remoteHost
	 *            the hostname/IP address of the client.
	 * @param port
	 *            the listen port of the client.
	 * @param username
	 *            the username expected from the client
	 * @return the newly-created Client, or null if an exception was thrown.
	 */
	protected Client makeClient(String remoteHost, int port, String username) {
		Client newClient = new Client(remoteHost, port, username,
				this.username, this.listenPort);
		return newClient;
	}

	/**
	 * Called when a remote client connects to the local client on the listen
	 * socket. Will check for duplicates (just in case the remote peer made a
	 * mistake), test it with a keep-alive, and close the new connection if the
	 * old one is still viable. If the new client is not matched or the old
	 * connection is closed, then it will register the new client.
	 * 
	 * @param socket
	 *            the socket of the newly-connected client
	 */
	protected synchronized void addClient(final Socket socket) {
		Client newClient = new Client(socket, this.username, this.listenPort);

		// Need to handshake first since we need to get the remote port info
		// before checking for duplicates
		if (!newClient.performHandshake()) {
			newClient.disconnect();
			return;
		}

		// Try to find an old version of this client (same IP/port)
		Client oldClient = findDuplicate(newClient);

		// If we already have this client in our list, then check to make sure
		// it's still live
		if (oldClient != null) {
			if (this.testClient(oldClient)) {
				// Old client is fine, so discard the new one
				return;
			}
		}

		// Register the new client
		this.registerClient(newClient);

		// Notify other clients of the new client
		this.notifyClients(newClient);

		// Add the client to the list of known clients
		this.clients.add(newClient);
	}

	/**
	 * Sends client exchange messages to currently-connected clients.
	 * 
	 * @param newClient
	 *            the newly-added client.
	 */
	protected void notifyClients(Client newClient) {
		// Go through each remote client and send a ClientMessage.
		for (Iterator<Client> clientIter = this.clients.iterator(); clientIter
				.hasNext();) {
			Client client = clientIter.next();
			try {
				// Exchange the client information
				client.sendClient(newClient);
			} catch (IOException e) {
				// Remove the client from the list of clients
				clientIter.remove();
				// Stop listening to messages from the client
				client.removeMessageListener(this);
				// Disconnect the client
				client.disconnect();
				// Notify the UI of the disconnect
				this.userInterface.clientDisconnected(e.getMessage(), client);
			}
		}
	}

	/**
	 * Checks to see if client is a duplicate of an already-connected client. If
	 * it is, returns the client that matches, else returns null.
	 * 
	 * @param client
	 *            the client to search for.
	 * @return the currently-connected duplicate client, or null if no match is
	 *         found.
	 */
	protected Client findDuplicate(Client client) {
		for (Client otherClient : this.clients) {
			if (client.equals(otherClient)) {
				return otherClient;
			}

		}
		return null;
	}

	/**
	 * Prints out the basic usage string to System error.
	 */
	protected static final void printUsage() {
		StringBuffer usageString = new StringBuffer();
		usageString
				.append("Usage: <Listen Port> <Username> [<Remote IP> <Remote Port>]");
		System.err.println(usageString.toString());
	}

	/**
	 * Passes the received chat message to the user interface.
	 */
	@Override
	public void chatMessageArrived(final Client client,
			final ChatMessage message) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {

				ChatClient.this.userInterface.broadcastMessageReceived(
						message.getTimestamp(), message.getMessage(), client);
			}
		});

	}

	/**
	 * Passes the received private chat message to the user interface.
	 */
	@Override
	public void privateChatMessageArrived(final Client client,
			final PrivateChatMessage message) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {

				ChatClient.this.userInterface.privateMessageReceived(
						message.getTimestamp(), message.getMessage(), client);
			}
		});
	}

	/**
	 * Adds the exchange chat client if it is not already connected to the local
	 * client.
	 */
	@Override
	public void clientMessageArrived(final Client client,
			final ClientExchangeMessage message) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				ChatClient.this.addClient(message.getIpAddress(),
						message.getPort(), message.getUsername());
			}
		});

	}

	/**
	 * Deregisters the client from the local client, disconnects it, and
	 * notifies the user interface.
	 */
	@Override
	public void disconnectMessageArrived(final Client client) {
		try {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				client.removeMessageListener(ChatClient.this);
				client.disconnect();
				ChatClient.this.clients.remove(client);
				ChatClient.this.userInterface.clientDisconnected("User quit.",
						client);
			}
		});
		}catch(Exception e){
			log.fine("Couldn't handle disconnect: " + e.getMessage());
		}
	}

	/**
	 * Listens for incoming connections, checking every 250ms for a user request
	 * to exit the chat client. Incoming connections are handled by worker
	 * threads.
	 */
	@Override
	public void run() {
		try {
			// Bind to the local listen port
			this.listenSocket = new ServerSocket(this.listenPort);
			this.listenSocket.setReuseAddress(true);
			// Wait for 250ms at a time
			this.listenSocket.setSoTimeout(250);
		} catch (IOException e) {
			// An exception here is likely to be a bind failure.
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		log.config("Listening on port " + this.listenPort);

		while (this.keepRunning) {
			try {
				// This will block for 250ms to allow checking for user exit
				// conditions.
				final Socket clientSocket = this.listenSocket.accept();
				/*
				 * Pass the actual work of adding the client to another thread,
				 * freeing this thread to accept new clients.
				 */
				this.workers.execute(new Runnable() {

					@Override
					public void run() {
						addClient(clientSocket);
					}
				});
			} catch (SocketTimeoutException ste) {
				// Ignored
			} catch (IOException e) {
				// Left in for debugging
				e.printStackTrace();
			}
		}
		this.doShutdown();
	}

	/**
	 * Disconnects all currently-connected clients, shuts down thread pools, and
	 * exits the application.
	 */
	protected void doShutdown() {
		// Close down connections to all clients.
		for (Client client : this.clients) {
			try {
				client.sendDisconnectMessage();
			} catch (IOException ioe) {
				// Ignored for now, who cares if we're shutting down. :)
			}
			client.disconnect();
		}
		// Shut down the threadpool
		this.workers.shutdown();
	}

	/**
	 * Sends the specified message to all currently-connected clients. If any
	 * exception is thrown while sending the message, then that client is
	 * disconnected. Actual work is handled by a worker thread. Will notify the
	 * user interface after all clients have been sent the message (or failed to
	 * send).
	 * 
	 * @see UIAdapter#broadcastMessageSent(long, String)
	 */
	@Override
	public void broadcastChatMessage(final String input) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				for (Iterator<Client> clientIter = ChatClient.this.clients
						.iterator(); clientIter.hasNext();) {
					Client client = clientIter.next();
					try {
						client.sendMessage(input);
					} catch (IOException e) {
						// Remove the client from the list of clients
						clientIter.remove();
						// Disconnect the client
						client.disconnect();
						// Notify the UI of the disconnect
						ChatClient.this.userInterface.clientDisconnected(
								"Failed to send broadcast chat message/"
										+ e.getMessage(), client);
					}
				}
				ChatClient.this.userInterface.broadcastMessageSent(
						System.currentTimeMillis(), input);

			}
		});
	}

	/**
	 * Sends the message to the specified client. If the message cannot be sent
	 * due to an exception, then the client is disconnected.
	 */
	@Override
	public void privateChatMessage(final Client client, final String message) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					client.sendPrivateMessage(message);
				} catch (IOException e) {
					ChatClient.this.clients.remove(client);
					client.removeMessageListener(ChatClient.this);
					client.disconnect();
					ChatClient.this.userInterface.clientDisconnected(
							"Failed to send private chat message/"
									+ e.getMessage(), client);
				}
				ChatClient.this.userInterface.privateMessageSent(
						System.currentTimeMillis(), message, client);
			}

		});
	}

	/**
	 * Sets the run flag to false, which should occur within 250ms.
	 * 
	 * @see ChatClient#run()
	 */
	@Override
	public void userRequestedShutdown() {
		this.keepRunning = false;
	}

	/**
	 * Replaces the ConsoleHandler for a specific Logger with one that will log
	 * all messages. This method could be adapted to replace other types of
	 * loggers if desired.
	 * 
	 * @param logger
	 *            the logger to update.
	 * @param newLevel
	 *            the new level to log.
	 */
	public static void replaceConsoleHandler(Logger logger, Level newLevel) {

		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : logger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}
		}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			logger.addHandler(consoleHandler);
		}
		// set the console handler to fine:
		consoleHandler.setLevel(newLevel);
	}
}

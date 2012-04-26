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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.rutgers.cs.chat.Client;

/**
 * A simple user interface for the chat client. General chat messages are sent
 * by typing and pressing &lt;Enter&gt;. Private chat messages are specified by
 * providing the username preceded by the at sign (@) followed by a space and
 * the message. The user can terminate the chat client by typing entering
 * "quit".
 * 
 * @author Robert Moore
 * 
 */
public class ConsoleUI extends Thread implements UIAdapter {

  /**
   * Collection of listeners for user input/quit requests.
   */
  protected final Collection<UserInputListener> listeners = new ConcurrentLinkedQueue<UserInputListener>();

  /**
   * List of currently-connected clients. Used to validate private message
   * destinations before passing to any UserInterfaceListener interfaces.
   */
  protected final Collection<Client> knownClients = new ConcurrentLinkedQueue<Client>();

  /**
   * Flag to keep awaiting user input.
   */
  protected boolean keepRunning = true;

  /**
   * The reader for user input.
   */
  private BufferedReader reader;

  /**
   * Adds a UserInterfaceListener interface to this object's list of listeners.
   * 
   * @param listener
   *          the UserInterfaceListener to add.
   */
  @Override
  public void addUserInputListener(UserInputListener listener) {
    this.listeners.add(listener);
  }

  /**
   * Removes a UserInterfaceListener interface from this object's list of
   * listeners.
   * 
   * @param listener
   *          the UserInterfaceListener to remove.
   */
  public void removeUserInputListener(UserInputListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * Prints out the chat message to the system out as "username: message"
   */
  @Override
  public void broadcastMessageReceived(long timestamp, String message,
      Client fromClient) {
    System.out.println(fromClient.getUsername() + ": " + message);
  }

  /**
   * Causes this ConsoleUI to terminate gracefully.
   */
  public void terminate() {
    this.keepRunning = false;
    synchronized (this) {
      this.interrupt();
    }
  }

  /**
   * Continuously awaits user input and passes the chat messages or quit request
   * to any registered UserInputListener interfaces.
   */
  @Override
  public void run() {

    // Convenience wrapper around System.in
    this.reader = new BufferedReader(new InputStreamReader(System.in));

    // Helpful little instructions.
    System.out
        .println("Send broadcast messages by typing and hitting <Enter>.");
    System.out
        .println("Send private messages like this: @username message to send.");
    System.out.println("Type \"quit\" to exit.");

    // Keep awiting user input until the user wants to quit.
    while (this.keepRunning) {
      try {
        if (!this.reader.ready()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException ie) {
            // Ignored
          }
          continue;
        }
        String line = this.reader.readLine();
        if ("quit".equalsIgnoreCase(line)) {
          for (UserInputListener listener : this.listeners) {
            listener.userRequestedShutdown();
          }
          break;
        }

        if (line == null) {
          continue;
        }
        // Check for private message
        if (line.startsWith("@")) {
          int usernameBreak = line.indexOf(' ');
          String username = line.substring(1, usernameBreak);
          String message = line.substring(usernameBreak + 1, line.length());
          Client theClient = null;
          // Find the client based on the username
          for (Client client : this.knownClients) {
            if (client.getUsername().equals(username)) {
              theClient = client;
              break;
            }
          }
          // If the client wasn't found, print an error message and
          // await next input
          if (theClient == null) {
            System.out.println("Could not find user \"" + username + "\".");
            continue;
          }

          // Notify the listeners that a private message was entered
          for (UserInputListener listener : this.listeners) {
            listener.privateChatMessage(theClient, message);
          }
        }
        // Broadcast (general) chat message
        else {
          // Notify the listeners that a broadcast chat message was
          // entered
          for (UserInputListener listener : this.listeners) {
            listener.broadcastChatMessage(line);
          }
        }
      } catch (IOException e) {
        // Left in for debugging
        e.printStackTrace();
        continue;
      }

    }

    this.listeners.clear();
  }

  /**
   * Does nothing, as the console already echoes the user's input.
   */
  @Override
  public void broadcastMessageSent(long timestamp, String message) {
    // Not going to repeat the message since the console already provides
    // display
  }

  /**
   * Adds the client to the collection of known clients.
   */
  @Override
  public void clientConnected(Client connectedClient) {
    System.out.println("Connected to " + connectedClient);
    this.knownClients.add(connectedClient);
  }

  /**
   * Removes the client from the collection of known clients. Prints a
   */
  @Override
  public void clientDisconnected(String reason, Client disconnectedClient) {
    this.knownClients.remove(disconnectedClient);
    System.out.println(disconnectedClient + " disconnected"
        + (reason == null ? "." : (" because: " + reason)));
  }

  /**
   * Prints an error message to the console to notify the user that the message
   * could not be sent to some client.
   */
  @Override
  public void messageNotSent(String message, String reason, Client client) {
    System.out.println("The following message could not be sent to " + client
        + ":\n" + message + (reason == null ? "\n" : "\n\nReason: " + reason));
  }

  @Override
  public void privateMessageSent(long timestamp, String message, Client client) {
    // Ignored
  }

  @Override
  public void privateMessageReceived(long timestamp, String message,
      Client client) {
    System.out.println("(" + client.getUsername() + "): " + message);
  }

}

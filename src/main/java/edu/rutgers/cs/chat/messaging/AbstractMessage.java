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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Base class for all messages exchanged between chat clients. Keeps the encoded
 * message length and message type for all subclasses.
 * 
 * This class provides static methods to encode messages to an OutputStream and
 * decode them from an InputStream.
 * 
 * The chat protocol behaves as follows:
 * 
 * <ul>
 * <li>After a connection is established, both sides transmit a Handshake
 * message with their respective listen ports and usernames.</li>
 * <li>If the handshakes are validated correctly, Chat, ClientExchange,
 * Keep-Alive, and Disconnect messages may be exchanged at any time.</li>
 * <li>Chat messages are sent any time the user provides a message to be
 * broadcast to all clients.</li>
 * <li>ClientExchange messages are sent to currently-connected remote clients
 * after a new client has connected to the listen socket of the local client.
 * Clients receiving a ClientExchange message may decide whether or not to
 * connect to the new client.</li>
 * <li>Disconnect messages should be sent when a client decides to exit the
 * chat. While the clients handle unexpected socket closure gracefully, it is
 * best practice to send a Disconnect message before closing a connection.
 * Clients receiving a Disconnect message should assume that the remote side of
 * the socket is closed.</li>
 * <li>Keep-Alive messages are usually sent to validate a connection to a remote
 * client if a ClientExchange message has been received with the same
 * information, or if a client connects from the same IP and has the same listen
 * port value.</li>
 * <li>Private Chat messages are only sent to a selected client.</li>
 * </ul>
 * 
 * The messages have a standard header of a 4-byte unsigned integer length field
 * and a 1-byte message type value. The message type values are listed below:
 * 
 * <ul>
 * <li>Chat messages have a message type value of 0.</li>
 * <li>ClientExchange messages have a message type value of 1.</li>
 * <li>Disconnect messages have a message type value of 2.</li>
 * <li>Handshake messages have a message type value of 3.</li>
 * <li>Keep-Alive messages have a message type value of 4.</li>
 * <li>Private Chat messages have a message type value of 5.</li>
 * </ul>
 * 
 * @author Robert Moore
 * 
 */
public abstract class AbstractMessage {

  /**
   * Message type for chat messages.
   */
  public static final byte TYPE_CHAT_MESSAGE = 0;

  /**
   * Message type for client exchange messages.
   */
  public static final byte TYPE_CLIENT_EXCHANGE_MESSAGE = 1;

  /**
   * Message type for client disconnect messages.
   */
  public static final byte TYPE_DISCONNECT_MESSAGE = 2;

  /**
   * Message type for handshake messages.
   */
  public static final byte TYPE_HANDSHAKE_MESSAGE = 3;

  /**
   * Message type for keep-alive messages.
   */
  public static final byte TYPE_KEEPALIVE_MESSAGE = 4;

  /**
   * Message type for private chat messages.
   */
  public static final byte TYPE_PRIVATE_CHAT_MESSAGE = 5;

  /**
   * Convenience array for printing message types.
   */
  public static final String[] MESSAGE_NAMES = { "Chat", "Client Exchange",
      "Disconnect", "Handshake", "Keep-Alive" };

  /**
   * Static reference to a disconnect message so that new objects don't need to
   * be allocated.
   */
  public static final DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage();

  /**
   * Static reference to a keep-alive message so that new objects don't need to
   * be allocated.
   */
  public static final KeepAliveMessage KEEPALIVE_MESSAGE = new KeepAliveMessage();

  /**
   * The length of the encoded message.
   */
  protected final int length;

  /**
   * The type of this message.
   */
  protected final byte type;

  /**
   * Creates a new abstract message with the specified length and message type.
   * 
   * @param length
   *          the length (in bytes) of the encoded message.
   * @param type
   *          the type of message.
   */
  protected AbstractMessage(final int length, final byte type) {
    this.length = length;
    this.type = type;
  }

  /**
   * Returns the encoded length of this message in bytes.
   * 
   * @return the encoded length of this message in bytes.
   */
  public int getLength() {
    return this.length;
  }

  /**
   * Returns the type of this message.
   * 
   * @return the type of this message.
   */
  public byte getType() {
    return this.type;
  }

  @Override
  public String toString() {
    return '(' + this.length + ") " + MESSAGE_NAMES[this.type];
  }

  /**
   * Represents a disconnect message sent between clients. Clients receiving
   * this message should assume that the remote side has closed the socket and
   * should not attempt to respond.
   * 
   * @author Robert Moore
   * 
   */
  public static final class DisconnectMessage extends AbstractMessage {
    /**
     * Creates a new disconnect message. Should only be invoked by the
     * AbstractMessage class.
     */
    protected DisconnectMessage() {
      super(1, TYPE_DISCONNECT_MESSAGE);
    }
  }

  /**
   * Represents a keep-alive message exchanged between chat clients.
   * 
   * @author Robert Moore
   * 
   */
  public static final class KeepAliveMessage extends AbstractMessage {
    /**
     * Creates a new keep-alive message. Should only be invoked by the
     * AbstractMessage class.
     */
    protected KeepAliveMessage() {
      super(1, TYPE_KEEPALIVE_MESSAGE);
    }
  }

  /**
   * Encodes the specified message onto the provided OutputStream.
   * 
   * @param message
   *          the message to encode
   * @param out
   *          the OutputStream on which to write the message.
   * @throws IOException
   *           if an IOException is thrown by the OutputStream.
   */
  public static void encodeMessage(final AbstractMessage message,
      final OutputStream out) throws IOException {

    // Wrap a DataOutputStream for convenience.
    DataOutputStream dout = new DataOutputStream(out);

    // Always write the message length and type
    dout.writeInt(message.getLength());
    dout.writeByte(message.getType());

    // Check to see if there's more to encode.
    if (message.getLength() > 1) {

      switch (message.getType()) {
      // Chat messages
      case AbstractMessage.TYPE_CHAT_MESSAGE: {
        ChatMessage chat = (ChatMessage) message;
        dout.writeLong(chat.getTimestamp());
        dout.writeInt(chat.getUsername().getBytes("UTF-16BE").length);
        dout.write(chat.getUsername().getBytes("UTF-16BE"));
        dout.write(chat.getMessage().getBytes("UTF-16BE"));
        break;
      }
      case AbstractMessage.TYPE_PRIVATE_CHAT_MESSAGE: {
        PrivateChatMessage chat = (PrivateChatMessage) message;
        dout.writeLong(chat.getTimestamp());
        dout.writeInt(chat.getUsername().getBytes("UTF-16BE").length);
        dout.write(chat.getUsername().getBytes("UTF-16BE"));
        dout.write(chat.getMessage().getBytes("UTF-16BE"));
        break;
      }
      // Client exchange messages
      case AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE:
        ClientExchangeMessage client = (ClientExchangeMessage) message;
        InetAddress addx = InetAddress.getByName(client.getIpAddress());
        dout.write(addx.getAddress());
        dout.writeShort(client.getPort());
        dout.write(client.getUsername().getBytes("UTF-16BE"));
        break;
      // Handshake messages.
      case AbstractMessage.TYPE_HANDSHAKE_MESSAGE:
        HandshakeMessage handshake = (HandshakeMessage) message;
        dout.write(HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE"));
        dout.writeShort(handshake.getListenPort());
        dout.write(handshake.getUsername().getBytes("UTF-16BE"));
        break;
      // Error handling
      default:
        System.err.println("Unknown message type: " + message.getType());
        break;
      }
    }
    // Always flush the output stream in case it's buffered.
    dout.flush();
  }

  /**
   * Decodes and returns the next message from the provided InputStream. If no
   * message can be decoded, returns null.
   * 
   * @param in
   *          the InputStream from which to decode the next message.
   * @return the decoded message, or null if no message can be decoded.
   * @throws IOException
   *           if the socket is closed or an IOException is thrown by the
   *           InputStream.
   */
  public static AbstractMessage decodeMessage(final InputStream in)
      throws IOException {
    // Check for null socket or EOF
    if (in == null || in.available() < 0) {
      throw new SocketException("Socket is null or closed.");
    }

    // Wrap for convenience
    DataInputStream din = new DataInputStream(in);

    // Message length and type should always be present
    int messageLength = din.readInt();
    byte messageType = din.readByte();

    AbstractMessage message = null;

    // Handle decoding based on the message type
    switch (messageType) {
    case AbstractMessage.TYPE_CHAT_MESSAGE: {
      // Decode the message timestamp
      long timestamp = din.readLong();

      // Decode the username as a UTF-16 big endian string
      int usernameLength = din.readInt();
      byte[] usernameBytes = new byte[usernameLength];
      din.readFully(usernameBytes);
      String username = new String(usernameBytes, "UTF-16BE");

      // Decode the chat message as a UTF-16 big endian string
      byte[] messageBytes = new byte[messageLength - 13 - usernameLength];
      din.readFully(messageBytes);
      String messageString = new String(messageBytes, "UTF-16BE");

      message = new ChatMessage(timestamp, username, messageString);

      break;
    }
    case AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE:
      // Decode the IP address of the client
      byte[] ipBytes = new byte[4];
      din.readFully(ipBytes);
      InetAddress addx = InetAddress.getByAddress(ipBytes);

      // Read the port number, masking just in case...
      int port = din.readShort() & 0xFFFF;

      // Decode the username as a UTF-16 big endian string
      byte[] unameBytes = new byte[messageLength - 7];
      din.readFully(unameBytes);
      String uname = new String(unameBytes, "UTF-16BE");

      message = new ClientExchangeMessage(addx.getHostAddress(), port, uname);
      break;
    case AbstractMessage.TYPE_HANDSHAKE_MESSAGE:
      // Decode the protocol string as a UTF-16 big endian string
      byte[] pstrBytes = new byte[HandshakeMessage.PROTOCOL_STRING
          .getBytes("UTF-16BE").length];
      din.readFully(pstrBytes);
      String protocolString = new String(pstrBytes, "UTF-16BE");
      // If the protocol strings do not match, print an error message
      // TODO: Need to signal to the client that the message is invalid
      if (!protocolString.equals(HandshakeMessage.PROTOCOL_STRING)) {
        System.err.println("Received invalid handshake protocol string: "
            + protocolString);
      }

      // Read the port number, masking just in case...
      int listenPort = din.readShort() & 0xFFFF;

      // Decoder the username as a UTF-16 big endian string
      byte[] nameBytes = new byte[messageLength - 3
          - HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE").length];
      din.readFully(nameBytes);
      String name = new String(nameBytes, "UTF-16BE");

      message = new HandshakeMessage(name, listenPort);
      break;
    case AbstractMessage.TYPE_DISCONNECT_MESSAGE:
      // Disconnect messages are all identical, so return the static instance
      message = AbstractMessage.DISCONNECT_MESSAGE;
      break;
    case AbstractMessage.TYPE_KEEPALIVE_MESSAGE:
      // Keep-alive messages are all identical, so return the static instance
      message = AbstractMessage.KEEPALIVE_MESSAGE;
      break;
    case AbstractMessage.TYPE_PRIVATE_CHAT_MESSAGE: {
      // Decode the message timestamp
      long timestamp = din.readLong();

      // Decode the username as a UTF-16 big endian string
      int usernameLength = din.readInt();
      byte[] usernameBytes = new byte[usernameLength];
      din.readFully(usernameBytes);
      String username = new String(usernameBytes, "UTF-16BE");

      // Decode the chat message as a UTF-16 big endian string
      byte[] messageBytes = new byte[messageLength - 13 - usernameLength];
      din.readFully(messageBytes);
      String messageString = new String(messageBytes, "UTF-16BE");

      message = new PrivateChatMessage(timestamp, username, messageString);
      break;
    }
    default:
      System.err.println("Unexpected message type when decoding: "
          + messageType);
    }

    return message;

  }

}

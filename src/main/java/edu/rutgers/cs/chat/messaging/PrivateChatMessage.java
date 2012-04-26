/*
 * CS352 Example Chat Client
 * Copyright (C) 2012 Rutgers University and Robert Moore
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
import java.util.Date;

/**
 * A chat message exchanged between two clients. Private chat messages are never
 * broadcast, and should be handled separately from normal (broadcast) chat
 * messages so the user can respond appropriately.
 * 
 * @author Robert Moore
 * 
 */
public class PrivateChatMessage extends AbstractMessage {

  /**
   * The time at which the message was created by the sending client.
   */
  protected final long timestamp;

  /**
   * The username at the sending client.
   */
  protected final String username;

  /**
   * The message that was sent.
   */
  protected final String message;

  /**
   * Creates a private chat message with the specified timestamp, username, and
   * message.
   * 
   * @param timestamp
   *          the time at which the message was created by the user.
   * @param username
   *          the username at the sending client.
   * @param message
   *          the message to send
   * @throws UnsupportedEncodingException
   *           if the username or message cannot be encoded in UTF-16 big
   *           endian.
   */
  public PrivateChatMessage(final long timestamp, final String username,
      final String message) throws UnsupportedEncodingException {
    super(13 + username.getBytes("UTF-16BE").length
        + message.getBytes("UTF-16BE").length,
        AbstractMessage.TYPE_PRIVATE_CHAT_MESSAGE);
    this.timestamp = timestamp;
    this.username = username;
    this.message = message;
  }

  /**
   * Returns the timestamp of this private chat message.
   * 
   * @return the timestamp of this private chat message.
   */
  public long getTimestamp() {
    return this.timestamp;
  }

  /**
   * Returns the username of this private chat message.
   * 
   * @return the username of this private chat message.
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Returns the message contained in this private chat message.
   * 
   * @return the message contained in this private chat message.
   */
  public String getMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString());
    sb.append(' ').append(this.username).append('@')
        .append(new Date(this.timestamp)).append("[P]: ").append(this.message);
    return sb.toString();
  }
}

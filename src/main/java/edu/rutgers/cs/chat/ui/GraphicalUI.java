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

package edu.rutgers.cs.chat.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;

import edu.rutgers.cs.chat.Client;

public class GraphicalUI extends JFrame implements UIAdapter, ActionListener,
		KeyListener {

	/**
	 * Logger for this class.
	 */
	private static final Logger log = Logger.getLogger(GraphicalUI.class
			.getName());

	private final JButton sendButton = new JButton("Send");
	private final JButton clearButton = new JButton("Clear");

	private final JTextArea textInput = new JTextArea(4, 80);

	private final JTextPane chatDisplay = new JTextPane();

	private final DefaultListModel userListModel = new DefaultListModel();
	private final JList userList = new JList(this.userListModel);

	private final Collection<UserInputListener> listeners = new ArrayList<UserInputListener>();

	private static final String STYLENAME_USER = "username";

	private static final String STYLENAME_INFO = "info";

	private static final String STYLENAME_PRIVATE = "private";

	private final Runnable autoScroller = new Runnable() {

		@Override
		public void run() {
			GraphicalUI.this.chatDisplay
					.setCaretPosition(GraphicalUI.this.chatDisplay
							.getStyledDocument().getLength());

		}
	};

	public GraphicalUI(final String username) {

		super("CS352 Chat Client for " + username);

		this.prepareChatArea();

		this.sendButton.addActionListener(this);
		this.clearButton.addActionListener(this);
		this.textInput.addKeyListener(this);

		this.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(2, 1));

		panel.add(this.sendButton);
		panel.add(this.clearButton);

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(new TitledBorder(BorderFactory
				.createLineBorder(Color.BLACK), "Your Message"));
		panel2.add(panel, BorderLayout.EAST);
		JScrollPane scroller = new JScrollPane(this.textInput);
		panel2.add(scroller, BorderLayout.CENTER);

		this.add(panel2, BorderLayout.SOUTH);
		panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(BorderFactory
				.createLineBorder(Color.BLACK), "Chat Log"));
		scroller = new JScrollPane(this.chatDisplay);
		panel.add(scroller, BorderLayout.CENTER);
		this.add(panel, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(BorderFactory
				.createLineBorder(Color.BLACK), "Users"));
		scroller = new JScrollPane(this.userList);
		panel.add(scroller, BorderLayout.CENTER);
		this.add(panel, BorderLayout.EAST);

		this.pack();
		this.setVisible(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				for (UserInputListener listener : GraphicalUI.this.listeners) {
					listener.userRequestedShutdown();
				}
			}
		});
	}

	private void prepareChatArea() {
		this.chatDisplay.setPreferredSize(new Dimension(640, 320));
		this.chatDisplay.setEditable(false);
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		Style def = StyleContext.getDefaultStyleContext().getStyle(
				StyleContext.DEFAULT_STYLE);

		Style usernameStyle = doc.addStyle(STYLENAME_USER, def);
		StyleConstants.setBold(usernameStyle, true);

		Style infoMessageStyle = doc.addStyle(STYLENAME_INFO, def);
		StyleConstants.setItalic(infoMessageStyle, true);

		Style privateMessageStyle = doc.addStyle(STYLENAME_PRIVATE, def);
		StyleConstants.setItalic(privateMessageStyle, true);
	}

	@Override
	public void broadcastMessageReceived(long timestamp, String message,
			Client fromClient) {
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String prefix = "["
				+ new SimpleDateFormat().format(new Date(timestamp)) + "] "
				+ fromClient + ":";
		String text = " " + message + "\n";
		try {
			doc.insertString(doc.getLength(), prefix,
					doc.getStyle(STYLENAME_USER));
			doc.insertString(doc.getLength(), text, null);
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with received message. Cause: " + e.getMessage());
		}

	}

	@Override
	public void broadcastMessageSent(long timestamp, String message) {

		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String prefix = "["
				+ new SimpleDateFormat().format(new Date(timestamp))
				+ "] I said:";
		String text = " " + message + "\n";
		try {
			doc.insertString(doc.getLength(), prefix,
					doc.getStyle(STYLENAME_USER));
			doc.insertString(doc.getLength(), text, null);
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with sent message. Cause: " + e.getMessage());
		}

	}

	@Override
	public void messageNotSent(String message, String reason, Client client) {
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String text = "(" + message + ") could not be sent to " + client
				+ "\nReason: " + reason + "\n";
		try {
			doc.insertString(doc.getLength(), text,
					doc.getStyle(STYLENAME_INFO));
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with unsent message. Cause: " + e.getMessage());
		}

	}

	@Override
	public void clientConnected(Client connectedClient) {
		log.fine(connectedClient + " connected.");
		this.userListModel.addElement(connectedClient);
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String text = "[" + new SimpleDateFormat().format(new Date()) + "] "
				+ connectedClient + " connected.\n";
		try {
			doc.insertString(doc.getLength(), text,
					doc.getStyle(STYLENAME_INFO));
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with connection notification. Cause: " + e.getMessage());
		}

	}

	@Override
	public void clientDisconnected(String reason, Client disconnectedClient) {
		this.userListModel.removeElement(disconnectedClient);
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String text = "[" + new SimpleDateFormat().format(new Date()) + "] "
				+ disconnectedClient + " disconnected.\n";
		try {
			doc.insertString(doc.getLength(), text,
					doc.getStyle(STYLENAME_INFO));
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with connection notification. Cause: " + e.getMessage());
		}

	}

	@Override
	public void addUserInputListener(UserInputListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.clearButton) {
			this.textInput.setText("");
		} else if (e.getSource() == this.sendButton) {
			this.sendChatMessage();
		}
	}

	private void sendChatMessage() {
		String msg = this.textInput.getText().trim();
		if (msg.length() > 0) {
			Client selectedUser = (Client) this.userList.getSelectedValue();
			this.userList.clearSelection();
			this.textInput.setText("");
			for (UserInputListener listener : this.listeners) {
				if (selectedUser != null) {
					listener.privateChatMessage(selectedUser, msg);
				} else {
					listener.broadcastChatMessage(msg);
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ENTER) {
			if (e.isShiftDown()) {
				this.textInput.append("\n");
				SwingUtilities.invokeLater(this.autoScroller);
			} else {
				this.sendChatMessage();
			}
		}
	}

	@Override
	public void privateMessageSent(long timestamp, String message, Client client) {
		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String prefix = "["
				+ new SimpleDateFormat().format(new Date(timestamp))
				+ "] I said (" + client + "):";
		String text = " " + message + "\n";
		try {
			doc.insertString(doc.getLength(), prefix,
					doc.getStyle(STYLENAME_USER));
			doc.insertString(doc.getLength(), text,
					doc.getStyle(STYLENAME_PRIVATE));
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with sent private message. Cause: " + e.getMessage());
		}
	}

	@Override
	public void privateMessageReceived(long timestamp, String message,
			Client client) {

		StyledDocument doc = this.chatDisplay.getStyledDocument();
		String prefix = "["
				+ new SimpleDateFormat().format(new Date(timestamp)) + "] ("
				+ client + "):";
		String text = " " + message + "\n";
		try {
			doc.insertString(doc.getLength(), prefix,
					doc.getStyle(STYLENAME_USER));
			doc.insertString(doc.getLength(), text,
					doc.getStyle(STYLENAME_PRIVATE));
			SwingUtilities.invokeLater(this.autoScroller);
		} catch (BadLocationException e) {
			log.warning("Couldn't update chat with received private message. Cause: " + e.getMessage());
		}

	}

}

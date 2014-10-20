package com.quickblox.sample.chat.core;

import java.io.InputStream;
import java.util.Calendar;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.quickblox.core.QBCallbackImpl;
import com.quickblox.core.result.Result;
import com.quickblox.module.chat.QBChatService;
import com.quickblox.module.chat.listeners.ChatMessageListener;
import com.quickblox.module.chat.xmpp.QBPrivateChat;
import com.quickblox.module.content.QBContent;
import com.quickblox.module.content.result.QBFileDownloadResult;
import com.quickblox.sample.chat.model.ChatMessage;
import com.quickblox.sample.chat.ui.activities.ChatActivity;

public class SingleChat implements Chat, ChatMessageListener {

	public static final String EXTRA_USER_ID = "user_id";

	private ChatActivity chatActivity;
	private QBPrivateChat chat;
	private int companionId;

	public static boolean sendingMessage = false;

	public SingleChat(ChatActivity chatActivity) {
		this.chatActivity = chatActivity;
		companionId = chatActivity.getIntent().getIntExtra(EXTRA_USER_ID, 0);
		chat = QBChatService.getInstance().createChat();
		chat.addChatMessageListener(this);
	}

	@Override
	public void release() {
		chat.removeChatMessageListener(this);
	}

	@Override
	public void processMessage(Message message) {
		PacketExtension composeExtension = message.getExtension("state",
				"jabber:state:event");
		PacketExtension attachmentExtension = message.getExtension(
				"attachment", "jabber:attachment:event");
		if (composeExtension != null) {
			try {
				String value = ((DefaultPacketExtension) composeExtension)
						.getValue("composing");
				if (value.equals("true")) {
					chatActivity.showTypingText("Typing...");
				} else {
					chatActivity.showTypingText("");
				}
			} catch (Exception e) {

			}

			try {
				String value2 = ((DefaultPacketExtension) composeExtension)
						.getValue("delivered");
				if (value2.equals("true")) {
					sendingMessage = false;
				} else {

				}
			} catch (Exception e) {

			}

		} else if (attachmentExtension != null) {
			if (attachmentExtension != null) {
				String uid = ((DefaultPacketExtension) attachmentExtension)
						.getValue("fileID");
				final String type = ((DefaultPacketExtension) attachmentExtension)
						.getValue("file_type");
				
				Log.e("SingleChat", "File ID  : "+uid);
				Log.e("SingleChat", "File type  :  "+type);
				
				// download file by ID
				QBContent.downloadFile(uid, new QBCallbackImpl() {
					@Override
					public void onComplete(Result result) {
						QBFileDownloadResult downloadResult = (QBFileDownloadResult) result;
						InputStream s = downloadResult.getContentStream();
						if (type.equals("image")) {
							Bitmap bitmap = BitmapFactory.decodeStream(s);
							showBitmapMessage(bitmap);
						} else if(type.equalsIgnoreCase("audio")) { 
							Log.e("SingleChat", "Audio file received");
						}
					}
				}); 
				
				showBitmapMessage(null);
				
			}
		} else {
			final String messageBody = message.getBody();
			// Show message
			chatActivity.showMessage(new ChatMessage(messageBody, Calendar
					.getInstance().getTime(), true, false, "text", null));
		}

		try {
			DefaultPacketExtension extension = new DefaultPacketExtension(
					"state", "jabber:state:event");
			extension.setValue("delivered", "true");

			Message msg = new Message();
			msg.setType(Message.Type.chat);
			msg.addExtension(extension);

			chat.sendMessage(companionId, msg);
		} catch (Exception e) {

		}

	}

	public void showBitmapMessage(Bitmap bitmap) {
		chatActivity.showMessage(new ChatMessage("", Calendar.getInstance()
				.getTime(), true, true, "image", bitmap));
	}

	@Override
	public boolean accept(Message.Type messageType) {
		switch (messageType) {
		case chat:
			return true;
		default:
			return false;
		}
	}

	@Override
	public void sendMessage(String message) throws XMPPException {
		chat.sendMessage(companionId, message);
	}

	@Override
	public void opponentTyping(boolean b) throws XMPPException {
		Message msg = new Message();
		DefaultPacketExtension extension = new DefaultPacketExtension("state",
				"jabber:state:event");
		if (b) {
			extension.setValue("composing", "true");
		} else {
			extension.setValue("composing", "false");
		}
		msg.setType(Message.Type.chat);
		msg.addExtension(extension);
		chat.sendMessage(companionId, msg);
	}

	@Override
	public void sendFileMessage(String uId, final String fileType)
			throws XMPPException {
		DefaultPacketExtension extension = new DefaultPacketExtension(
				"attachment", "jabber:attachment:event");
		extension.setValue("fileID", uId);
		extension.setValue("file_type", fileType);

		Message message = new Message();
		message.setType(Message.Type.chat); // 1-1 chat message
		message.addExtension(extension);

		chat.sendMessage(companionId, message);
	}

}

package com.quickblox.sample.chat.model;

import java.util.Date;

import android.graphics.Bitmap;

public class ChatMessage {

	private boolean incoming;
	private String text;
	private Date time;
	private String sender;

	boolean isFile;
	private String fileType;
	
	private Bitmap bitmap;

	public ChatMessage(String text, Date time, boolean incoming,
			boolean isFile, String fileType, Bitmap bitmap) {
		this(text, null, time, incoming, isFile, fileType, bitmap);
	}

	public ChatMessage(String text, String sender, Date time, boolean incoming,
			boolean isFile, String fileType, Bitmap bitmap) {
		this.text = text;
		this.sender = sender;
		this.time = time;
		this.incoming = incoming;
		this.isFile = isFile;
		this.fileType = fileType;
		this.bitmap = bitmap;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public boolean isFile() {
		return isFile;
	}

	public void setFile(boolean isFile) {
		this.isFile = isFile;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public void setIncoming(boolean incoming) {
		this.incoming = incoming;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public boolean isIncoming() {
		return incoming;
	}

	public String getText() {
		return text;
	}

	public Date getTime() {
		return time;
	}

	public String getSender() {
		return sender;
	}
}

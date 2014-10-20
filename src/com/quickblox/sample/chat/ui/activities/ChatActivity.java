package com.quickblox.sample.chat.ui.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.quickblox.core.QBCallbackImpl;
import com.quickblox.core.result.Result;
import com.quickblox.module.content.QBContent;
import com.quickblox.module.content.result.QBFileUploadTaskResult;
import com.quickblox.sample.chat.App;
import com.quickblox.sample.chat.R;
import com.quickblox.sample.chat.core.Chat;
import com.quickblox.sample.chat.core.RoomChat;
import com.quickblox.sample.chat.core.SingleChat;
import com.quickblox.sample.chat.model.ChatMessage;
import com.quickblox.sample.chat.ui.adapters.ChatAdapter;

public class ChatActivity extends Activity {

	public static final String EXTRA_MODE = "mode";
	private static final String TAG = ChatActivity.class.getSimpleName();
	private EditText messageEditText;
	private Mode mode = Mode.SINGLE;
	private Chat chat;
	public ChatAdapter adapter;

	private ListView messagesContainer;
	TextView meLabel, companionLabel, statusTv;
	Button sendButton, attachButton;

	String opponentName, picturePath;
	AlertDialog alert = null;

	public static void start(Context context, Bundle bundle) {
		Intent intent = new Intent(context, ChatActivity.class);
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		initViews();
	}

	@Override
	public void onBackPressed() {
		try {
			chat.release();
		} catch (XMPPException e) {
			Log.e(TAG, "failed to release chat", e);
		}
		super.onBackPressed();
	}

	private void initViews() {
		messagesContainer = (ListView) findViewById(R.id.messagesContainer);
		messageEditText = (EditText) findViewById(R.id.messageEdit);

		sendButton = (Button) findViewById(R.id.chatSendButton);
		attachButton = (Button) findViewById(R.id.attachbutton);

		meLabel = (TextView) findViewById(R.id.meLabel);
		companionLabel = (TextView) findViewById(R.id.companionLabel);
		statusTv = (TextView) findViewById(R.id.status_tv);

		RelativeLayout container = (RelativeLayout) findViewById(R.id.container);

		adapter = new ChatAdapter(this, new ArrayList<ChatMessage>());
		messagesContainer.setAdapter(adapter);

		Intent intent = getIntent();
		mode = (Mode) intent.getSerializableExtra(EXTRA_MODE);
		opponentName = intent.getStringExtra("opponent_name");

		switch (mode) {
		case GROUP:
			chat = new RoomChat(this);
			container.removeView(meLabel);
			container.removeView(companionLabel);
			break;
		case SINGLE:
			chat = new SingleChat(this);
			int userId = intent.getIntExtra(SingleChat.EXTRA_USER_ID, 0);
			companionLabel.setText(opponentName);
			restoreMessagesFromHistory(userId);
			break;
		}

		attachButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showSelectionDialog();
			}
		});

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				String lastMsg = messageEditText.getText().toString();
				if (TextUtils.isEmpty(lastMsg)) {
					return;
				}

				messageEditText.setText("");
				try {
					SingleChat.sendingMessage = true;
					chat.sendMessage(lastMsg);
				} catch (XMPPException e) {
					Log.e(TAG, "failed to send a message", e);
				}

				if (mode == Mode.SINGLE) {
					showMessage(new ChatMessage(lastMsg, Calendar.getInstance()
							.getTime(), false, false, "text", null));
				}
			}
		});

		messageEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				try {
					if (s.length() > 0) {
						chat.opponentTyping(true);
					} else {
						chat.opponentTyping(false);
					}
				} catch (XMPPException e) {
					Log.e(TAG, "failed to send a message", e);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

	}

	protected void showSelectionDialog() {

		final String[] ITEMS = new String[] { "Photos", "Audio", "Video" };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Hello, title!");

		LayoutInflater factory = LayoutInflater.from(this);
		View content = factory.inflate(R.layout.dialog_selection, null);

		ListView lv = (ListView) content.findViewById(R.id.list);
		lv.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, ITEMS));
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (arg2 == 0) {
					Intent i = new Intent(
							Intent.ACTION_PICK,
							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(i, 111);
				} else if (arg2 == 1) {
					Intent intent = new Intent();
					intent.setType("audio/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(
							Intent.createChooser(intent, "Select Audio "), 112);
				} else if (arg2 == 2) {

				}
				alert.dismiss();
			}
		});

		builder.setView(content);

		alert = builder.create();
		alert.show();
	}

	public void showMessage(ChatMessage message) {
		saveMessageToHistory(message);
		adapter.add(message);
		adapter.notifyDataSetChanged();
		scrollDown();
	}

	public void showMessage(List<ChatMessage> messages) {
		adapter.add(messages);
		adapter.notifyDataSetChanged();
		scrollDown();
	}

	public void saveMessageToHistory(ChatMessage message) {
		if (mode == Mode.SINGLE) {
			((App) getApplication()).addMessage(
					getIntent().getIntExtra(SingleChat.EXTRA_USER_ID, 0),
					message);
		}
	}

	private void restoreMessagesFromHistory(int userId) {
		List<ChatMessage> messages = ((App) getApplication())
				.getMessages(userId);
		if (messages != null) {
			showMessage(messages);
		}
	}

	public void scrollDown() {
		messagesContainer.setSelection(messagesContainer.getCount() - 1);
	}

	public static enum Mode {
		SINGLE, GROUP
	}

	public void showTypingText(String message) {
		statusTv.setText(message);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 111 && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			picturePath = cursor.getString(columnIndex);
			cursor.close();

			if (mode == Mode.SINGLE) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);

				showMessage(new ChatMessage("", Calendar.getInstance()
						.getTime(), false, true, "image", bitmap));
			}
			uploadFileToContentModule(picturePath, "image");
		} else if (requestCode == 112 && resultCode == RESULT_OK
				&& null != data) {
			Uri selectedAudio = data.getData();
			picturePath = getPath(selectedAudio);

			uploadFileToContentModule(picturePath, "audio");

		}
	}

	public String getPath(Uri uri) {
		try {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(uri, projection, null,
					null, null);
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} catch (Exception e) {

		}
		return "";
	}

	private void uploadFileToContentModule(String picturePath,
			final String fileType) {
		File image = new java.io.File(picturePath);
		SingleChat.sendingMessage = true;

		QBContent.uploadFileTask(image, false, new QBCallbackImpl() {
			@Override
			public void onComplete(Result result) {
				if (result.isSuccess()) {
					QBFileUploadTaskResult res = (QBFileUploadTaskResult) result;
					String uid = res.getFile().getUid();
					try {
						chat.sendFileMessage(uid, fileType);
					} catch (XMPPException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void showBitmap(Bitmap bitmap) {

	}

}

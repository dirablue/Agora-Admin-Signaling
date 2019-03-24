package admin.signaling.data;

import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import admin.signaling.service.SignalService;
import admin.signaling.type.ChannelStatus;
import admin.signaling.type.LoginStatus;
import admin.signaling.type.MessageJsonKeys;
import admin.signaling.type.MessageTypes;
import admin.signaling.type.RecordingStatus;
import io.agora.signal.Signal;
import io.agora.signal.Signal.LoginSession;
import io.agora.signal.Signal.LoginSession.Channel;
import io.agora.signal.Signal.MessageCallback;

public class SignalingAccount {

	private static final Logger log = LoggerFactory.getLogger(SignalService.class);

	/**
	 * Account
	 */
	public String accountName = null;
	public String token = null;
	public Integer uid = null;
	public Signal signal = null;

	public Signal.LoginSession session = null;
	public LoginStatus loginStatus = LoginStatus.None;

	/**
	 * Channel
	 */
	public String channelName = null;
	public Channel channel = null;
	public ChannelStatus channelStatus = ChannelStatus.None;
	public long lastChannelMessageTimeSec = 0;
	public long recordingStopTimeSec = 0;

	public String channelHostAccount = null;
	
	public RecordingStatus recordingStatus = RecordingStatus.None;
	private long recordingGroupStartTime = 0;
	public ConcurrentLinkedQueue<String> groupMessageQueue = new ConcurrentLinkedQueue<String>();

	public Thread recordingClearTimerThread;
	public long recordingDelayTimeSec;

	private Gson gson = new Gson();
	private static final Type JsonType = new TypeToken<HashMap<String, Object>>() {
	}.getType();


	public SignalingAccount(String appId, String accountName) {
		this.signal = new Signal(appId);
		this.accountName = accountName;
	}

	public void login() {
		login(null);
	}

	public void login(Callable<Void> callback) {

		log.info("login accountName={}", this.accountName);
		this.loginStatus = LoginStatus.Connecting;

		String token = this.token;
		if (token == null) {
			token = "_no_need_token";
		}

		String accountName = this.accountName;

		this.signal.login(accountName, token, new Signal.LoginCallback() {
			@Override
			public void onLoginSuccess(final Signal.LoginSession newLoginSession, int newUid) {
				log.info("onLoginSuccess account={} uid={}", accountName, newUid);
				session = newLoginSession;
				uid = newUid;
				loginStatus = LoginStatus.LogIn;

				if (callback != null) {
					try {
						callback.call();
					} catch (Exception e) {
					}
				}
			}

			@Override
			public void onLoginFailed(LoginSession session, int ecode) {
				super.onLoginFailed(session, ecode);
				log.warn("onLoginFailed ecode={}", ecode);
				loginStatus = LoginStatus.LogOff;
			}

			@Override
			public void onLogout(Signal.LoginSession session, int ecode) {
				log.warn("onLogout ecode={}", ecode);
				loginStatus = LoginStatus.LogOff;
			}

			@Override
			public void onMessageInstantReceive(Signal.LoginSession session, String account, int uid, String msg) {
				log.info("onMessageInstantReceive account={} uid={} msg={}", account, uid, msg);
			}
		});
	}

	public void joinChannel(String channelName, String channelHostAccount, Callable<Void> callback) {
		if (loginStatus != LoginStatus.LogIn) {
			log.warn("Failed to join Channel. the connection is not connected. account={} uid={} loginStatus={}",
					this.accountName, this.uid, this.loginStatus);
			return;
		}

		this.channelName = channelName;
		this.channelHostAccount = channelHostAccount;

		this.recordingStatus = RecordingStatus.None;
		this.lastChannelMessageTimeSec = System.currentTimeMillis() / 1000;

		if (channel != null) {
			channel.channelClearAttr();
			groupMessageQueue.clear();
		}
		channel = session.channelJoin(channelName, new Signal.ChannelCallback() {

			@Override
			public void onChannelJoined(Signal.LoginSession session, Signal.LoginSession.Channel channel) {
				channelStatus = ChannelStatus.Joined;

				log.info("onChannelJoined account={} uid={} channel={}", accountName, uid, channelName);

				// if not ready yet
				if (recordingStatus == RecordingStatus.None) {
					recordingStatus = RecordingStatus.Ready;

					try {
						if (callback != null) {
							callback.call();
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}

			@Override
			public void onChannelJoinFailed(LoginSession session, Channel channel, int ecode) {
				channelStatus = ChannelStatus.FailedToJoin;
				log.info("onChannelJoinFailed account={} uid={} channel={} ecode={}", accountName, uid, channelName,
						ecode);
			}

			@Override
			public void onChannelLeaved(Signal.LoginSession session, Signal.LoginSession.Channel channel, int ecode) {
				channelStatus = ChannelStatus.Leaved;
				log.info("onChannelLeaved account={} uid={} channel={} ecode={}", accountName, uid, channelName, ecode);
			}

			@Override
			public void onMessageChannelReceive(Signal.LoginSession session, Signal.LoginSession.Channel channel,
					String account, int uid, String msg) {
				log.info("onMessageChannelReceive account={} uid={} channel={} msg={}", account, uid, channelName, msg);

				lastChannelMessageTimeSec = System.currentTimeMillis() / 1000;

				if (recordingStatus == RecordingStatus.Ready) {
					// recording is not started yet
					try {
						Map<String, Object> mapData = gson.fromJson(msg, JsonType);
						if (mapData == null)
							return;

						String messageType = (String) mapData.get(MessageJsonKeys.MessageType);
						if (MessageTypes.Start.equals(messageType) && account.equals(channelHostAccount)) {

							recordingStatus = RecordingStatus.Start;
							recordingGroupStartTime = System.currentTimeMillis();
							log.info("Starting Recording group message. account={} uid={} channel={} msg={}", account,
									uid, channelName, msg);
						}

					} catch (Exception e) {
					}

				} else if (recordingStatus == RecordingStatus.Start) {
					try {
						Map<String, Object> mapData = gson.fromJson(msg, JsonType);
						if (mapData == null)
							return;

						long recordedTime = (System.currentTimeMillis() - recordingGroupStartTime) / 1000;
						recordedTime -= recordingDelayTimeSec;

						mapData.put(MessageJsonKeys.RecordedTime, recordedTime);
						groupMessageQueue.add(gson.toJson(mapData));

						log.info("Recored Message. account={} uid={} channel={} msg={} recordedTime={}", account, uid,
								channelName, msg, recordedTime);

					} catch (Exception e) {
					}
				}
			}

		});
	}

	public void leaveChannel() {
		if (channel != null) {
			channel.channelLeave();
			channel.channelClearAttr();
			channel = null;
		}
	}

	public void resetChannelData() {
		leaveChannel();

		this.channelName = null;
		this.channelHostAccount = null;
		this.channelStatus = ChannelStatus.None;
		this.recordingStatus = RecordingStatus.None;
		this.recordingGroupStartTime = 0;
		this.recordingDelayTimeSec = 0;
		this.groupMessageQueue.clear();

		if (this.recordingClearTimerThread != null) {
			this.recordingClearTimerThread.interrupt();
			this.recordingClearTimerThread = null;
		}
	}

	public void sendPrivateMessage(String toAccount, String message) {

		session.messageInstantSend(toAccount, message, new MessageCallback() {
			@Override
			public void onMessageSendSuccess(Signal.LoginSession session) {
				log.info("PrivateMessage:onMessageSendSuccess account={} toAccount={} message={}", accountName,
						toAccount, message);
			}

			@Override
			public void onMessageSendError(LoginSession session, int ecode) {
				// retry if there is error
				log.warn("PrivateMessage:onMessageSendError account={} toAccount={} message={}", accountName, toAccount,
						message);
			}
		});
	}

	public void sendGroupMessage(String message) {

		channel.messageChannelSend(message, new MessageCallback() {
			@Override
			public void onMessageSendSuccess(Signal.LoginSession session) {
				log.info("GroupMessage:onMessageSendSuccess account={} channel={} message={}", accountName, channelName,
						message);
			}

			@Override
			public void onMessageSendError(LoginSession session, int ecode) {
				// retry if there is error
				log.warn("GroupMessage:onMessageSendError account={} channel={} message={}", accountName, channelName,
						message);
			}
		});
	}

	public Path outputGroupMessagesToFile(String outputDir) {
		try {
			boolean isError = true;
			String fileSuffix = "";
			Path filePath = null;

			for (int i = 0; i < 5; i++) {
				filePath = Paths.get(outputDir + "/" + channelName + fileSuffix + ".txt");
				filePath = filePath.toAbsolutePath();

				if (Files.exists(filePath)) {
					fileSuffix = "_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date()) + i;

				} else {
					isError = false;
					break;
				}
			}
			if (isError) {
				log.error("Couldn't create output file. account={} channel={}", accountName, channelName);
				return null;
			}

			try (FileWriter writer = new FileWriter(filePath.toString())) {
				gson.toJson(groupMessageQueue, writer);
			}

			return filePath;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

}

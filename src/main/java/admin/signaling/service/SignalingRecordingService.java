/**
 * 
 */
package admin.signaling.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import admin.signaling.AppProperties;
import admin.signaling.client.AWSClient;
import admin.signaling.data.SignalingAccount;
import admin.signaling.type.LoginStatus;
import admin.signaling.type.MessageJsonKeys;
import admin.signaling.type.MessageTypes;
import admin.signaling.type.RecordingStatus;

/**
 * @author gaku
 *
 */
@Service
public class SignalingRecordingService {
	
	private static final Logger log = LoggerFactory.getLogger(SignalingRecordingService.class);
	
	
    @Autowired
    private AppProperties appProp;
    
    @Autowired
    private AWSClient awsClient;
	
    private ConcurrentLinkedQueue<SignalingAccount> accountQueue = new ConcurrentLinkedQueue<SignalingAccount>();
    
    private Map<String, SignalingAccount> usedAccountMap = new ConcurrentHashMap<String, SignalingAccount>();
    
    public ConcurrentLinkedQueue<SignalingAccount> getAccountQueue() {
		return accountQueue;
	}
    
    public Map<String, SignalingAccount> getUsedAccountMap() {
		return usedAccountMap;
	}
    
    @Async
    public void createChannelAccounts(int num) {
    	for (int i = 0; i < num; i++) {
    		SignalingAccount account = createAccount();
    		account.login(null);
    	}
    }
    
    private SignalingAccount createAccount() {
		UUID uuid = UUID.randomUUID();
		
		String accountName = "+channel_account_" + uuid.toString();
		
		log.info("Create Channel Account. account={}", accountName);
		
		SignalingAccount account = new SignalingAccount(appProp.getAppId(), accountName);
		accountQueue.add(account);
		
		return account;
    }
    
    @Async
	public void readyRecording(String channelName, String hostAccount, SignalingAccount adminAccount) {
    	
    	if (channelName == null) {
    		log.warn("ChannelName is missing. channelName={}", channelName);
    		return;
    	}
    	
    	synchronized(channelName) {
			if (usedAccountMap.containsKey(channelName)) {
				log.warn("Failed to Start Recording. channel account is alredy started. channelName={}", channelName);
				return;
			}
			
			SignalingAccount account = null;
			
			List<SignalingAccount> loggedOffAccounts = new ArrayList<SignalingAccount>();
			
			while ((account = accountQueue.poll()) != null) {
				if (account.loginStatus == LoginStatus.LogIn) break;
				
				loggedOffAccounts.add(account);
			}
			
			accountQueue.addAll(loggedOffAccounts);
			
			if (account == null) {
				final SignalingAccount targetAccount = createAccount();
				targetAccount.login(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						joinChannel(targetAccount, channelName, hostAccount, adminAccount);
						return null;
					}
				});
			} else {
				joinChannel(account, channelName, hostAccount, adminAccount);
			}
			
			return;
    	}
	}
    
    @SuppressWarnings("unchecked")
	@Async
	public void resumeRecording(String channelName, String hostAccount, SignalingAccount adminAccount) {
    	synchronized(channelName) {
    		SignalingAccount account = usedAccountMap.get(channelName);
    		if (account == null) {
				log.warn("Failed to Resume Recording. channel account is alredy started. channelName={}", channelName);
				return;
    		}
    		if (account.recordingStatus != RecordingStatus.Stop) {
    			// if it's not stopped, return
    			return;
    		}
    		if (account.recordingClearTimerThread != null) {
    			account.recordingClearTimerThread.interrupt();
    			account.recordingClearTimerThread = null;
    		}

    		long delayTimeSec = (System.currentTimeMillis() / 1000) - account.recordingStopTimeSec;
    		account.recordingDelayTimeSec = delayTimeSec;
    		account.lastChannelMessageTimeSec = System.currentTimeMillis() / 1000;
    		account.recordingStatus = RecordingStatus.Start;
    		
    		JSONObject json = new JSONObject();
    		json.put(MessageJsonKeys.MessageType, MessageTypes.Resume);
    		json.put(MessageJsonKeys.Slug, channelName);
    		
    		adminAccount.sendPrivateMessage(hostAccount, json.toJSONString());
    		
    		return;
    	}
    }
    
    private void joinChannel(SignalingAccount account, String channelName, String hostAccount, 
    		SignalingAccount adminAccount) {
    	
		usedAccountMap.put(channelName, account);
		account.joinChannel(channelName, hostAccount, new Callable<Void>() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Void call() throws Exception {
				
        		JSONObject json = new JSONObject();
        		json.put(MessageJsonKeys.MessageType, MessageTypes.Ready);
        		json.put(MessageJsonKeys.Slug, channelName);
        		adminAccount.sendPrivateMessage(hostAccount, json.toJSONString());
        		
				return null;
			}
		});
    }
    
    public String stopRecordingByAccount(SignalingAccount account, boolean isSaveChatLog) {
    	
    	String channelName = account.channelName;
    	
    	synchronized (channelName) {
    		if (account.recordingStatus != RecordingStatus.Stop) {
    			account.recordingStatus = RecordingStatus.Stop;
    			
    		} else {
    			// if recording is already stopped, return
    			return null;
    		}
		}
    	
    	log.info("stopRecordingByAccount. channelName={}", channelName);
    	
    	String savedFilePath = null;
		account.recordingStopTimeSec = System.currentTimeMillis() / 1000;
		
		if (isSaveChatLog) {
			Path outputFilePath = account.outputGroupMessagesToFile(appProp.getRecordingFileOutputDir());
			if (outputFilePath == null) {
				log.info("Failed to file output. path={}", outputFilePath);
				
			} else {
				log.info("Succeeded file output. path={}", outputFilePath);
				
				// upload to S3
				String uploadFile = channelName + ".txt";
				savedFilePath = awsClient.uploadFileToS3(uploadFile, outputFilePath);
				log.info("Succeeded to upload file. to={} from={}", uploadFile, outputFilePath);
			}
		}
		
		if (account.recordingClearTimerThread != null) {
			account.recordingClearTimerThread.interrupt();
			account.recordingClearTimerThread = null;
		}
		
		account.recordingClearTimerThread = new Thread(() -> {
			try {
				log.info("Account will be reset in {}sec. channelName={}", 
						appProp.getRecordingResumeSec(), channelName);
				
				// wait for resume time. if there is no resume, clear account
				TimeUnit.SECONDS.sleep(appProp.getRecordingResumeSec());
				
				synchronized (channelName) {
					usedAccountMap.remove(channelName);
					account.resetChannelData();
					accountQueue.add(account);
					
					log.info("Account has been reset. channelName={}", channelName);
				}
				
			} catch (InterruptedException e) {
				log.info("Account is resumed. channelName={}", channelName);
			}
			
		});
		account.recordingClearTimerThread.start();
		
		return savedFilePath;
    }
    	
	public String stopRecording(String channelName) {
		if (channelName == null) {
			log.warn("ChannelName is missing. channelName={}", channelName);
			return null;
		}
		
		SignalingAccount account = usedAccountMap.get(channelName);
		if (account == null) {
			log.warn("ChannelName is missing. channelName={}", channelName);
			return null;
		}
		
		return stopRecordingByAccount(account, true);
	}
		
	@Scheduled(fixedRate = 5000)
	public void reloginScheduler() {
		try {
			for (SignalingAccount account : accountQueue) {
				if (account.loginStatus == LoginStatus.LogOff) {
					account.login(null);
				}
			}

		} catch (Exception e) {
			log.error("error", e);
		}
	}
	
	@Scheduled(fixedRate = 10000)
	public void autoStopRecordingScheduler() {
		try {
			long currentTime = System.currentTimeMillis() / 1000;
			for (SignalingAccount account : usedAccountMap.values()) {
				
				if (account.recordingStatus != RecordingStatus.Stop && 
					account.lastChannelMessageTimeSec + appProp.getRecordingTimeoutSec() < currentTime) {
					// timeout
					log.info("Timeout. channelName={}", account.channelName);
					stopRecordingByAccount(account, true);
				}
			}

		} catch (Exception e) {
			log.error("error", e);
		}
	}
	
	@Scheduled(fixedRate = 60000)
	public void removeRecordingFileScheduler() {
		try {
			long keepTimeSec = DateTime.now().minusDays(appProp.getRecordingFileKeepDays()).getMillis() / 1000;
			
			Path path = Paths.get(appProp.getRecordingFileOutputDir());
			Files.list(path)
		        .filter(n -> {
		            try {
		                return Files.getLastModifiedTime(n)
		                        .to(TimeUnit.SECONDS) < keepTimeSec;
		            } catch (IOException ex) {
		                return false;
		            }
		        })
		        .forEach(n -> {
		            try {
		                Files.delete(n);
		            } catch (IOException ex) {
		            }
		        });

		} catch (Exception e) {
			log.error("error", e);
		}
	}
	
}

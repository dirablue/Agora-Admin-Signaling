package admin.signaling.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import admin.signaling.AppProperties;
import admin.signaling.data.SignalingAccount;
import admin.signaling.type.LoginStatus;

@Service
public class SignalService {
	
	private static final Logger log = LoggerFactory.getLogger(SignalService.class);
	
    @Autowired
    private AppProperties appProp;
	
	private SignalingAccount adminAccount;
	
	public void loginAdminAccount() {
		try {
			if (adminAccount == null) {
				UUID uuid = UUID.randomUUID();
				
				adminAccount = new SignalingAccount(
					appProp.getAppId(), 
					appProp.getAdminName() + "_" + uuid.toString()
				);
			}
			adminAccount.login();
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public SignalingAccount getAdminAccount() {
		return adminAccount;
	}
	
	@Async
	public void sendAdminPrivateMessage(String toAccount, String message) {
		try {
			adminAccount.sendPrivateMessage(toAccount, message);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Scheduled(fixedRate = 5000)
	public void resendScheduler() {
		try {
			if (adminAccount != null) {
				if (adminAccount.loginStatus == LoginStatus.LogOff) {
					adminAccount.login();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
}

package admin.signaling.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.internal.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import admin.signaling.AppProperties;
import admin.signaling.data.SignalingAccount;
import admin.signaling.error.ForbiddenException;
import admin.signaling.error.ServerException;
import admin.signaling.service.SignalService;
import admin.signaling.service.SignalingRecordingService;

@RestController
public class AdminController {
	
	private static final Logger log = LoggerFactory.getLogger(AdminController.class);
	
    @Autowired
    private SignalService signalService;
    
    @Autowired
    private SignalingRecordingService signalRecordingService;
    
    @Autowired
    private AppProperties appProp;

    private Map<String, Object> getSuccessResponse(Map<String, Object> params) {
    	Map<String, Object> res = new HashMap<String, Object>();
    	res.put("status", "ok");
    	res.put("params", params);
    	return res;
    }
    
    private Map<String, Object> getErrorResponse(Map<String, Object> params) {
    	Map<String, Object> res = new HashMap<String, Object>();
    	res.put("status", "error");
    	res.put("params", params);
    	return res;
    }
    
    private void verifySigkey(Map<String, Object> params) {
    	Object sigkey = params.get("sigkey");
    	if (sigkey == null || !sigkey.equals(appProp.getSigKey())) {
    		throw new ForbiddenException();
    	}
    }

    /**
     * Send any json message from an admin user to an specific user
     * @param params
     *  sigkey: signal key for verification
     * 	message:  sending message
     *  to_account: sending target user
     * @return
     */
    @RequestMapping("/admin/send")
    public ResponseEntity<Object> adminSend(
    		@RequestBody Map<String, Object> params) {
    	
    	try {
    		verifySigkey(params);
	    	
	    	String message = (String) params.get("message");
	    	String toAccount = (String) params.get("to_account");
	    	
	    	if (StringHelper.isNullOrEmptyString(toAccount)) {
	    		return new ResponseEntity<Object>(
	    				getErrorResponse(params), HttpStatus.BAD_REQUEST);
	    	}
	    	
	    	signalService.sendAdminPrivateMessage(toAccount, message);
	    	
	    	Map<String, Object> res = getSuccessResponse(params);
	    	return new ResponseEntity<Object>(res, HttpStatus.OK);
	    	
    	} catch (ForbiddenException e) {
    		throw e;
	    	
    	} catch (Exception e) {
    		log.error(e.getMessage(), e);
    		throw new ServerException();
    	}
    }
    
    /**
     * Send a ready message from an admin user to an specific user
     * @param params 
     *  sigkey: signal key for verification
     * 	message:  sending message
     *  to_account: sending target user
     * @return
     */
    @RequestMapping("/admin/recording/ready")
    public ResponseEntity<Object> adminRecordingReady(
    		@RequestBody Map<String, Object> params) {
    	
    	try {
    		verifySigkey(params);
	    	
	    	String channel = (String) params.get("channel");
	    	String hostAccount = (String) params.get("host_account");
	    	
	    	if (StringHelper.isNullOrEmptyString(channel) ||
	    		StringHelper.isNullOrEmptyString(hostAccount)) {
	    		
	    		return new ResponseEntity<Object>(
	    				getErrorResponse(params), HttpStatus.BAD_REQUEST);
	    	}
	    	
	    	signalRecordingService.readyRecording(channel, hostAccount, signalService.getAdminAccount());
	    	
	    	Map<String, Object> res = getSuccessResponse(params);
	    	return new ResponseEntity<Object>(res, HttpStatus.OK);
	    	
    	} catch (ForbiddenException e) {
    		throw e;
	    	
    	} catch (Exception e) {
    		log.error(e.getMessage(), e);
    		throw new ServerException();
    	}
    }
    
    /**
     * Resume a recording signaling
     * @param params
     *  sigkey: signal key for verification
     * 	channel:  target channel
     *  host_account: host account of a channel
     * @return
     */
    @RequestMapping("/admin/recording/resume")
    public ResponseEntity<Object> adminRecordingResume(
    		@RequestBody Map<String, Object> params) {
    	
    	try {
    		verifySigkey(params);
	    	
	    	String channel = (String) params.get("channel");
	    	String hostAccount = (String) params.get("host_account");
	    	
	    	if (StringHelper.isNullOrEmptyString(channel) ||
		    	StringHelper.isNullOrEmptyString(hostAccount)) {
	    		
	    		return new ResponseEntity<Object>(
	    				getErrorResponse(params), HttpStatus.BAD_REQUEST);
	    	}
	    	
	    	signalRecordingService.resumeRecording(channel, hostAccount, signalService.getAdminAccount());
	    	
	    	Map<String, Object> res = getSuccessResponse(params);
	    	return new ResponseEntity<Object>(res, HttpStatus.OK);
	    	
    	} catch (ForbiddenException e) {
    		throw e;
	    	
    	} catch (Exception e) {
    		log.error(e.getMessage(), e);
    		throw new ServerException();
    	}
    }
    
    /**
     * Stop a recording signaling
     * @param params
     *  sigkey: signal key for verification
     * 	channel:  target channel
     * @return
     */
    @RequestMapping("/admin/recording/stop")
    public ResponseEntity<Object> adminRecordingStop(
    		@RequestBody Map<String, Object> params) {
    	
    	try {
    		verifySigkey(params);
	    	
	    	String channel = (String) params.get("channel");
	    	
	    	if (StringHelper.isNullOrEmptyString(channel)) {
	    		return new ResponseEntity<Object>(
	    				getErrorResponse(params), HttpStatus.BAD_REQUEST);
	    	}
	    	
	    	String savedFilePath = signalRecordingService.stopRecording(channel);
	    	if (savedFilePath != null) {
	    		
	    		Map<String, Object> res = getSuccessResponse(params);
		    	res.put("savedFilePath", savedFilePath);
		    	return new ResponseEntity<Object>(res, HttpStatus.OK);
	    	
	    	} else {
	    		Map<String, Object> res = getErrorResponse(params);
		    	res.put("errorMessage", "Failed to find channel data.");
		    	return new ResponseEntity<Object>(res, HttpStatus.NOT_FOUND);
	    	}
	    	
    	} catch (ForbiddenException e) {
    		throw e;
	    	
    	} catch (Exception e) {
    		log.error(e.getMessage(), e);
    		throw new ServerException();
    	}
    }
    
    /**
     * Show status of signaling and recording. (for debug)
     * @param params
     *  sigkey: signal key for verification
     * @return
     */
    @RequestMapping("/admin/recording/status")
    public ResponseEntity<Object> adminRecordingStatus(
    		@RequestBody Map<String, Object> params) {
    	
    	try {
    		verifySigkey(params);
	    	
	    	List<String> channelNames = new ArrayList<String>();
	    	for (SignalingAccount account : signalRecordingService.getUsedAccountMap().values()) {
	    		channelNames.add(account.channelName + 
	    				": status=" + account.recordingStatus + 
	    				" messages=" + account.groupMessageQueue.size());
	    	}
	    	
	    	int accountNum = signalRecordingService.getAccountQueue().size();
	    	
	    	Map<String, Object> res = getSuccessResponse(params);
	    	res.put("avaiable_account_num", accountNum);
	    	res.put("recording_channels", channelNames);
	    	
	    	return new ResponseEntity<Object>(res, HttpStatus.OK);
	    	
    	} catch (ForbiddenException e) {
    		throw e;
	    	
    	} catch (Exception e) {
    		log.error(e.getMessage(), e);
    		throw new ServerException();
    	}
    }
}

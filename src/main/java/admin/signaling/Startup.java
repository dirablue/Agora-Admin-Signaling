package admin.signaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import admin.signaling.service.SignalService;
import admin.signaling.service.SignalingRecordingService;

@Service
public class Startup {
	
	private static final Logger log = LoggerFactory.getLogger(Startup.class);
	
    @Autowired
    private SignalService signalService;
    
    @Autowired
    private SignalingRecordingService signalRecordingService;
    
    @Autowired
    private AppProperties appProp;
    
    /**
     * this is called when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReadyApp() {
    	
    	final String sep = System.lineSeparator();
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append(sep);
    	sb.append("------------------------------------------------" + sep);
    	sb.append("AppPropName=" + appProp.getName() + sep);
    	sb.append("AWS Region=" + appProp.getAwsRegion() + sep);
    	sb.append("AWS BucketName=" + appProp.getAwsBucketName() + sep);
    	sb.append("AWS RecordedChatDir=" + appProp.getAwsRecordedChatDir() + sep);
    	sb.append("------------------------------------------------");
    	log.info(sb.toString());
    	
    	signalService.loginAdminAccount();
    	signalRecordingService.createChannelAccounts(0);

    }
}

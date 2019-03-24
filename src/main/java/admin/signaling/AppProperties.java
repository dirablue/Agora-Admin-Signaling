package admin.signaling;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
	
	private String name;
	
	private String appId;
	private String sigKey;
	
	private String adminName;
	
	private String awsAccessKey;
	private String awsSecretKey;
	private String awsRegion;
	private String awsBucketName;
	private String awsRecordedChatDir;
	
	private int recordingTimeoutSec;
	private int recordingResumeSec;
	private int recordingFileKeepDays;
	private String recordingFileOutputDir;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAppId() {
		return appId;
	}
	
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	public String getSigKey() {
		return sigKey;
	}
	
	public void setSigKey(String sigKey) {
		this.sigKey = sigKey;
	}
	
	public String getAdminName() {
		return adminName;
	}
	
	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}
	
	public String getAwsRegion() {
		return awsRegion;
	}
	
	public void setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
	}
	
	public String getAwsAccessKey() {
		return awsAccessKey;
	}
	
	public void setAwsAccessKey(String awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}
	
	public String getAwsSecretKey() {
		return awsSecretKey;
	}
	
	public void setAwsSecretKey(String awsSecretKey) {
		this.awsSecretKey = awsSecretKey;
	}
	
	public String getAwsBucketName() {
		return awsBucketName;
	}
	
	public void setAwsBucketName(String awsBucketName) {
		this.awsBucketName = awsBucketName;
	}
	
	public String getAwsRecordedChatDir() {
		return awsRecordedChatDir;
	}
	
	public void setAwsRecordedChatDir(String awsRecordedChatDir) {
		this.awsRecordedChatDir = awsRecordedChatDir;
	}
	
	public int getRecordingTimeoutSec() {
		return recordingTimeoutSec;
	}
	
	public void setRecordingTimeoutSec(int recordingTimeoutSec) {
		this.recordingTimeoutSec = recordingTimeoutSec;
	}
	
	public int getRecordingResumeSec() {
		return recordingResumeSec;
	}
	
	public void setRecordingResumeSec(int recordingResumeSec) {
		this.recordingResumeSec = recordingResumeSec;
	}
	
	public int getRecordingFileKeepDays() {
		return recordingFileKeepDays;
	}
	
	public void setRecordingFileKeepDays(int recordingFileKeepDays) {
		this.recordingFileKeepDays = recordingFileKeepDays;
	}
	
	public String getRecordingFileOutputDir() {
		return recordingFileOutputDir;
	}
	
	public void setRecordingFileOutputDir(String recordingFileOutputDir) {
		this.recordingFileOutputDir = recordingFileOutputDir;
	}
}

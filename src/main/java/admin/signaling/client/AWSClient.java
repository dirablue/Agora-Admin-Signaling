package admin.signaling.client;

import java.io.File;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import admin.signaling.AppProperties;

@Service
public class AWSClient {

	private AmazonS3 s3client;
	
    @Autowired
    private AppProperties appProp;
	
	@PostConstruct
    private void initializeAmazon() {
       AWSCredentials credentials = new BasicAWSCredentials(
		   appProp.getAwsAccessKey(), 
		   appProp.getAwsSecretKey()
		   );
       
       String regiion = appProp.getAwsRegion();
       
       this.s3client = AmazonS3ClientBuilder.standard()
    		   .withRegion(regiion)
		       .withCredentials(new AWSStaticCredentialsProvider(credentials))
		       .build();
	}
	
	public String uploadFileToS3(String fileName, Path path) {
		File file = new File(path.toUri());
		return uploadFileToS3(fileName, file);
	}
	
	public String uploadFileToS3(String fileName, File file) {
		String saveFileName = appProp.getAwsRecordedChatDir() + "/" + fileName;
		PutObjectResult result = s3client.putObject(new PutObjectRequest(
    		appProp.getAwsBucketName(), 
    		saveFileName, 
    		file
    		)
            .withCannedAcl(CannedAccessControlList.PublicRead));
		if (result != null) {
			return saveFileName;
			
		} else {
			return null;
		}
	}
}


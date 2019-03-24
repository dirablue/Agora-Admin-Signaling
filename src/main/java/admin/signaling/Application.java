package admin.signaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class Application {
    
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	
    public static void main(String[] args) {
    	SpringApplication.run(Application.class, args);
//        ApplicationContext ctx = SpringApplication.run(Application.class, args);
    	
    	log.info("Admin Signaling is started");
        
//        String[] beanNames = ctx.getBeanDefinitionNames();
//        Arrays.sort(beanNames);
//        for (String beanName : beanNames) {
//            System.out.println(beanName);
//        }
        
//        Config config = (Config)ctx.getBean("config");
//        System.out.println("AppId=" + config.getAppId());
    }

}

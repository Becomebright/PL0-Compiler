package bright;

import bright.web.StorageService;
import bright.web.StorageProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class Application {

	public static void main(String[] args) throws IOException, URISyntaxException {
	    SpringApplication.run(Application.class, args);
    }

	@Bean
	CommandLineRunner init(StorageService storageService) {
	    System.out.println("<==========应用启动成功==========>");
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}
}
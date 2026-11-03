package cnu.ssd.pennywise;

import cnu.ssd.pennywise.controller.AuthController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

/**
 * Single entry point for the whole monolith: builds the Spring {@code ApplicationContext}
 * (wiring every Controller → Service → Repository → Entity bean and the PostgreSQL
 * {@code DataSource}), then hands off to the Swing UI on the Event Dispatch Thread.
 */
@SpringBootApplication
public class PennyWiseApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PennyWiseApplication.class);
		// Spring Boot sets java.awt.headless=true by default, which blocks Swing
		// from ever opening a window. This app has a GUI, so turn that back off.
		application.setHeadless(false);
		ConfigurableApplicationContext context = application.run(args);

		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
				// fall back to the default look and feel
			}
			AuthController authController = context.getBean(AuthController.class);
			authController.startApplication();
		});
	}
}

package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Created by bsollish on 8/8/17.
 */
public class AppYamlFeature implements Feature {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public AppYamlFeature() {}

  public boolean configure(FeatureContext context) {
    Configuration config = context.getConfiguration();
    if (!config.isRegistered(AppYamlMessageBodyProvider.class)) {
      logger.info("****************** AppYamlFeature: registering AppYamlMessageBodyProvider");

      context.register(AppYamlMessageBodyProvider.class);
    }

    return true;
  }
}

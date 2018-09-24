package software.wings.jersey;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Created by peeyushaggarwal on 1/13/17.
 */
public class KryoFeature implements Feature {
  public KryoFeature() {}

  public boolean configure(FeatureContext context) {
    Configuration config = context.getConfiguration();
    if (!config.isRegistered(KryoMessageBodyProvider.class)) {
      context.register(KryoMessageBodyProvider.class);
    }

    return true;
  }
}

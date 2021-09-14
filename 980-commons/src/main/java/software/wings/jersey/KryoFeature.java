/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.jersey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

@Singleton
public class KryoFeature implements Feature {
  @Inject KryoMessageBodyProvider kryoMessageBodyProvider;

  @Override
  public boolean configure(FeatureContext context) {
    Configuration config = context.getConfiguration();
    if (kryoMessageBodyProvider != null && !config.isRegistered(kryoMessageBodyProvider)) {
      context.register(kryoMessageBodyProvider);
    }

    return true;
  }
}

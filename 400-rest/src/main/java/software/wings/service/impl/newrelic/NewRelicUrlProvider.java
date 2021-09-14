/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.newrelic;

import software.wings.stencils.DataProvider;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Map;

/**
 * Created by raghu on 08/28/17.
 */
@Singleton
public class NewRelicUrlProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Collections.singletonMap("https://api.newrelic.com", "https://api.newrelic.com");
  }
}

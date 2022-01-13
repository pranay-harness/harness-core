/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

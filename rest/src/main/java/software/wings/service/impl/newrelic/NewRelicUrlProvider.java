package software.wings.service.impl.newrelic;

import com.google.inject.Singleton;

import software.wings.stencils.DataProvider;

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

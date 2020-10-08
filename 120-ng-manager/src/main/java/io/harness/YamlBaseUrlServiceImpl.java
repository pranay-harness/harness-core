package io.harness;

import com.google.inject.Inject;

import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.schema.YamlBaseUrlService;

public class YamlBaseUrlServiceImpl implements YamlBaseUrlService {
  @Inject NextGenConfiguration nextGenConfiguration;

  @Override
  public String getBaseUrl() {
    return nextGenConfiguration.getNgManagerClientConfig().getBaseUrl();
  }
}

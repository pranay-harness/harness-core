package io.harness.cvng.beans;

import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.impl.AppDynamicsCVConfigTransformer;
import io.harness.cvng.core.services.impl.SplunkCVConfigTransformer;

public enum DataSourceType {
  APP_DYNAMICS(AppDynamicsCVConfigTransformer.class),
  SPLUNK(SplunkCVConfigTransformer.class);
  private Class<? extends CVConfigTransformer> cvConfigMapperClass;
  DataSourceType(Class<? extends CVConfigTransformer> cvConfigMapperClass) {
    this.cvConfigMapperClass = cvConfigMapperClass;
  }

  public Class<? extends CVConfigTransformer> getCvConfigMapperClass() {
    return cvConfigMapperClass;
  }
}

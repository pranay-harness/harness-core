package io.harness.cvng.beans;

import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.AppDynamicsCVConfigTransformer;
import io.harness.cvng.core.services.impl.AppDynamicsDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.SplunkCVConfigTransformer;

public enum DataSourceType {
  APP_DYNAMICS(AppDynamicsCVConfigTransformer.class, AppDynamicsDataCollectionInfoMapper.class),
  SPLUNK(SplunkCVConfigTransformer.class, null);
  private Class<? extends CVConfigTransformer> cvConfigMapperClass;
  private Class<? extends DataCollectionInfoMapper> dataCollectionInfoMapperClass;
  DataSourceType(Class<? extends CVConfigTransformer> cvConfigMapperClass,
      Class<? extends DataCollectionInfoMapper> dataCollectionInfoMapperClass) {
    this.cvConfigMapperClass = cvConfigMapperClass;
    this.dataCollectionInfoMapperClass = dataCollectionInfoMapperClass;
  }

  public Class<? extends CVConfigTransformer> getCvConfigMapperClass() {
    return cvConfigMapperClass;
  }

  public Class<? extends DataCollectionInfoMapper> getDataCollectionInfoMapperClass() {
    return dataCollectionInfoMapperClass;
  }
}

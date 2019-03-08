package io.harness.delegate.beans.executioncapability;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CapabilityCheckGeneratorFactory {
  @Inject private HttpCapabilityGenerator httpConnectionCheckGenerator;

  public CapabilityGenerator obtainCapabilityCheckGenerator(String taskType) {
    if ("HTTP".equals(taskType)) {
      return httpConnectionCheckGenerator;
    }

    return null;
  }
}

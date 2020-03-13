package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.pricing.data.CloudProvider;

public interface InstanceResourceService {
  Resource getComputeVMResource(String instanceType, String region, CloudProvider cloudProvider);
}

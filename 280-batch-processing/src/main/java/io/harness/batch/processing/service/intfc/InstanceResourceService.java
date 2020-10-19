package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.ccm.commons.beans.Resource;

public interface InstanceResourceService {
  Resource getComputeVMResource(String instanceType, String region, CloudProvider cloudProvider);
}

package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.constants.CloudProvider;

public interface InstanceResourceService {
  Resource getComputeVMResource(String instanceType, String region, CloudProvider cloudProvider);
}

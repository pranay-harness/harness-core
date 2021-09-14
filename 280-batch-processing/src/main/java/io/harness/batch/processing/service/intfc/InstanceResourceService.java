/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.constants.CloudProvider;

public interface InstanceResourceService {
  Resource getComputeVMResource(String instanceType, String region, CloudProvider cloudProvider);
}

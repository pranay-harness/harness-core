/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.instance.instanceInfo;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.schema.type.instance.QLInstance;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface InstanceController<T extends QLInstance> {
  T populateInstance(Instance instance);
}

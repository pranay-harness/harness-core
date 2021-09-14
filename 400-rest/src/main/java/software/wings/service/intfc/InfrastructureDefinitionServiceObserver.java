/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import software.wings.infra.InfrastructureDefinition;

public interface InfrastructureDefinitionServiceObserver {
  void onSaved(InfrastructureDefinition infrastructureDefinition);
  void onUpdated(InfrastructureDefinition infrastructureDefinition);
}

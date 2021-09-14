/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@OwnedBy(PL)
public interface Resource {
  String getType();

  Set<ScopeLevel> getValidScopeLevels();

  Optional<String> getEventFrameworkEntityType();

  ResourceInfo getResourceInfoFromEvent(Message message);

  List<Boolean> validate(List<String> resourceIds, Scope scope);

  EnumSet<ValidatorType> getSelectorKind();
}

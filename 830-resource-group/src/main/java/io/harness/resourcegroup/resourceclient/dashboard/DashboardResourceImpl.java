/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.resourcegroup.resourceclient.dashboard;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class DashboardResourceImpl implements Resource {
  public static final String DASHBOARD = "DASHBOARDS";

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    return resourceIds.stream().map(resourceId -> true).collect(Collectors.toList());
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(STATIC, DYNAMIC);
  }

  @Override
  public String getType() {
    return DASHBOARD;
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.ACCOUNT, ScopeLevel.ORGANIZATION);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.empty();
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    return null;
  }
}

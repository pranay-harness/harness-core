/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLogTaskMetadata;

import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateInsightsSummary;
import software.wings.beans.DelegatePerpetualTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.Set;

@OwnedBy(DEL)
public class DelegateServiceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Delegate.class);
    set.add(DelegateConnection.class);
    set.add(DelegateSelectionLog.class);
    set.add(DelegateSelectionLogTaskMetadata.class);
    set.add(DelegateConnectionResult.class);
    set.add(DelegateGroup.class);
    set.add(DelegateInsightsSummary.class);
    set.add(DelegateTaskUsageInsights.class);
    set.add(DelegatePerpetualTaskUsageInsights.class);
    set.add(PerpetualTaskRecord.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}

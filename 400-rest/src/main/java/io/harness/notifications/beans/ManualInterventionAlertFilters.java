/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notifications.beans;

import io.harness.data.structure.CollectionUtils;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class ManualInterventionAlertFilters implements io.harness.notifications.conditions.ManualInterventionFilters {
  private List<String> appIds;
  private List<String> envIds;

  @Override
  @Nonnull
  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  @Override
  @Nonnull
  public List<String> getEnvIds() {
    return CollectionUtils.emptyIfNull(envIds);
  }
}

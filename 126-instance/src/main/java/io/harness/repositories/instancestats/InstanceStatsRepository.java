/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.instancestats;

import io.harness.models.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId, String orgId, String projectId, String serviceId) throws Exception;
}

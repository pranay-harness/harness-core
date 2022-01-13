/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.dao.InstanceDataDao;
import io.harness.ccm.commons.entities.batch.InstanceData;
import io.harness.ccm.commons.service.intf.InstanceDataService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CE)
public class InstanceDataServiceImpl implements InstanceDataService {
  @Inject InstanceDataDao instanceDataDao;

  @Override
  public InstanceData get(String instanceId) {
    return instanceDataDao.get(instanceId);
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(List<String> instanceIds) {
    return instanceDataDao.fetchInstanceDataForGivenInstances(instanceIds);
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    return instanceDataDao.fetchInstanceDataForGivenInstances(accountId, clusterId, instanceIds);
  }
}

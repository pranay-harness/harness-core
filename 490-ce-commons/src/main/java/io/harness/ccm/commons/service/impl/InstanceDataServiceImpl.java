/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

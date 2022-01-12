/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.perpetualtask;

import io.harness.cvng.beans.CVNGPerpetualTaskDTO;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;

import io.kubernetes.client.openapi.ApiException;
import java.util.List;

public interface CVDataCollectionTaskService {
  void resetTask(String accountId, String orgIdentifier, String projectIdentifier, String taskId,
      DataCollectionConnectorBundle bundle);

  String create(String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle);
  void delete(String accountId, String taskId);

  CVNGPerpetualTaskDTO getCVNGPerpetualTaskDTO(String taskId);

  String getDataCollectionResult(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionRequest dataCollectionRequest);

  List<String> getNamespaces(String accountId, String orgIdentifier, String projectIdentifier, String filter,
      DataCollectionConnectorBundle bundle) throws ApiException;
  List<String> getWorkloads(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      String filter, DataCollectionConnectorBundle bundle) throws ApiException;
  List<String> checkCapabilityToGetEvents(String accountId, String orgIdentifier, String projectIdentifier,
      DataCollectionConnectorBundle bundle) throws ApiException;
}

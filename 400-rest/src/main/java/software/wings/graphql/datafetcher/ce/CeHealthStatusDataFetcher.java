/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;

import software.wings.graphql.schema.type.aggregation.cloudprovider.CEHealthStatusDTO;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;

import com.google.inject.Inject;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CeHealthStatusDataFetcher implements DataFetcher<CEHealthStatusDTO> {
  @Inject HealthStatusService healthStatusService;

  public DataFetcher get() {
    return dataFetchingEnvironment -> get(dataFetchingEnvironment);
  }

  @Override
  public CEHealthStatusDTO get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    QLCloudProvider cloudProvider = dataFetchingEnvironment.getSource();
    String cloudProviderId = cloudProvider.getId();

    try {
      CEHealthStatus ceHealthStatus = healthStatusService.getHealthStatus(cloudProviderId, false);
      return CEHealthStatusDTO.builder()
          .isHealthy(ceHealthStatus.isHealthy())
          .messages(ceHealthStatus.getMessages())
          .clusterHealthStatusList(ceHealthStatus.getClusterHealthStatusList())
          .build();
    } catch (IllegalArgumentException e) {
      log.error("Exception getting health status", e);
    }
    return null;
  }
}

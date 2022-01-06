/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.anomaly.mappers;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.anomaly.entities.AnomalyEntity;

import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QlAnomalyMapper {
  public QLAnomalyData toDto(AnomalyEntity source) {
    QLAnomalyData returnDTO = new QLAnomalyData();
    returnDTO.setEntity(new QLEntityInfo());
    returnDTO.getEntity().setClusterId(source.getClusterId());
    returnDTO.getEntity().setClusterName(source.getClusterName());
    returnDTO.getEntity().setNamespace(source.getNamespace());
    returnDTO.getEntity().setWorkloadName(source.getWorkloadName());
    returnDTO.getEntity().setWorkloadType(source.getWorkloadType());
    returnDTO.getEntity().setAwsAccount(source.getAwsAccount());
    returnDTO.getEntity().setAwsService(source.getAwsService());
    returnDTO.getEntity().setGcpProject(source.getGcpProject());
    returnDTO.getEntity().setGcpProduct(source.getGcpProduct());
    returnDTO.getEntity().setGcpSKUId(source.getGcpSKUId());
    returnDTO.getEntity().setGcpSKUDescription(source.getGcpSKUDescription());
    returnDTO.setComment(source.getNote());
    returnDTO.setActualAmount(source.getActualCost());
    returnDTO.setExpectedAmount(source.getExpectedCost());
    returnDTO.setAnomalyScore(source.getAnomalyScore());
    returnDTO.setId(source.getId());
    if (source.getAnomalyTime() != null) {
      returnDTO.setTime(source.getAnomalyTime().toEpochMilli());
    }
    returnDTO.setUserFeedback(source.getFeedback());
    return returnDTO;
  }
}

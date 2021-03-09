package io.harness.ccm.anomaly.mappers;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import lombok.experimental.UtilityClass;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo;

@UtilityClass
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
    returnDTO.setTime(source.getAnomalyTime().toEpochMilli());
    returnDTO.setUserFeedback(source.getFeedback());
    return returnDTO;
  }
}

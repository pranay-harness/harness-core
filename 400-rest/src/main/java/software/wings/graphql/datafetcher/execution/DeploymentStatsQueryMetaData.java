/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.execution;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortCriteria;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeploymentStatsQueryMetaData {
  private static final DeploymentTableSchema schema = new DeploymentTableSchema();

  enum DataType { STRING, INTEGER, TIMESTAMP, LONG, HSTORE }

  public enum DeploymentMetaDataFields {
    TIME_SERIES("TIME_BUCKET", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    COUNT("COUNT", DataType.INTEGER, QLFilterKind.SIMPLE),
    DURATION("DURATION", DataType.LONG, QLFilterKind.SIMPLE),
    APPID("APPID", DataType.STRING, QLFilterKind.SIMPLE),
    STATUS("STATUS", DataType.STRING, QLFilterKind.SIMPLE),
    TRIGGER_ID("TRIGGER_ID", DataType.STRING, QLFilterKind.SIMPLE),
    TRIGGERED_BY("TRIGGERED_BY", DataType.STRING, QLFilterKind.SIMPLE),
    PIPELINEID("PIPELINE", DataType.STRING, QLFilterKind.SIMPLE),
    SERVICEID("SERVICEID", DataType.STRING, QLFilterKind.ARRAY),
    ENVID("ENVID", DataType.STRING, QLFilterKind.ARRAY),
    CLOUDPROVIDERID("CLOUDPROVIDERID", DataType.STRING, QLFilterKind.ARRAY),
    WORKFLOWID("WORKFLOWID", DataType.STRING, QLFilterKind.ARRAY),
    ENVTYPE("ENVTYPE", DataType.STRING, QLFilterKind.ARRAY),
    STARTTIME("STARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    ENDTIME("ENDTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    ROLLBACK_DURATION("ROLLBACK_DURATION", DataType.LONG, QLFilterKind.SIMPLE),
    INSTANCES_DEPLOYED("INSTANCES_DEPLOYED", DataType.INTEGER, QLFilterKind.SIMPLE),
    TAGS("TAGS", DataType.HSTORE, QLFilterKind.HSTORE),
    DEPLOYMENT_TYPE("DEPLOYMENT_TYPE", DataType.STRING, QLFilterKind.ARRAY),
    WORKFLOW_TYPE("WORKFLOW_TYPE", DataType.STRING, QLFilterKind.ARRAY),
    ORCHESTRATION_WORKFLOW_TYPE("ORCHESTRATION_WORKFLOW_TYPE", DataType.STRING, QLFilterKind.ARRAY),
    ROLLBACK_COUNT("ROLLBACK_COUNT", DataType.LONG, QLFilterKind.SIMPLE);

    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    DeploymentMetaDataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
      this.fieldName = fieldName;
      this.dataType = dataType;
      this.filterKind = filterKind;
    }

    public DataType getDataType() {
      return dataType;
    }

    public QLFilterKind getFilterKind() {
      return filterKind;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public enum ResultType { SINGLE_POINT, AGGREGATE_DATA, TIME_SERIES, STACKED_TIME_SERIES, STACKED_BAR_CHART }

  List<DeploymentMetaDataFields> groupByFields;

  private ResultType resultType;

  private List<DeploymentMetaDataFields> fieldNames;

  private List<QLDeploymentSortCriteria> sortCriteria;

  private String query;

  List<QLDeploymentFilter> filters;
}

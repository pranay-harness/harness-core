package software.wings.graphql.datafetcher.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortCriteria;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    TAGS("TAGS", DataType.HSTORE, QLFilterKind.HSTORE);

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

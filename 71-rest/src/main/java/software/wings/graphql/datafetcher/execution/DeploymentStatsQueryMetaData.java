package software.wings.graphql.datafetcher.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentStatsQueryMetaData {
  private static final DeploymentTableSchema schema = new DeploymentTableSchema();

  enum DataType { STRING, INTEGER, TIMESTAMP, LONG }

  enum DeploymentMetaDataFields {
    TIME_SERIES("TIME_BUCKET", DataType.TIMESTAMP),
    COUNT("COUNT", DataType.INTEGER),
    DURATION("DURATION", DataType.LONG),
    APPID("APPID", DataType.STRING),
    STATUS("STATUS", DataType.STRING),
    TRIGGERID("TRIGGERID", DataType.STRING),
    TRIGGEREDBY("TRIGGERED_BY", DataType.STRING),
    PIPELINEID("PIPELINE", DataType.STRING),
    SERVICEID("SERVICEID", DataType.STRING),
    ENVID("ENVID", DataType.STRING),
    CLOUDPROVIDERID("CLOUDPROVIDERID", DataType.STRING),
    WORKFLOWID("WORKFLOWID", DataType.STRING);

    private DataType dataType;
    private String fieldName;

    DeploymentMetaDataFields(String fieldName, DataType dataType) {
      this.fieldName = fieldName;
      this.dataType = dataType;
    }

    public DataType getDataType() {
      return dataType;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public enum ResultType { SINGLE_POINT, AGGREGATE_DATA, TIME_SERIES, STACKED_TIME_SERIES, STACKED_BAR_CHART }

  List<DeploymentMetaDataFields> groupByFields;

  private ResultType resultType;

  private List<DeploymentMetaDataFields> fieldNames;

  private String query;
}

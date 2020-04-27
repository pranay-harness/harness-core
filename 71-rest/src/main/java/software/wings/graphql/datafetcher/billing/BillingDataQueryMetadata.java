package software.wings.graphql.datafetcher.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDataQueryMetadata {
  private static final BillingDataTableSchema schema = new BillingDataTableSchema();

  enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE }

  public enum BillingDataMetaDataFields {
    TIME_SERIES("STARTTIMEBUCKET", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    STARTTIME("STARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    MIN_STARTTIME("MINSTARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    MAX_STARTTIME("MAXSTARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    SUM("COST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    APPID("APPID", DataType.STRING, QLFilterKind.SIMPLE),
    REGION("REGION", DataType.STRING, QLFilterKind.SIMPLE),
    SERVICEID("SERVICEID", DataType.STRING, QLFilterKind.SIMPLE),
    CLUSTERID("CLUSTERID", DataType.STRING, QLFilterKind.SIMPLE),
    CLUSTERNAME("CLUSTERNAME", DataType.STRING, QLFilterKind.SIMPLE),
    ENVID("ENVID", DataType.STRING, QLFilterKind.SIMPLE),
    CLOUDSERVICENAME("CLOUDSERVICENAME", DataType.STRING, QLFilterKind.SIMPLE),
    TASKID("TASKID", DataType.STRING, QLFilterKind.SIMPLE),
    INSTANCETYPE("INSTANCETYPE", DataType.STRING, QLFilterKind.SIMPLE),
    PARENTINSTANCEID("PARENTINSTANCEID", DataType.STRING, QLFilterKind.SIMPLE),
    INSTANCEID("INSTANCEID", DataType.STRING, QLFilterKind.SIMPLE),
    LAUNCHTYPE("LAUNCHTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADNAME("WORKLOADNAME", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADTYPE("WORKLOADTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    NAMESPACE("NAMESPACE", DataType.STRING, QLFilterKind.SIMPLE),
    IDLECOST("ACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CPUIDLECOST("CPUACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MEMORYIDLECOST("MEMORYACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CLUSTERTYPE("CLUSTERTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    TOTALWORKLOADS("TOTALWORKLOADS", DataType.INTEGER, QLFilterKind.SIMPLE),
    TOTALNAMESPACES("TOTALNAMESPACES", DataType.INTEGER, QLFilterKind.SIMPLE),
    MAXCPUUTILIZATION("MAXCPUUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MAXMEMORYUTILIZATION("MAXMEMORYUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AVGCPUUTILIZATION("AVGCPUUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AVGMEMORYUTILIZATION("AVGMEMORYUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CLOUDPROVIDERID("CLOUDPROVIDERID", DataType.STRING, QLFilterKind.SIMPLE),
    CPUBILLINGAMOUNT("CPUBILLINGAMOUNT", DataType.STRING, QLFilterKind.SIMPLE),
    MEMORYBILLINGAMOUNT("MEMORYBILLINGAMOUNT", DataType.STRING, QLFilterKind.SIMPLE),
    UNALLOCATEDCOST("UNALLOCATEDCOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    SYSTEMCOST("SYSTEMCOST", DataType.DOUBLE, QLFilterKind.SIMPLE);
    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    BillingDataMetaDataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
      this.fieldName = fieldName;
      this.dataType = dataType;
      this.filterKind = filterKind;
    }

    public QLFilterKind getFilterKind() {
      return filterKind;
    }

    public DataType getDataType() {
      return dataType;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public enum ResultType { STACKED_TIME_SERIES, NODE_AND_POD_DETAILS }

  List<BillingDataMetaDataFields> groupByFields;

  private ResultType resultType;

  private List<BillingDataMetaDataFields> fieldNames;

  private List<QLBillingSortCriteria> sortCriteria;

  private String query;

  List<QLBillingDataFilter> filters;
}

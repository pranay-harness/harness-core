package software.wings.graphql.datafetcher.ce.activePods;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.graphql.datafetcher.budget.BudgetAlertsTableSchema;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CeActivePodCountQueryMetadata {
  private static final BudgetAlertsTableSchema schema = new BudgetAlertsTableSchema();
  enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE }

  public enum CeActivePodCountMetaDataFields {
    STARTTIME("STARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    ENDTIME("ENDTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    ACCOUNTID("ACCOUNTID", DataType.STRING, QLFilterKind.SIMPLE),
    CLUSTERID("CLUSTERID", DataType.STRING, QLFilterKind.SIMPLE),
    INSTANCEID("INSTANCEID", DataType.STRING, QLFilterKind.SIMPLE),
    PODCOUNT("PODCOUNT", DataType.DOUBLE, QLFilterKind.SIMPLE);

    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    CeActivePodCountMetaDataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
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

  List<CeActivePodCountQueryMetadata> groupByFields;

  private List<CeActivePodCountMetaDataFields> fieldNames;

  private List<QLBillingSortCriteria> sortCriteria;

  private String query;

  List<QLBillingDataFilter> filters;
}

package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CEEventsQueryMetaData {
  private static final CEEventsTableSchema schema = new CEEventsTableSchema();
  enum DataType { STRING, TIMESTAMP, DOUBLE }
  public enum CEEventsMetaDataFields {
    STARTTIME("STARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    CLUSTERID("CLUSTERID", DataType.STRING, QLFilterKind.SIMPLE),
    CLUSTERTYPE("CLUSTERTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    INSTANCETYPE("INSTANCETYPE", DataType.STRING, QLFilterKind.SIMPLE),
    APPID("APPID", DataType.STRING, QLFilterKind.SIMPLE),
    SERVICEID("SERVICEID", DataType.STRING, QLFilterKind.SIMPLE),
    ENVID("ENVID", DataType.STRING, QLFilterKind.SIMPLE),
    CLOUDSERVICENAME("CLOUDSERVICENAME", DataType.STRING, QLFilterKind.SIMPLE),
    TASKID("TASKID", DataType.STRING, QLFilterKind.SIMPLE),
    LAUNCHTYPE("LAUNCHTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADNAME("WORKLOADNAME", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADTYPE("WORKLOADTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    NAMESPACE("NAMESPACE", DataType.STRING, QLFilterKind.SIMPLE),
    EVENTDESCRIPTION("EVENTDESCRIPTION", DataType.STRING, QLFilterKind.SIMPLE),
    COSTEVENTTYPE("COSTEVENTTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    COSTEVENTSOURCE("COSTEVENTSOURCE", DataType.STRING, QLFilterKind.SIMPLE),
    OLDYAMLREF("OLDYAMLREF", DataType.STRING, QLFilterKind.SIMPLE),
    NEWYAMLREF("NEWYAMLREF", DataType.STRING, QLFilterKind.SIMPLE),
    COST_CHANGE_PERCENT("COST_CHANGE_PERCENT", DataType.DOUBLE, QLFilterKind.SIMPLE);

    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    CEEventsMetaDataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
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

  private List<CEEventsMetaDataFields> fieldNames;

  private List<QLEventsSortCriteria> sortCriteria;

  private String query;

  List<QLEventsDataFilter> filters;
}

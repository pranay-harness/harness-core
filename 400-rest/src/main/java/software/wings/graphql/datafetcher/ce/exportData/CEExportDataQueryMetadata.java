/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEFilter;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESort;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CEExportDataQueryMetadata {
  private static final CEExportDataTableSchema schema = new CEExportDataTableSchema();
  enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE }

  public enum CEExportDataMetadataFields {
    TIME_SERIES("STARTTIMEBUCKET", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    STARTTIME("STARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    MIN_STARTTIME("MINSTARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    MAX_STARTTIME("MAXSTARTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    SUM("COST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    TOTALCOST("BILLINGAMOUNT", DataType.DOUBLE, QLFilterKind.SIMPLE),
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
    INSTANCENAME("INSTANCENAME", DataType.STRING, QLFilterKind.SIMPLE),
    LAUNCHTYPE("LAUNCHTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADNAME("WORKLOADNAME", DataType.STRING, QLFilterKind.SIMPLE),
    WORKLOADTYPE("WORKLOADTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    NAMESPACE("NAMESPACE", DataType.STRING, QLFilterKind.SIMPLE),
    IDLECOST("ACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CPUIDLECOST("CPUACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MEMORYIDLECOST("MEMORYACTUALIDLECOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CLUSTERTYPE("CLUSTERTYPE", DataType.STRING, QLFilterKind.SIMPLE),
    CPUBILLINGAMOUNT("CPUBILLINGAMOUNT", DataType.STRING, QLFilterKind.SIMPLE),
    MEMORYBILLINGAMOUNT("MEMORYBILLINGAMOUNT", DataType.STRING, QLFilterKind.SIMPLE),
    UNALLOCATEDCOST("UNALLOCATEDCOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    SYSTEMCOST("SYSTEMCOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CPUREQUEST("CPUREQUEST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MEMORYREQUEST("MEMORYREQUEST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MAXCPUUTILIZATION("MAXCPUUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MAXMEMORYUTILIZATION("MAXMEMORYUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AVGCPUUTILIZATION("AVGCPUUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AVGMEMORYUTILIZATION("AVGMEMORYUTILIZATION", DataType.DOUBLE, QLFilterKind.SIMPLE),
    CPULIMIT("CPULIMIT", DataType.DOUBLE, QLFilterKind.SIMPLE),
    MEMORYLIMIT("MEMORYLIMIT", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDCPUUTILIZATIONVALUE("AGGREGATEDCPUUTILIZATIONVALUE", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDMEMORYUTILIZATIONVALUE("AGGREGATEDMEMORYUTILIZATIONVALUE", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDCPUREQUEST("AGGREGATEDCPUREQUEST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDMEMORYREQUEST("AGGREGATEDMEMORYREQUEST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDCPULIMIT("AGGREGATEDCPULIMIT", DataType.DOUBLE, QLFilterKind.SIMPLE),
    AGGREGATEDMEMORYLIMIT("AGGREGATEDMEMORYLIMIT", DataType.DOUBLE, QLFilterKind.SIMPLE);
    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    CEExportDataMetadataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
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

  List<CEExportDataMetadataFields> groupByFields;

  private List<CEExportDataMetadataFields> fieldNames;

  private List<QLCESort> sortCriteria;

  private String query;

  List<QLCEFilter> filters;
}

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLCEUtilization {
  CPU_REQUEST("cpurequest"),
  CPU_LIMIT("cpulimit"),
  CPU_UTILIZATION_VALUE("cpuutilizationnvalue"),
  MEMORY_REQUEST("memoryrequest"),
  MEMORY_LIMIT("memorylimit"),
  MEMORY_UTILIZATION_VALUE("memoryutilizationvalue"),
  STORAGE_REQUEST("storagerequest"),
  STORAGE_UTILIZATION_VALUE("storageutilizationvalue");

  private final String name;

  QLCEUtilization(String s) {
    name = s;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }

  public String toString() {
    return this.name;
  }
}

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.appmanifest.ApplicationManifestSummary;
import software.wings.beans.appmanifest.LastDeployedHelmChartInformation;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManifestVariable extends Variable {
  private List<String> workflowIds;
  private String serviceId;
  private String serviceName;
  private List<ApplicationManifestSummary> applicationManifestSummary;
  private LastDeployedHelmChartInformation lastDeployedHelmChartInfo;

  @Builder
  public ManifestVariable(String name, String description, boolean mandatory, String value, boolean fixed,
      String allowedValues, List<String> allowedList, Map<String, Object> metadata, VariableType type, String serviceId,
      List<ApplicationManifestSummary> applicationManifestSummary, String serviceName, List<String> workflowIds,
      LastDeployedHelmChartInformation lastDeployedHelmChartInformation) {
    super(name, description, mandatory, value, fixed, allowedValues, allowedList, metadata, type);
    this.serviceId = serviceId;
    this.applicationManifestSummary = applicationManifestSummary;
    this.serviceName = serviceName;
    this.workflowIds = workflowIds;
    this.lastDeployedHelmChartInfo = lastDeployedHelmChartInformation;
  }
}

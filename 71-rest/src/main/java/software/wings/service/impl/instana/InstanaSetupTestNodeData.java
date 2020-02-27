package software.wings.service.impl.instana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstanaSetupTestNodeData extends SetupTestNodeData {
  private InstanaInfraParams infraParams;
  private InstanaApplicationParams applicationParams;
  private List<InstanaTagFilter> tagFilters;
  @Builder
  private InstanaSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String guid, InstanaInfraParams infraParams, InstanaApplicationParams applicationParams,
      List<InstanaTagFilter> tagFilters) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.INSTANA, fromTime, toTime);
    this.infraParams = infraParams;
    this.applicationParams = applicationParams;
    this.tagFilters = tagFilters;
  }

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.tagFilters);
  }
}

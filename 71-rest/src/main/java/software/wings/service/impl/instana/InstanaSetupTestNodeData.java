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
  private List<String> metrics;
  private String query;
  private List<InstanaTagFilter> tagFilters;
  private String hostTagFilter;
  @Builder
  private InstanaSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String guid, List<String> metrics, String query, List<InstanaTagFilter> tagFilters, String hostTagFilter) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.INSTANA, fromTime, toTime);
    this.metrics = metrics;
    this.query = query;
    this.tagFilters = tagFilters;
    this.hostTagFilter = hostTagFilter;
  }

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.tagFilters);
  }
}

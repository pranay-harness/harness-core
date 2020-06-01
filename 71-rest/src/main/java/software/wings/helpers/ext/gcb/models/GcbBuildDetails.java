package software.wings.helpers.ext.gcb.models;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Data
public class GcbBuildDetails {
  private String id;
  private String projectId;
  private GcbBuildStatus status;
  private String statusDetail;
  private GcbBuildSource source;
  private List<BuildStep> steps;
  private GcbResult results;
  private String createTime;
  private String startTime;
  private String finishTime;
  private String timeout;
  private List<String> images;
  private String queueTtl;
  private GcbArtifacts artifacts;
  private String logsBucket;
  private SourceProvenance sourceProvenance;
  private String buildTriggerId;
  private BuildOptions options;
  private String logUrl;
  private Map<String, String> substitutions;
  private List<String> tags;
  private List<String> secrets;
  private Map<String, TimeSpan> timing;
}

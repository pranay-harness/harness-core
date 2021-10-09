package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 11/20/17.
 */
@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class BuildExecutionSummary {
  String artifactStreamId;
  String artifactSource;
  String revision;
  String buildUrl;
  String buildName;
  String metadata;
}

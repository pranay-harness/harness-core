package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 11/20/17.
 */
@Data
@Builder
public class BuildExecutionSummary {
  String artifactStreamId;
  String artifactSource;
  String revision;
  String buildUrl;
  String buildName;
  String metadata;
}

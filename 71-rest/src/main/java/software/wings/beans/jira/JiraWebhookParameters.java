package software.wings.beans.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
public class JiraWebhookParameters {
  private String name;
  private String url;
  private List<String> events;
  private Map<String, String> filters;
  private Boolean excludeBody;
  private Map<String, String> jqlFilter;
  private Boolean excludeIssueDetails;
}

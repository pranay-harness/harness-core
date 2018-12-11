package software.wings.beans.jira;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

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

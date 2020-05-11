package software.wings.service.impl.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.jira.JiraCustomFieldValue;
import software.wings.delegatetasks.jira.JiraAction;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by Pranjal on 05/13/2019
 */
@OwnedBy(CDC)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JiraConfiguration {
  @NotNull private JiraAction jiraAction;
  @NotNull String jiraConnectorId;
  @NotNull private String project;
  private String issueType;
  private String priority;
  private List<String> labels;
  private String summary;
  private String description;
  private String status;
  private String comment;
  private String issueId;
  private Map<String, JiraCustomFieldValue> customFields;
}

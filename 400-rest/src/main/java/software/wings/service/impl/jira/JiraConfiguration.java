/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCustomFieldValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 05/13/2019
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
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

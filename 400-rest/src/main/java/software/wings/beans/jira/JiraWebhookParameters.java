/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

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

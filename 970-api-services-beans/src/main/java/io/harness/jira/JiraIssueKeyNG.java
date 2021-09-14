/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueKeyNG {
  @NotNull String url;
  @NotNull String key;

  public JiraIssueKeyNG(@NotEmpty String baseUrl, @NotEmpty String key) {
    this.url = JiraIssueUtilsNG.prepareIssueUrl(baseUrl, key);
    this.key = key;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Collections.singletonMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraCreateIssueRequestNG {
  @NotNull Map<String, Object> fields = new HashMap<>();

  public JiraCreateIssueRequestNG(JiraProjectNG project, JiraIssueTypeNG issueType, Map<String, String> fields) {
    // Add project and issue type fields which are required and are not part of fields. We don't need special handling
    // for status field as we manually remove status field from issueType.fields before calling this method.
    this.fields.put(JiraConstantsNG.PROJECT_KEY, singletonMap("key", project.getKey()));
    this.fields.put(JiraConstantsNG.ISSUE_TYPE_KEY, singletonMap("id", issueType.getId()));
    if (EmptyPredicate.isEmpty(fields)) {
      return;
    }

    fields = new HashMap<>(fields);
    JiraIssueUtilsNG.updateFieldValues(this.fields, issueType.getFields(), fields, true);
  }
}

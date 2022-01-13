/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import io.harness.jira.JiraCustomFieldValue;

import software.wings.beans.GraphNode;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JiraUtils {
  public static GraphNode getJiraCreateNodeWithoutCustomFields(String jiraConnectorId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(JIRA_CREATE_UPDATE.name())
        .name("Create Jira")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("description", "test123")
                        .put("issueType", "Story")
                        .put("jiraAction", "CREATE_TICKET")
                        .put("jiraConnectorId", jiraConnectorId)
                        .put("priority", "P1")
                        .put("project", "TJI")
                        .put("publishAsVar", true)
                        .put("summary", "test")
                        .put("sweepingOutputName", "Jiravar")
                        .put("sweepingOutputScope", "PIPELINE")
                        .put("labels", Collections.singletonList("demo"))
                        .build())
        .build();
  }

  public static GraphNode getJiraCreateNodeWithCustomFields(String jiraConnectorId) {
    Map<String, Object> customFields = new HashMap<>();
    JiraCustomFieldValue component = new JiraCustomFieldValue("multiselect", "10333");
    customFields.put("components", component);

    JiraCustomFieldValue customfield_10006 = new JiraCustomFieldValue("any", "TJI-125");
    customFields.put("customfield_10006", customfield_10006);

    JiraCustomFieldValue customfield_10644 = new JiraCustomFieldValue("string", "12354");
    customFields.put("customfield_10644", customfield_10644);

    JiraCustomFieldValue customfield_10105 = new JiraCustomFieldValue("number", "2");
    customFields.put("customfield_10105", customfield_10105);

    JiraCustomFieldValue customfield_10666 = new JiraCustomFieldValue("option", "10335");
    customFields.put("customfield_10666", customfield_10666);

    JiraCustomFieldValue customfield_10747 = new JiraCustomFieldValue("datetime", "1579077000000");
    customFields.put("customfield_10747", customfield_10747);

    JiraCustomFieldValue duedate = new JiraCustomFieldValue("date", "2019-12-31");
    customFields.put("duedate", duedate);

    return GraphNode.builder()
        .id(generateUuid())
        .type(JIRA_CREATE_UPDATE.name())
        .name("Create Jira")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("description", "test123")
                        .put("issueType", "Story")
                        .put("jiraAction", "CREATE_TICKET")
                        .put("jiraConnectorId", jiraConnectorId)
                        .put("priority", "P1")
                        .put("project", "TJI")
                        .put("summary", "test")
                        .put("sweepingOutputName", "Jiravar")
                        .put("sweepingOutputScope", "PIPELINE")
                        .put("labels", Collections.singletonList("demo"))
                        .put("customFields", customFields)
                        .build())
        .build();
  }
}

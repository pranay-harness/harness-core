package io.harness.testframework.framework.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.GraphNode;

import java.util.Collections;

public class JiraUtils {
  public static GraphNode getJiraCreateNode(String jiraConnectorId) {
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
}

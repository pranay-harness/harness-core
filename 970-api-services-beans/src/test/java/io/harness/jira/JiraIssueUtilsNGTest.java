package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class JiraIssueUtilsNGTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testSplitByComma() {
    assertThat(JiraIssueUtilsNG.splitByComma(null)).isEmpty();
    assertThat(JiraIssueUtilsNG.splitByComma("  ")).isEmpty();
    assertThat(JiraIssueUtilsNG.splitByComma("abc")).containsExactly("abc");
    assertThat(JiraIssueUtilsNG.splitByComma("abc,  def")).containsExactly("abc", "def");
    assertThat(JiraIssueUtilsNG.splitByComma("abc,  def, \"ghi, jkl\"")).containsExactly("abc", "def", "ghi, jkl");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateFieldValues() throws IOException {
    String createMetadataJson = getResource("create_metadata.json");
    JsonNode node = JsonUtils.readTree(createMetadataJson);
    JiraIssueCreateMetadataNG createMetadata = new JiraIssueCreateMetadataNG(node);
    JiraIssueTypeNG issueType = createMetadata.getProjects().get("JEL").getIssueTypes().get("Story");
    assertThat(issueType).isNotNull();
    assertThat(issueType.getFields().size()).isEqualTo(19);

    issueType.removeField(JiraConstantsNG.STATUS_NAME);
    assertThat(issueType.getFields().size()).isEqualTo(18);

    Map<String, Object> currFieldsTmp = new HashMap<>();
    assertThatThrownBy(()
                           -> JiraIssueUtilsNG.updateFieldValues(
                               currFieldsTmp, issueType.getFields(), ImmutableMap.of("Component/s", "ds")))
        .isNotNull();
    assertThatThrownBy(
        () -> JiraIssueUtilsNG.updateFieldValues(currFieldsTmp, issueType.getFields(), ImmutableMap.of("number", "ds")))
        .isNotNull();
    assertThatThrownBy(()
                           -> JiraIssueUtilsNG.updateFieldValues(
                               currFieldsTmp, issueType.getFields(), ImmutableMap.of("Custom Date", "ds")))
        .isNotNull();
    assertThatThrownBy(()
                           -> JiraIssueUtilsNG.updateFieldValues(
                               currFieldsTmp, issueType.getFields(), ImmutableMap.of("customtime", "ds")))
        .isNotNull();

    Map<String, String> fields = new HashMap<>();
    fields.put("Summary", "summary");
    fields.put("Description", "description");
    fields.put("Component/s", "test, TestComp1");
    fields.put("Fix Version/s", "Version 1.0, Version 3.0");
    fields.put("Labels", "abc, def, \"hij, klm\"");
    fields.put("Priority", "Low");
    fields.put("Epic Link", "JEL-197");
    fields.put("CustomArray", "56.89");
    fields.put("testcustomfields", "1");
    fields.put("Test Custom Fields", "18");
    fields.put("test custom fields", "19.5");
    fields.put("number", "18.18");
    fields.put("Custom Date", "2021-03-25");
    fields.put("customtime", "2021-03-25T18:58:16.535+0000");
    fields.put("Original Estimate", "3d");
    fields.put("Remaining Estimate", "2d");

    Map<String, Object> currFields = new HashMap<>();
    JiraIssueUtilsNG.updateFieldValues(currFields, issueType.getFields(), fields);
    assertThat(currFields.size()).isEqualTo(15);
    assertThat(currFields.get("summary")).isEqualTo("summary");
    assertThat(currFields.get("description")).isEqualTo("description");
    assertThat(((List<JiraFieldAllowedValueNG>) currFields.get("components"))
                   .stream()
                   .map(JiraFieldAllowedValueNG::getId)
                   .collect(Collectors.toList()))
        .containsExactly("10001", "10000");
    assertThat(((List<JiraFieldAllowedValueNG>) currFields.get("fixVersions"))
                   .stream()
                   .map(JiraFieldAllowedValueNG::getId)
                   .collect(Collectors.toList()))
        .containsExactly("10000", "10002");
    assertThat((List<String>) currFields.get("labels")).containsExactly("abc", "def", "hij, klm");
    assertThat(((JiraFieldAllowedValueNG) currFields.get("priority")).getId()).isEqualTo("4");
    assertThat(currFields.get("customfield_10102")).isEqualTo("JEL-197");
    assertThat((Double) currFields.get("customfield_10212")).isCloseTo(56.89, offset(0.000001));
    assertThat(((List<JiraFieldAllowedValueNG>) currFields.get("customfield_10204"))
                   .stream()
                   .map(JiraFieldAllowedValueNG::getId)
                   .collect(Collectors.toList()))
        .containsExactly("10102");
    assertThat((Long) currFields.get("customfield_10206")).isEqualTo(18);
    assertThat((Double) currFields.get("customfield_10203")).isCloseTo(19.5, offset(0.000001));
    assertThat((Double) currFields.get("customfield_10207")).isCloseTo(18.18, offset(0.000001));
    assertThat(currFields.get("customfield_10210")).isEqualTo("2021-03-25");
    assertThat(currFields.get("customfield_10211")).isEqualTo("2021-03-25T18:58:16.535+0000");

    assertThat(((JiraTimeTrackingFieldNG) currFields.get("timetracking")).getOriginalEstimate()).isEqualTo("3d");
    assertThat(((JiraTimeTrackingFieldNG) currFields.get("timetracking")).getRemainingEstimate()).isEqualTo("2d");
  }

  private String getResource(String path) throws IOException {
    return Resources.toString(
        Objects.requireNonNull(getClass().getClassLoader().getResource("jira/" + path)), StandardCharsets.UTF_8);
  }
}

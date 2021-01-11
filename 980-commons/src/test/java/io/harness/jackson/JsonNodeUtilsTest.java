package io.harness.jackson;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JsonNodeUtilsTest extends CategoryTest {
  String JSON_RESOURCE = "testJson.json";
  ObjectNode jsonNode;

  @Before
  public void setup() throws IOException {
    final String resource =
        IOUtils.resourceToString(JSON_RESOURCE, StandardCharsets.UTF_8, getClass().getClassLoader());
    ObjectMapper objectMapper = new ObjectMapper();
    jsonNode = (ObjectNode) objectMapper.readTree(resource);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testDeletePropertiesInJsonNode() {
    JsonNodeUtils.deletePropertiesInJsonNode(jsonNode, "orgIdentifier");
    assertThat(jsonNode.get("orgIdentifier")).isNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdatePropertiesInJsonNode() {
    Map<String, String> properties = new HashMap<>();
    properties.put("orgIdentifier", "abc");
    properties.put("abcd", "xyz");
    JsonNodeUtils.updatePropertiesInJsonNode(jsonNode, properties);
    assertThat(jsonNode.get("abcd")).isNull();
    assertThat(jsonNode.get("orgIdentifier").textValue()).isEqualTo("abc");
    assertThat(jsonNode.get("orgIdentifier")).isInstanceOf(TextNode.class);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpsertPropertyInObjectNode() {
    JsonNodeUtils.upsertPropertyInObjectNode(jsonNode, "required", "latest", "latest1");
    JsonNodeUtils.upsertPropertyInObjectNode(jsonNode, "orgIdentifier", "newOrg");
    JsonNodeUtils.upsertPropertyInObjectNode(jsonNode, "newNode", "newNodeVal");
    JsonNodeUtils.upsertPropertyInObjectNode(jsonNode, "newNode1", "newNodeVal1", "newNodeVal2");
    assertThat(jsonNode.get("orgIdentifier").textValue()).isEqualTo("newOrg");
    assertThat(jsonNode.get("required")).isInstanceOf(ArrayNode.class);
    ((ArrayNode) jsonNode.get("required")).iterator().forEachRemaining(node -> {
      assertThat(node.textValue()).isIn("identifier", "name", "spec", "type", "latest", "latest1");
    });
    assertThat(jsonNode.get("newNode")).isInstanceOf(TextNode.class);
    assertThat(jsonNode.get("newNode1")).isInstanceOf(ArrayNode.class);
  }
}
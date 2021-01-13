package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PMSInputSetElementMapperTest extends CategoryTest {
  private final String PIPELINE_IDENTIFIER = "Test_Pipline11";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToInputSetEntity() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputSet = "inputSet1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml);

    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("company", "harness");
    tags.put("kind", "normal");
    List<NGTag> tagsList = TagMapper.convertToList(tags);

    assertThat(entity.getIdentifier()).isEqualTo("input1");
    assertThat(entity.getName()).isEqualTo("this name");
    assertThat(entity.getDescription()).isEqualTo("this has a description too");
    assertThat(entity.getTags()).isEqualTo(tagsList);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToInputSetEntityForOverlay() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputSet = "overlaySet1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml);

    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("isOverlaySet", "yes it is");
    List<NGTag> tagsList = TagMapper.convertToList(tags);
    List<String> references = new ArrayList<>();
    references.add("inputSet2");
    references.add("inputSet22");

    assertThat(entity.getIdentifier()).isEqualTo("overlay1");
    assertThat(entity.getName()).isEqualTo("thisName");
    assertThat(entity.getDescription()).isEqualTo("this is an overlay input set");
    assertThat(entity.getTags()).isEqualTo(tagsList);
    assertThat(entity.getInputSetReferences()).isEqualTo(references);
  }
}
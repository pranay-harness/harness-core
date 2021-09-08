package io.harness.template.mappers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.template.beans.TemplateFilterProperties;
import io.harness.template.beans.TemplateFilterPropertiesDTO;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDC)
public class TemplateFilterPropertiesMapperTest extends CategoryTest {
  @InjectMocks TemplateFilterPropertiesMapper templateFilterPropertiesMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testWriteTemplateEntity() {
    TemplateFilterPropertiesDTO filterPropertiesDTO =
        TemplateFilterPropertiesDTO.builder().templateIdentifiers(Collections.singletonList("template1")).build();
    TemplateFilterProperties filterProperties =
        (TemplateFilterProperties) templateFilterPropertiesMapper.toEntity(filterPropertiesDTO);
    assertThat(filterProperties).isNotNull();
    assertThat(filterProperties.getTemplateIdentifiers()).isEqualTo(filterPropertiesDTO.getTemplateIdentifiers());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testWriteTemplateResponseDto() {
    TemplateFilterProperties filterProperties =
        TemplateFilterProperties.builder().templateNames(Collections.singletonList("templateName")).build();
    TemplateFilterPropertiesDTO templateFilterPropertiesDTO =
        (TemplateFilterPropertiesDTO) templateFilterPropertiesMapper.writeDTO(filterProperties);
    assertThat(templateFilterPropertiesDTO).isNotNull();
    assertThat(templateFilterPropertiesDTO.getTemplateNames()).isEqualTo(filterProperties.getTemplateNames());
  }
}
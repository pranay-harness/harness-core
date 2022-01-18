/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PipelineYamlDtoMapperTest extends CategoryTest {
  String correctYaml = "pipeline:\n"
      + "  identifier: n1\n"
      + "  orgIdentifier: n2\n"
      + "  projectIdentifier: n3\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: s1";
  String wrongYaml = "pipeline:"
      + "  identifier:: n1"
      + "  stages:\n"
      + "    - stage:\n";
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToDto() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(correctYaml).build();
    PipelineConfig pipelineConfig = PipelineYamlDtoMapper.toDto(pipelineEntity);
    PipelineInfoConfig pipelineInfoConfig = pipelineConfig.getPipelineInfoConfig();
    assertThat(pipelineInfoConfig.getIdentifier()).isEqualTo("n1");
    assertThat(pipelineInfoConfig.getOrgIdentifier()).isEqualTo("n2");
    assertThat(pipelineInfoConfig.getProjectIdentifier()).isEqualTo("n3");
    assertThat(pipelineInfoConfig.getStages()).hasSize(1);

    assertThatThrownBy(() -> PipelineYamlDtoMapper.toDto(PipelineEntity.builder().yaml(wrongYaml).build()))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToDtoForYaml() {
    PipelineConfig pipelineConfig = PipelineYamlDtoMapper.toDto(correctYaml);
    PipelineInfoConfig pipelineInfoConfig = pipelineConfig.getPipelineInfoConfig();
    assertThat(pipelineInfoConfig.getIdentifier()).isEqualTo("n1");
    assertThat(pipelineInfoConfig.getOrgIdentifier()).isEqualTo("n2");
    assertThat(pipelineInfoConfig.getProjectIdentifier()).isEqualTo("n3");
    assertThat(pipelineInfoConfig.getStages()).hasSize(1);

    assertThatThrownBy(() -> PipelineYamlDtoMapper.toDto(wrongYaml)).isInstanceOf(InvalidRequestException.class);
  }
}

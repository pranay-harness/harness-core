/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PMSPipelineFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().build();
    List<String> fieldsToBeUpdated = new ArrayList<>();
    fieldsToBeUpdated.add(PipelineEntityKeys.name);
    fieldsToBeUpdated.add(PipelineEntityKeys.accountId);
    fieldsToBeUpdated.add(PipelineEntityKeys.orgIdentifier);
    fieldsToBeUpdated.add(PipelineEntityKeys.projectIdentifier);
    fieldsToBeUpdated.add(PipelineEntityKeys.yaml);
    fieldsToBeUpdated.add(PipelineEntityKeys.tags);
    fieldsToBeUpdated.add(PipelineEntityKeys.deleted);
    fieldsToBeUpdated.add(PipelineEntityKeys.description);
    fieldsToBeUpdated.add(PipelineEntityKeys.stageCount);
    fieldsToBeUpdated.add(PipelineEntityKeys.lastUpdatedAt);
    fieldsToBeUpdated.add(PipelineEntityKeys.filters);

    for (String field : fieldsToBeUpdated) {
      assertThat(true).isEqualTo(PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity).modifies(field));
    }
  }
}

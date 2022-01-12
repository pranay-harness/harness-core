/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.mappers;

import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class PMSPipelineFilterHelper {
  public Update getUpdateOperations(PipelineEntity pipelineEntity) {
    Update update = new Update();
    update.set(PipelineEntityKeys.accountId, pipelineEntity.getAccountId());
    update.set(PipelineEntityKeys.orgIdentifier, pipelineEntity.getOrgIdentifier());
    update.set(PipelineEntityKeys.projectIdentifier, pipelineEntity.getProjectIdentifier());
    update.set(PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    update.set(PipelineEntityKeys.tags, pipelineEntity.getTags());
    update.set(PipelineEntityKeys.deleted, false);
    update.set(PipelineEntityKeys.name, pipelineEntity.getName());
    update.set(PipelineEntityKeys.description, pipelineEntity.getDescription());
    update.set(PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    update.set(PipelineEntityKeys.lastUpdatedAt, System.currentTimeMillis());
    update.set(PipelineEntityKeys.filters, pipelineEntity.getFilters());
    update.set(PipelineEntityKeys.stageNames, pipelineEntity.getStageNames());

    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(PipelineEntityKeys.deleted, true);
    return update;
  }
}

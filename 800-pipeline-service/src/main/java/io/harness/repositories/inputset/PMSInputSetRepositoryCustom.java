/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.pms.inputset.gitsync.InputSetYamlDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PMSInputSetRepositoryCustom {
  List<InputSetEntity> findAll(Criteria criteria);

  Page<InputSetEntity> findAll(
      Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  InputSetEntity save(InputSetEntity entityToSave, InputSetYamlDTO yamlDTO);

  Optional<InputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String identifier, boolean notDeleted);

  InputSetEntity update(InputSetEntity entityToUpdate, InputSetYamlDTO yamlDTO, ChangeType changeType);

  InputSetEntity switchValidationFlag(Criteria criteria, Update update);

  InputSetEntity delete(InputSetEntity entityToDelete, InputSetYamlDTO yamlDTO);

  UpdateResult deleteAllInputSetsWhenPipelineDeleted(Query query, Update update);
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.pms.filter.creation.FilterCreatorMergeService;
import io.harness.pms.filter.creation.FilterCreatorMergeServiceResponse;
import io.harness.pms.filter.utils.ModuleInfoFilterUtils;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.serializer.JsonUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceHelper {
  @Inject private final FilterService filterService;
  @Inject private final FilterCreatorMergeService filterCreatorMergeService;

  public static void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  public static Criteria getPipelineEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean deleted, Long version) {
    Criteria criteria = Criteria.where(PipelineEntityKeys.accountId)
                            .is(accountId)
                            .and(PipelineEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(PipelineEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineEntityKeys.identifier)
                            .is(pipelineIdentifier)
                            .and(PipelineEntityKeys.deleted)
                            .is(deleted);

    if (version != null) {
      criteria.and(PipelineEntityKeys.version).is(version);
    }

    return criteria;
  }

  public PipelineEntity updatePipelineInfo(PipelineEntity pipelineEntity) throws IOException {
    FilterCreatorMergeServiceResponse filtersAndStageCount = filterCreatorMergeService.getPipelineInfo(pipelineEntity);
    PipelineEntity newEntity = pipelineEntity.withStageCount(filtersAndStageCount.getStageCount())
                                   .withStageNames(filtersAndStageCount.getStageNames());
    if (isNotEmpty(filtersAndStageCount.getFilters())) {
      filtersAndStageCount.getFilters().forEach((key, value) -> newEntity.getFilters().put(key, Document.parse(value)));
    }
    return newEntity;
  }

  public void populateFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO = filterService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINESETUP);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a pipeline filter with the identifier ");
    } else {
      populateFilter(criteria, (PipelineFilterPropertiesDto) pipelineFilterDTO.getFilterProperties());
    }
  }

  public static void populateFilter(Criteria criteria, @NotNull PipelineFilterPropertiesDto pipelineFilter) {
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getName())) {
      criteria.and(PipelineEntityKeys.name).is(pipelineFilter.getName());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getDescription())) {
      criteria.and(PipelineEntityKeys.description).is(pipelineFilter.getDescription());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getPipelineTags())) {
      criteria.and(PipelineEntityKeys.tags).in(pipelineFilter.getPipelineTags());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getPipelineIdentifiers())) {
      criteria.and(PipelineEntityKeys.identifier).in(pipelineFilter.getPipelineIdentifiers());
    }
    if (pipelineFilter.getModuleProperties() != null) {
      ModuleInfoFilterUtils.processNode(
          JsonUtils.readTree(pipelineFilter.getModuleProperties().toJson()), "filters", criteria);
    }
  }
}

package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.springdata.SpringDataMongoUtils.populateInFilter;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.springdata.SpringDataMongoUtils;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.gitsync.TemplateGitSyncBranchContextGuard;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CDC)
public class NGTemplateServiceHelper {
  private final FilterService filterService;

  public static void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  public TemplateGitSyncBranchContextGuard getTemplateGitContext(
      TemplateEntity template, GitEntityFindInfoDTO gitEntityBasicInfo, String commitMsg) {
    boolean defaultFromOtherRepo = false;
    String branch = "";
    String yamlGitConfigId = "";
    if (gitEntityBasicInfo != null) {
      if (gitEntityBasicInfo.getDefaultFromOtherRepo() != null) {
        defaultFromOtherRepo = gitEntityBasicInfo.getDefaultFromOtherRepo();
      }
      branch = gitEntityBasicInfo.getBranch();
      yamlGitConfigId = gitEntityBasicInfo.getYamlGitConfigId();
    }
    GitSyncBranchContext branchContext = GitSyncBranchContext.builder()
                                             .gitBranchInfo(GitEntityInfo.builder()
                                                                .branch(branch)
                                                                .yamlGitConfigId(yamlGitConfigId)
                                                                .findDefaultFromOtherRepos(defaultFromOtherRepo)
                                                                .filePath(template.getFilePath())
                                                                .folderPath(template.getRootFolder())
                                                                .lastObjectId(template.getObjectIdOfYaml())
                                                                .commitMsg(commitMsg)
                                                                .build())
                                             .build();
    return new TemplateGitSyncBranchContextGuard(branchContext, false);
  }

  public Criteria formCriteria(String accountId, String orgId, String projectId, String filterIdentifier,
      TemplateFilterPropertiesDTO filterProperties, boolean deleted, String searchTerm) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(TemplateEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgId)) {
      criteria.and(TemplateEntityKeys.orgIdentifier).is(orgId);
    }
    if (isNotEmpty(projectId)) {
      criteria.and(TemplateEntityKeys.projectIdentifier).is(projectId);
    }
    criteria.and(TemplateEntityKeys.deleted).is(deleted);

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      populateFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier, searchTerm);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      NGTemplateServiceHelper.populateFilter(criteria, filterProperties, searchTerm);
    } else {
      Criteria searchTermCriteria = getSearchTermCriteria(searchTerm);
      if (searchTermCriteria != null) {
        criteria.andOperator(searchTermCriteria);
      }
    }
    return criteria;
  }

  private void populateFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier, String searchTerm) {
    FilterDTO pipelineFilterDTO =
        filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.TEMPLATE);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a Template filter with the identifier ");
    } else {
      populateFilter(criteria, (TemplateFilterPropertiesDTO) pipelineFilterDTO.getFilterProperties(), searchTerm);
    }
  }

  private static void populateFilter(
      Criteria criteria, @NotNull TemplateFilterPropertiesDTO templateFilter, String searchTerm) {
    populateInFilter(criteria, TemplateEntityKeys.identifier, templateFilter.getTemplateIdentifiers());
    List<Criteria> criteriaList = new ArrayList<>();
    Criteria nameFilter = getCaseInsensitiveFilter(TemplateEntityKeys.name, templateFilter.getTemplateNames());
    if (nameFilter != null) {
      criteriaList.add(nameFilter);
    }
    Criteria descriptionFilter = getDescriptionFilter(templateFilter.getDescription());
    if (descriptionFilter != null) {
      criteriaList.add(descriptionFilter);
    }
    Criteria searchTermCriteria = getSearchTermCriteria(searchTerm);
    if (searchTermCriteria != null) {
      criteriaList.add(searchTermCriteria);
    }
    if (criteriaList.size() != 0) {
      criteria.andOperator(criteriaList.toArray(new Criteria[0]));
    }
    populateTagsFilter(criteria, templateFilter.getTags());
    populateInFilter(criteria, TemplateEntityKeys.templateEntityType, templateFilter.getTemplateEntityTypes());
    populateInFilter(criteria, TemplateEntityKeys.childType, templateFilter.getChildTypes());
  }

  private static Criteria getSearchTermCriteria(String searchTerm) {
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria();
      searchCriteria.orOperator(where(TemplateEntityKeys.identifier)
                                    .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.versionLabel)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.description)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.tags + "." + NGTagKeys.key)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.tags + "." + NGTagKeys.value)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));

      return searchCriteria;
    }
    return null;
  }

  private static Criteria getCaseInsensitiveFilter(String fieldName, List<String> values) {
    if (isNotEmpty(values)) {
      List<Criteria> criteriaForCaseInsensitive =
          values.stream()
              .map(value -> where(fieldName).regex(value, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS))
              .collect(Collectors.toList());
      return new Criteria().orOperator(criteriaForCaseInsensitive.toArray(new Criteria[0]));
    }
    return null;
  }

  private static Criteria getDescriptionFilter(String description) {
    if (isBlank(description)) {
      return null;
    }
    String[] descriptionsWords = description.split(" ");
    if (isNotEmpty(descriptionsWords)) {
      String pattern = SpringDataMongoUtils.getPatternForMatchingAnyOneOf(Arrays.asList(descriptionsWords));
      return where(TemplateEntityKeys.description)
          .regex(pattern, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS);
    }
    return null;
  }

  private static void populateTagsFilter(Criteria criteria, Map<String, String> tags) {
    if (isEmpty(tags)) {
      return;
    }
    criteria.and(TemplateEntityKeys.tags).in(TagMapper.convertToList(tags));
  }
}

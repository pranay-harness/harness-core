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
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.springdata.SpringDataMongoUtils;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      populateFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      NGTemplateServiceHelper.populateFilter(criteria, filterProperties);
    }

    Criteria searchCriteria = new Criteria();
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      searchCriteria.orOperator(where(TemplateEntityKeys.identifier)
                                    .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.description)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.tags + "." + NGTagKeys.key)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(TemplateEntityKeys.tags + "." + NGTagKeys.value)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  private void populateFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO =
        filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.TEMPLATE);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a Template filter with the identifier ");
    } else {
      populateFilter(criteria, (TemplateFilterPropertiesDTO) pipelineFilterDTO.getFilterProperties());
    }
  }

  private static void populateFilter(Criteria criteria, @NotNull TemplateFilterPropertiesDTO templateFilter) {
    populateCaseInsensitiveFilter(criteria, TemplateEntityKeys.name, templateFilter.getTemplateNames());
    populateInFilter(criteria, TemplateEntityKeys.identifier, templateFilter.getTemplateIdentifiers());
    populateDescriptionFilter(criteria, templateFilter.getDescription());
    populateTagsFilter(criteria, templateFilter.getTags());
    populateInFilter(criteria, TemplateEntityKeys.templateEntityType, templateFilter.getTemplateEntityTypes());
    populateInFilter(criteria, TemplateEntityKeys.childType, templateFilter.getChildTypes());
  }

  private static void populateCaseInsensitiveFilter(Criteria criteria, String fieldName, List<String> values) {
    if (isNotEmpty(values)) {
      Criteria orOperator = new Criteria().orOperator(
          values.stream()
              .map(value -> where(fieldName).regex(value, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS))
              .toArray(Criteria[] ::new));
      criteria.andOperator(orOperator);
    }
  }

  private static void populateDescriptionFilter(Criteria criteria, String description) {
    if (isBlank(description)) {
      return;
    }
    String[] descriptionsWords = description.split(" ");
    if (isNotEmpty(descriptionsWords)) {
      String pattern = SpringDataMongoUtils.getPatternForMatchingAnyOneOf(Arrays.asList(descriptionsWords));
      Criteria descriptionCriteria = where(TemplateEntityKeys.description)
                                         .regex(pattern, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS);
      criteria.andOperator(descriptionCriteria);
    }
  }

  private static void populateTagsFilter(Criteria criteria, Map<String, String> tags) {
    if (isEmpty(tags)) {
      return;
    }
    criteria.and(TemplateEntityKeys.tags).in(TagMapper.convertToList(tags));
  }
}

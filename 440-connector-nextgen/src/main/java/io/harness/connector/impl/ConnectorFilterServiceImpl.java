package io.harness.connector.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.Scope.ORG;
import static io.harness.encryption.Scope.PROJECT;
import static io.harness.filter.FilterType.CONNECTOR;
import static io.harness.springdata.SpringDataMongoUtils.populateInFilter;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.connector.apis.dto.ConnectorFilterPropertiesDTO;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.ScopeHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.service.FilterService;
import io.harness.ng.core.mapper.TagMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ConnectorFilterServiceImpl implements ConnectorFilterService {
  FilterService filterService;

  public static final String CREDENTIAL_TYPE_KEY = "credentialType";
  public static final String INHERIT_FROM_DELEGATE_STRING = "INHERIT_FROM_DELEGATE";

  @Override
  public Criteria createCriteriaFromConnectorListQueryParams(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filterIdentifier, String searchTerm, FilterPropertiesDTO filterProperties,
      Boolean includeAllConnectorsAccessibleAtScope) {
    if (isNotBlank(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    }
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    if (includeAllConnectorsAccessibleAtScope != null && includeAllConnectorsAccessibleAtScope) {
      addCriteriaToReturnAllConnectorsAccessible(criteria, orgIdentifier, projectIdentifier);
    } else {
      criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
      criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    if (isNotBlank(searchTerm)) {
      populateSearchTermFilter(criteria, searchTerm);
    }
    if (isEmpty(filterIdentifier) && filterProperties == null) {
      return criteria;
    }

    if (isNotBlank(filterIdentifier)) {
      populateSavedConnectorFilter(
          criteria, filterIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm);
    } else {
      populateConnectorFiltersInTheCriteria(criteria, (ConnectorFilterPropertiesDTO) filterProperties, searchTerm);
    }
    return criteria;
  }

  private void addCriteriaToReturnAllConnectorsAccessible(
      Criteria criteria, String orgIdentifier, String projectIdentifier) {
    if (isNotBlank(projectIdentifier)) {
      Criteria orCriteria = new Criteria().orOperator(
          Criteria.where(ConnectorKeys.scope).is(PROJECT).and(ConnectorKeys.projectIdentifier).is(projectIdentifier),
          Criteria.where(ConnectorKeys.scope).is(ORG).and(ConnectorKeys.orgIdentifier).is(orgIdentifier),
          Criteria.where(ConnectorKeys.scope).is(ACCOUNT));
      criteria.andOperator(orCriteria);
    } else if (isNotBlank(orgIdentifier)) {
      Criteria orCriteria = new Criteria().orOperator(
          Criteria.where(ConnectorKeys.scope).is(ORG).and(ConnectorKeys.orgIdentifier).is(orgIdentifier),
          Criteria.where(ConnectorKeys.scope).is(ACCOUNT));
      criteria.andOperator(orCriteria);
    } else {
      criteria.and(ConnectorKeys.scope).is(ACCOUNT);
    }
  }

  private void populateSavedConnectorFilter(Criteria criteria, String filterIdentifier, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String searchTerm) {
    FilterDTO connectorFilterDTO =
        filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, CONNECTOR);
    if (connectorFilterDTO == null) {
      throw new InvalidRequestException(String.format("Could not find a connector filter with the identifier %s, in %s",
          ScopeHelper.getScopeMessageForLogs(accountIdentifier, orgIdentifier, projectIdentifier)));
    }
    populateConnectorFiltersInTheCriteria(
        criteria, (ConnectorFilterPropertiesDTO) connectorFilterDTO.getFilterProperties(), searchTerm);
  }

  private void populateConnectorFiltersInTheCriteria(
      Criteria criteria, ConnectorFilterPropertiesDTO connectorFilter, String searchTerm) {
    if (connectorFilter == null) {
      return;
    }
    populateInFilter(criteria, ConnectorKeys.categories, connectorFilter.getCategories());
    populateInFilter(criteria, ConnectorKeys.type, connectorFilter.getTypes());
    populateInFilter(criteria, ConnectorKeys.name, connectorFilter.getConnectorNames());
    populateInFilter(criteria, ConnectorKeys.identifier, connectorFilter.getConnectorIdentifiers());
    populateDescriptionFilter(criteria, connectorFilter.getDescription(), searchTerm);
    populateInFilter(criteria, ConnectorKeys.connectionStatus, connectorFilter.getConnectivityStatuses());
    populateInheritCredentialsFromDelegateFilter(criteria, connectorFilter.getInheritingCredentialsFromDelegate());
    populateTagsFilter(criteria, connectorFilter.getTags());
  }

  private void populateTagsFilter(Criteria criteria, Map<String, String> tags) {
    if (isEmpty(tags)) {
      return;
    }
    criteria.and(ConnectorKeys.tags).in(TagMapper.convertToList(tags));
  }

  private void populateInheritCredentialsFromDelegateFilter(
      Criteria criteria, Boolean inheritingCredentialsFromDelegate) {
    if (inheritingCredentialsFromDelegate != null) {
      if (inheritingCredentialsFromDelegate.booleanValue()) {
        addCriteriaForInheritingFromDelegate(criteria);
      } else {
        addCriteriaForNotInheritingFromDelegate(criteria);
      }
    }
  }

  private void addCriteriaForNotInheritingFromDelegate(Criteria criteria) {
    Criteria criteriaForInheritingFromDelegate = new Criteria().orOperator(
        where(CREDENTIAL_TYPE_KEY).exists(false), where(CREDENTIAL_TYPE_KEY).ne(INHERIT_FROM_DELEGATE_STRING));
    criteria.andOperator(criteriaForInheritingFromDelegate);
  }

  private void addCriteriaForInheritingFromDelegate(Criteria criteria) {
    criteria.and(CREDENTIAL_TYPE_KEY).is(INHERIT_FROM_DELEGATE_STRING);
  }

  private String getPatternForMatchingAnyOneOf(List<String> wordsToBeMatched) {
    return StringUtils.collectionToDelimitedString(wordsToBeMatched, "|");
  }

  private void populateDescriptionFilter(Criteria criteria, String description, String searchTerm) {
    if (isBlank(description)) {
      return;
    }
    String[] descriptionsWords = description.split(" ");
    if (isNotEmpty(descriptionsWords) && isBlank(searchTerm)) {
      String pattern = getPatternForMatchingAnyOneOf(Arrays.asList(descriptionsWords));
      Criteria descriptionCriteria = Criteria.where(ConnectorKeys.description)
                                         .regex(pattern, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS);
      criteria.andOperator(descriptionCriteria);
    }
  }

  private void populateSearchTermFilter(Criteria criteria, String searchTerm) {
    if (isNotBlank(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(ConnectorKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ConnectorKeys.identifier).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ConnectorKeys.tags).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ConnectorKeys.description).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
  }

  public Criteria createCriteriaFromConnectorFilter(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType connectorType, ConnectorCategory category) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    if (connectorType != null) {
      criteria.and(ConnectorKeys.type).is(connectorType.name());
    }

    if (category != null) {
      criteria.and(ConnectorKeys.categories).in(category);
    }

    if (isNotBlank(searchTerm)) {
      Criteria seachCriteria = new Criteria().orOperator(where(ConnectorKeys.name).regex(searchTerm, "i"),
          where(NGCommonEntityConstants.IDENTIFIER_KEY).regex(searchTerm, "i"),
          where(NGCommonEntityConstants.TAGS_KEY).regex(searchTerm, "i"));
      criteria.andOperator(seachCriteria);
    }
    return criteria;
  }
}

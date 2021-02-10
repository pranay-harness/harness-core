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
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.ScopeHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.service.FilterService;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.mapper.TagMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ConnectorFilterServiceImpl implements ConnectorFilterService {
  FilterService filterService;

  public static final String CREDENTIAL_TYPE_KEY = "credentialType";
  public static final String INHERIT_FROM_DELEGATE_STRING = "INHERIT_FROM_DELEGATE";

  @Override
  public Criteria createCriteriaFromConnectorListQueryParams(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filterIdentifier, String encodedSearchTerm, FilterPropertiesDTO filterProperties,
      Boolean includeAllConnectorsAccessibleAtScope) {
    if (isNotBlank(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    }
    String searchTerm = getDecodedSearchTerm(encodedSearchTerm);
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    if (includeAllConnectorsAccessibleAtScope != null && includeAllConnectorsAccessibleAtScope) {
      addCriteriaToReturnAllConnectorsAccessible(criteria, orgIdentifier, projectIdentifier);
    } else {
      criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
      criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));

    if (isEmpty(filterIdentifier) && filterProperties == null) {
      applySearchFilter(criteria, searchTerm);
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

  private String getDecodedSearchTerm(String encodedSearchTerm) {
    String decodedString = null;
    if (isNotBlank(encodedSearchTerm)) {
      try {
        decodedString = java.net.URLDecoder.decode(encodedSearchTerm, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        log.info("Encountered exception while decoding {}", encodedSearchTerm);
      }
    }
    return decodedString;
  }

  private void applySearchFilter(Criteria criteria, String searchTerm) {
    if (isNotBlank(searchTerm)) {
      Criteria criteriaWithSearchTerm = getSearchTermFilter(criteria, searchTerm);
      criteria.andOperator(criteriaWithSearchTerm);
    }
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
    populateNameDesciptionAndSearchTermFilter(criteria, connectorFilter.getConnectorNames(),
        connectorFilter.getDescription(), searchTerm, connectorFilter.getInheritingCredentialsFromDelegate());
    populateInFilter(criteria, ConnectorKeys.identifier, connectorFilter.getConnectorIdentifiers());
    populateInFilter(criteria, ConnectorKeys.connectionStatus, connectorFilter.getConnectivityStatuses());
    populateTagsFilter(criteria, connectorFilter.getTags());
  }

  private void populateNameDesciptionAndSearchTermFilter(Criteria criteria, List<String> connectorNames,
      String description, String searchTerm, Boolean inheritingCredentialsFromDelegate) {
    List<Criteria> criteriaList = new ArrayList<>();
    Criteria nameCriteria = getNameFilter(criteria, connectorNames);
    if (nameCriteria != null) {
      criteriaList.add(nameCriteria);
    }
    Criteria descriptionCriteria = getDescriptionFilter(criteria, description, searchTerm);
    if (descriptionCriteria != null) {
      criteriaList.add(descriptionCriteria);
    }
    Criteria inheritingFromDelegateCriteria =
        getInheritCredentialsFromDelegateFilter(criteria, inheritingCredentialsFromDelegate);
    if (inheritingFromDelegateCriteria != null) {
      criteriaList.add(inheritingFromDelegateCriteria);
    }
    Criteria searchCriteria = getSearchTermFilter(criteria, searchTerm);
    if (searchCriteria != null) {
      criteriaList.add(searchCriteria);
    }
    if (criteriaList.size() != 0) {
      criteria.andOperator(criteriaList.toArray(new Criteria[0]));
    }
  }

  private Criteria getNameFilter(Criteria criteria, List<String> connectorNames) {
    if (isEmpty(connectorNames)) {
      return null;
    }
    List<Criteria> criteriaForNames =
        connectorNames.stream()
            .map(
                name -> where(ConnectorKeys.name).regex(name, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS))
            .collect(Collectors.toList());
    Criteria searchCriteriaForNames = new Criteria().orOperator(criteriaForNames.toArray(new Criteria[0]));
    return searchCriteriaForNames;
  }

  private void populateTagsFilter(Criteria criteria, Map<String, String> tags) {
    if (isEmpty(tags)) {
      return;
    }
    criteria.and(ConnectorKeys.tags).in(TagMapper.convertToList(tags));
  }

  private Criteria getInheritCredentialsFromDelegateFilter(
      Criteria criteria, Boolean inheritingCredentialsFromDelegate) {
    if (inheritingCredentialsFromDelegate != null) {
      if (inheritingCredentialsFromDelegate.booleanValue()) {
        return addCriteriaForInheritingFromDelegate(criteria);
      } else {
        return addCriteriaForNotInheritingFromDelegate(criteria);
      }
    }
    return null;
  }

  private Criteria addCriteriaForNotInheritingFromDelegate(Criteria criteria) {
    Criteria criteriaForInheritingFromDelegate = new Criteria().orOperator(
        where(CREDENTIAL_TYPE_KEY).exists(false), where(CREDENTIAL_TYPE_KEY).ne(INHERIT_FROM_DELEGATE_STRING));
    return criteriaForInheritingFromDelegate;
  }

  private Criteria addCriteriaForInheritingFromDelegate(Criteria criteria) {
    return where(CREDENTIAL_TYPE_KEY).is(INHERIT_FROM_DELEGATE_STRING);
  }

  private String getPatternForMatchingAnyOneOf(List<String> wordsToBeMatched) {
    return StringUtils.collectionToDelimitedString(wordsToBeMatched, "|");
  }

  private Criteria getDescriptionFilter(Criteria criteria, String description, String searchTerm) {
    if (isBlank(description)) {
      return null;
    }
    String[] descriptionsWords = description.split(" ");
    if (isNotEmpty(descriptionsWords)) {
      String pattern = getPatternForMatchingAnyOneOf(Arrays.asList(descriptionsWords));
      return where(ConnectorKeys.description).regex(pattern, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS);
    }
    return null;
  }

  private Criteria getSearchTermFilter(Criteria criteria, String searchTerm) {
    if (isNotBlank(searchTerm)) {
      Criteria tagCriteria = createCriteriaForSearchingTag(searchTerm);
      Criteria searchCriteria = new Criteria().orOperator(
          where(ConnectorKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ConnectorKeys.identifier).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ConnectorKeys.description).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          tagCriteria);
      return searchCriteria;
    }
    return null;
  }

  private Criteria createCriteriaForSearchingTag(String searchTerm) {
    String keyToBeSearched = searchTerm;
    String valueToBeSearched = "";
    if (searchTerm.contains(":")) {
      String[] split = searchTerm.split(":");
      keyToBeSearched = split[0];
      valueToBeSearched = split.length >= 2 ? split[1] : "";
    }
    return where(ConnectorKeys.tags).is(NGTag.builder().key(keyToBeSearched).value(valueToBeSearched).build());
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
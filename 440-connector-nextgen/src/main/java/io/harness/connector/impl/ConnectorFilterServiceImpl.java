package io.harness.connector.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.springdata.SpringDataMongoUtils.populateInFilter;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.apis.dto.ConnectorListFilter;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.entities.ConnectorFilter;
import io.harness.connector.entities.ConnectorFilter.ConnectorFilterKeys;
import io.harness.connector.mappers.ConnectorFilterMapper;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.repositories.filters.ConnectorFilterRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ConnectorFilterServiceImpl implements ConnectorFilterService {
  private ConnectorFilterRepository filterRepository;
  private ConnectorFilterMapper connectorFilterMapper;

  public static final String CREDENTIAL_TYPE_KEY = "credentialType";
  public static final String INHERIT_FROM_DELEGATE_STRING = "INHERIT_FROM_DELEGATE";

  @Override
  public ConnectorFilterDTO create(String accountId, ConnectorFilterDTO filter) {
    ConnectorFilter filterEntity = connectorFilterMapper.toConnectorFilter(filter, accountId);
    try {
      return connectorFilterMapper.writeDTO(filterRepository.save(filterEntity));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector Filter %s already exists", filter.getIdentifier()));
    }
  }

  @Override
  public ConnectorFilterDTO update(String accountId, ConnectorFilterDTO filter) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountId, filter.getOrgIdentifier(), filter.getProjectIdentifier(), filter.getIdentifier());
    Optional<ConnectorFilter> existingConnectorFilter =
        filterRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (!existingConnectorFilter.isPresent()) {
      throw new InvalidRequestException(
          format("No connector filter exists with the  Identifier %s", filter.getIdentifier()));
    }
    ConnectorFilter filterEntity = connectorFilterMapper.toConnectorFilter(filter, accountId);
    filterEntity.setId(existingConnectorFilter.get().getId());
    filterEntity.setVersion(existingConnectorFilter.get().getVersion());
    ConnectorFilter updatedConnectorFilter = filterRepository.save(filterEntity);
    return connectorFilterMapper.writeDTO(updatedConnectorFilter);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountId, orgIdentifier, projectIdentifier, identifier);
    long deletedCount = filterRepository.deleteByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (deletedCount == 1) {
      return true;
    }
    throwNoConnectorFilterExistsException(orgIdentifier, projectIdentifier, identifier);
    return false;
  }

  private void throwNoConnectorFilterExistsException(
      String orgIdentifier, String projectIdentifier, String identifier) {
    throw new InvalidRequestException(format("No Connector Filter exists with the identifier %s in org %s, project %s",
        identifier, orgIdentifier, projectIdentifier));
  }

  @Override
  public ConnectorFilterDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountId, orgIdentifier, projectIdentifier, identifier);
    Optional<ConnectorFilter> connectorFilter =
        filterRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (!connectorFilter.isPresent()) {
      throwNoConnectorFilterExistsException(orgIdentifier, projectIdentifier, identifier);
    }
    return connectorFilterMapper.writeDTO(connectorFilter.get());
  }

  @Override
  public Page<ConnectorFilterDTO> list(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> filterIds) {
    Pageable pageable = getPageRequest(
        PageRequest.builder()
            .pageIndex(page)
            .pageSize(size)
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(ConnectorFilterKeys.createdAt, OrderType.DESC).build()))
            .build());
    Criteria criteria = new Criteria();
    if (isNotEmpty(filterIds)) {
      criteria.and(ConnectorFilterKeys.identifier).in(filterIds);
    }
    criteria.and(ConnectorFilterKeys.accountIdentifier).in(accountId);
    criteria.and(ConnectorFilterKeys.orgIdentifier).in(orgIdentifier);
    criteria.and(ConnectorFilterKeys.projectIdentifier).in(projectIdentifier);
    return filterRepository.findAll(criteria, pageable).map(connectorFilterMapper::writeDTO);
  }

  public Criteria createCriteriaFromConnectorListQueryParams(
      String accountIdentifier, ConnectorListFilter connectorFilterQueryParams) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    if (connectorFilterQueryParams == null) {
      return criteria;
    }
    if (isNotBlank(connectorFilterQueryParams.getFilterIdentifier())) {
      populateSavedConnectorFilter(criteria, connectorFilterQueryParams.getFilterIdentifier(), accountIdentifier,
          connectorFilterQueryParams.getFilterOrgIdentifier(), connectorFilterQueryParams.getFilterProjectIdentifier());
    } else {
      ConnectorFilterDTO connectorFilter = createConnectorFilter(connectorFilterQueryParams);
      populateInFilter(criteria, ConnectorKeys.orgIdentifier, connectorFilterQueryParams.getOrgIdentifier());
      populateInFilter(criteria, ConnectorKeys.projectIdentifier, connectorFilterQueryParams.getProjectIdentifier());
      populateConnectorFiltersInTheCriteria(criteria, connectorFilter);
    }
    return criteria;
  }

  private ConnectorFilterDTO createConnectorFilter(ConnectorListFilter connectorFilterQueryParams) {
    return ConnectorFilterDTO.builder()
        .connectorIdentifiers(connectorFilterQueryParams.getConnectorIdentifier())
        .categories(connectorFilterQueryParams.getCategory())
        .connectivityStatuses(connectorFilterQueryParams.getConnectivityStatus())
        .descriptions(connectorFilterQueryParams.getDescription())
        .inheritingCredentialsFromDelegate(connectorFilterQueryParams.getInheritingCredentialsFromDelegate())
        .scopes(connectorFilterQueryParams.getScope())
        .connectorNames(connectorFilterQueryParams.getName())
        .types(connectorFilterQueryParams.getType())
        .searchTerm(connectorFilterQueryParams.getSearchTerm())
        .build();
  }

  private void populateSavedConnectorFilter(Criteria criteria, String filterIdentifier, String accountIdentifier,
      String orgIdentifier, String projectIdentifier) {
    ConnectorFilterDTO connectorFilter = get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier);
    criteria.and(ConnectorKeys.orgIdentifier).is(connectorFilter.getOrgIdentifier());
    criteria.and(ConnectorKeys.projectIdentifier).is(connectorFilter.getProjectIdentifier());
    populateConnectorFiltersInTheCriteria(criteria, connectorFilter);
  }

  private void populateConnectorFiltersInTheCriteria(Criteria criteria, ConnectorFilterDTO connectorFilter) {
    if (connectorFilter == null) {
      return;
    }
    populateInFilter(criteria, ConnectorKeys.scope, connectorFilter.getScopes());
    populateInFilter(criteria, ConnectorKeys.categories, connectorFilter.getCategories());
    populateInFilter(criteria, ConnectorKeys.type, connectorFilter.getTypes());
    populateSearchTermFilter(criteria, connectorFilter.getSearchTerm());
    populateInFilter(criteria, ConnectorKeys.name, connectorFilter.getConnectorNames());
    populateInFilter(criteria, ConnectorKeys.identifier, connectorFilter.getConnectorIdentifiers());
    populateDescriptionFilter(criteria, connectorFilter.getDescriptions(), connectorFilter.getSearchTerm());
    populateInFilter(criteria, ConnectorKeys.connectionStatus, connectorFilter.getConnectivityStatuses());
    populateInheritCredentialsFromDelegateFilter(criteria, connectorFilter.getInheritingCredentialsFromDelegate());
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

  private void populateDescriptionFilter(Criteria criteria, List<String> descriptions, String searchTerm) {
    if (isNotEmpty(descriptions) && isBlank(searchTerm)) {
      String pattern = getPatternForMatchingAnyOneOf(descriptions);
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

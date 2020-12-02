package io.harness.connector.mappers;

import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.entities.ConnectorFilter;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Singleton;

@Singleton
public class ConnectorFilterMapper {
  public ConnectorFilter toConnectorFilter(ConnectorFilterDTO filter, String accountIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, filter.getOrgIdentifier(), filter.getProjectIdentifier(), filter.getIdentifier());
    return ConnectorFilter.builder()
        .name(filter.getName())
        .identifier(filter.getIdentifier())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(filter.getOrgIdentifier())
        .projectIdentifier(filter.getProjectIdentifier())
        .connectorIdentifier(filter.getConnectorIdentifiers())
        .fullyQualifiedIdentifier(fullyQualifiedIdentifier)
        .categories(filter.getCategories())
        .connectivityStatuses(filter.getConnectivityStatuses())
        .descriptions(filter.getDescriptions())
        .inheritingCredentialsFromDelegate(filter.getInheritingCredentialsFromDelegate())
        .scopes(filter.getScopes())
        .connectorNames(filter.getConnectorNames())
        .types(filter.getTypes())
        .searchTerm(filter.getSearchTerm())
        .build();
  }

  public ConnectorFilterDTO writeDTO(ConnectorFilter filterEntity) {
    return ConnectorFilterDTO.builder()
        .name(filterEntity.getName())
        .identifier(filterEntity.getIdentifier())
        .orgIdentifier(filterEntity.getOrgIdentifier())
        .projectIdentifier(filterEntity.getProjectIdentifier())
        .connectorIdentifiers(filterEntity.getConnectorIdentifier())
        .categories(filterEntity.getCategories())
        .connectivityStatuses(filterEntity.getConnectivityStatuses())
        .descriptions(filterEntity.getDescriptions())
        .inheritingCredentialsFromDelegate(filterEntity.getInheritingCredentialsFromDelegate())
        .scopes(filterEntity.getScopes())
        .connectorNames(filterEntity.getConnectorNames())
        .types(filterEntity.getTypes())
        .searchTerm(filterEntity.getSearchTerm())
        .build();
  }
}
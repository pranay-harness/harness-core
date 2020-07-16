package io.harness.connector;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorSummaryMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Map;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorScopeHelper {
  private final ConnectorSummaryMapper connectorSummaryMapper;
  private final OrgScopeHelper orgScopeHelper;
  private final ProjectScopeHelper projectScopeHelper;

  public Page<ConnectorSummaryDTO> createConnectorSummaryListForConnectors(Page<Connector> connectorsList) {
    // todo: @deepak The account resides in 71-rest, thus I could not get the accountName
    String accountName = "Test Account";
    Map<String, String> orgIdentifierOrgNameMap =
        orgScopeHelper.createOrgIdentifierOrgNameMap(orgScopeHelper.getOrgIdentifiers(connectorsList.toList()));
    Map<String, String> projectIdentifierProjectNameMap = projectScopeHelper.createProjectIdentifierProjectNameMap(
        projectScopeHelper.getProjectIdentifiers(connectorsList.toList()));
    return connectorsList.map(connector
        -> connectorSummaryMapper.writeConnectorSummaryDTO(
            connector, accountName, orgIdentifierOrgNameMap, projectIdentifierProjectNameMap));
  }
}
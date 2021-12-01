package io.harness.batch.processing.connectors;

import com.google.inject.Singleton;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.Account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Service
@Singleton
@Slf4j
public class ConnectorsHealthUpdateService {
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired private BatchMainConfig mainConfig;
  @Autowired private AccountShardService accountShardService;

  public void update() {
    List<Account> accounts = accountShardService.getCeEnabledAccounts();
    log.info("accounts size: {}", accounts.size());
    for (Account account : accounts) {
      log.info("Fetching connectors for account name {}, account id {}", account.getAccountName(), account.getUuid());
      List<ConnectorResponseDTO> nextGenConnectorResponses = getNextGenConnectorResponses(account.getUuid());
      for (ConnectorResponseDTO connector : nextGenConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        try {
          processConnector(connector, account.getUuid());
        } catch (Exception e) {
          log.error("Exception processing Connector id: {} for account id: {}", connectorInfo.getIdentifier(), account.getUuid(), e);
        }
      }
    }
    return;
  }

  public void processConnector(ConnectorResponseDTO connector, String accountId) {
    log.info("connector.getConnector().getIdentifier(): {}, accountId: {}", connector.getConnector().getIdentifier(), accountId);
    ConnectorValidationResult connectorValidationResult =
        RestCallToNGManagerClientUtils.execute(connectorResourceClient.testConnectionInternal(
                connector.getConnector().getIdentifier(), accountId, null, null));
    log.info("connectorValidationResult {}", connectorValidationResult);
    return;
  }

  public List<ConnectorResponseDTO> getNextGenConnectorResponses(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.GCP_CLOUD_COST, ConnectorType.CE_AWS, ConnectorType.CE_AZURE))
            .ccmConnectorFilter(CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
            //.connectivityStatuses(Arrays.asList(ConnectivityStatus.SUCCESS))
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    do {
      response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectorResponses.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));
    log.info("Processing batch size of {}", nextGenConnectorResponses.size());
    return nextGenConnectorResponses;
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }
}

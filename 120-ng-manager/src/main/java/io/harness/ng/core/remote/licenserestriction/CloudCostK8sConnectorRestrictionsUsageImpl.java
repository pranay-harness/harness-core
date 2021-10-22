package io.harness.ng.core.remote.licenserestriction;

import static io.harness.delegate.beans.connector.ConnectorType.CE_KUBERNETES_CLUSTER;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.repositories.ConnectorCustomRepository;

import com.google.inject.Inject;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CE)
public class CloudCostK8sConnectorRestrictionsUsageImpl
    implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject private ConnectorCustomRepository connectorCustomRepository;

  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(ConnectorKeys.type).is(CE_KUBERNETES_CLUSTER.name());
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    return connectorCustomRepository.count(criteria);
  }
}

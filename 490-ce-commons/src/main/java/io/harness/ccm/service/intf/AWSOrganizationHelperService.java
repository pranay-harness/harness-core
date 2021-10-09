package io.harness.ccm.service.intf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import java.util.List;

@OwnedBy(HarnessTeam.CE)
public interface AWSOrganizationHelperService {
  List<CECloudAccount> getAWSAccounts(String accountId, String connectorId, CEAwsConnectorDTO ceAwsConnectorDTO,
      String awsAccessKey, String awsSecretKey);
}

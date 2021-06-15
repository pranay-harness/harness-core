package io.harness.ccm.service.intf;

import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import java.util.List;

public interface AWSOrganizationHelperService {
  List<CECloudAccount> getAWSAccounts(String accountId, String connectorId, CEAwsConnectorDTO ceAwsConnectorDTO,
      String awsAccessKey, String awsSecretKey);
}

package io.harness.ccm.commons.service.impl;

import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_ACCOUNT_FIELD;

import io.harness.ccm.commons.dao.CECloudAccountDao;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.service.intf.EntityMetadataService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityMetadataServiceImpl implements EntityMetadataService {
  @Inject private CECloudAccountDao cloudAccountDao;

  @Override
  public Map<String, String> getEntityIdToNameMapping(
      List<String> entityIds, String harnessAccountId, String fieldName) {
    if (AWS_ACCOUNT_FIELD.equals(fieldName)) {
      return getAccountNamePerAwsAccountId(entityIds, harnessAccountId);
    }
    return null;
  }

  @Override
  public Map<String, String> getAccountNamePerAwsAccountId(List<String> awsAccountIds, String harnessAccountId) {
    List<CECloudAccount> awsAccounts = cloudAccountDao.getByInfraAccountId(awsAccountIds, harnessAccountId);
    Map<String, String> accountIdToName = new HashMap<>();
    if (awsAccounts != null) {
      awsAccounts.forEach(ceCloudAccount
          -> accountIdToName.putIfAbsent(ceCloudAccount.getInfraAccountId(), ceCloudAccount.getAccountName()));
    }
    return accountIdToName;
  }
}

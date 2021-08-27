package io.harness.ccm.commons.service.impl;

import io.harness.ccm.commons.dao.CECloudAccountDao;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.service.intf.EntityMetadataService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityMetadataServiceImpl implements EntityMetadataService {
  @Inject private CECloudAccountDao cloudAccountDao;

  @Override
  public Map<String, String> getAccountNamePerAwsAccountId(List<String> awsAccountIds, String harnessAccountId) {
    List<CECloudAccount> awsAccounts = cloudAccountDao.getByInfraAccountId(awsAccountIds, harnessAccountId);
    return awsAccounts.stream().collect(
        Collectors.toMap(CECloudAccount::getInfraAccountId, CECloudAccount::getAccountName));
  }
}

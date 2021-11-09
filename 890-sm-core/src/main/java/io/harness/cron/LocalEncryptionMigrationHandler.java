package io.harness.cron;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
import io.harness.beans.LocalEncryptionMigrationInfo;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SortOrder;
import io.harness.ff.FeatureFlagService;
import io.harness.repositories.LocalEncryptionMigrationInfoRepository;
import io.harness.secrets.SecretsDao;
import io.harness.security.encryption.EncryptionType;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public class LocalEncryptionMigrationHandler implements Runnable {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private SecretsDao secretsDao;
  @Inject private LocalEncryptionMigrationInfoRepository localEncryptionMigrationInfoRepository;

  private final static String PAGE_SIZE = "1000";

  @Override
  public void run() {
    PageRequest<EncryptedData> pageRequest = initPageRequest();
    if (featureFlagService.isEnabled(FeatureName.LOCAL_MULTI_CRYPTO_MODE, "accountId")) {
      Optional<LocalEncryptionMigrationInfo> localEncryptionMigrationInfo =
          localEncryptionMigrationInfoRepository.findByAccountId("accountId");
      localEncryptionMigrationInfo.ifPresent(encryptionMigrationInfo
          -> pageRequest.addFilter(EncryptedDataKeys.createdAt, SearchFilter.Operator.GT,
              encryptionMigrationInfo.getLastMigratedRecordCreatedAtTimestamp()));

      PageResponse<EncryptedData> encryptedDataPageResponse = secretsDao.listSecrets(pageRequest);
    }
  }

  private PageRequest<EncryptedData> initPageRequest() {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    pageRequest.setLimit(PAGE_SIZE);
    pageRequest.setOffset("0");
    pageRequest.addFilter(EncryptedDataKeys.accountId, SearchFilter.Operator.EQ, "accountId");
    pageRequest.addFilter(EncryptedDataKeys.encryptionType, SearchFilter.Operator.EQ, EncryptionType.LOCAL);
    pageRequest.addOrder(EncryptedDataKeys.createdAt, SortOrder.OrderType.ASC);
    return pageRequest;
  }
}

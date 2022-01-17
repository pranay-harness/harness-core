/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.secret.SecretSanitizerThreadLocal;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManifestDelegateConfigHelper {
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private SecretDecryptionService decryptionService;
  @Inject private EncryptionService encryptionService;

  public void decryptManifestDelegateConfig(ManifestDelegateConfig manifestDelegateConfig) {
    if (manifestDelegateConfig == null) {
      return;
    }

    StoreDelegateConfig storeDelegateConfig = manifestDelegateConfig.getStoreDelegateConfig();
    switch (storeDelegateConfig.getType()) {
      case GIT:
        GitStoreDelegateConfig gitStoreDelegateConfig = (GitStoreDelegateConfig) storeDelegateConfig;
        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
        gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
        if (isNotEmpty(gitStoreDelegateConfig.getEncryptedDataDetails())) {
          Set<String> secrets = new HashSet<>();
          for (EncryptedDataDetail encryptedDataDetail : gitStoreDelegateConfig.getEncryptedDataDetails()) {
            secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedDataDetail, false)));
          }
          SecretSanitizerThreadLocal.addAll(secrets);
        }
        break;

      case HTTP_HELM:
        HttpHelmStoreDelegateConfig httpHelmStoreConfig = (HttpHelmStoreDelegateConfig) storeDelegateConfig;
        for (DecryptableEntity entity : httpHelmStoreConfig.getHttpHelmConnector().getDecryptableEntities()) {
          decryptionService.decrypt(entity, httpHelmStoreConfig.getEncryptedDataDetails());
        }
        if (isNotEmpty(httpHelmStoreConfig.getEncryptedDataDetails())) {
          Set<String> secrets = new HashSet<>();
          for (EncryptedDataDetail encryptedDataDetail : httpHelmStoreConfig.getEncryptedDataDetails()) {
            secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedDataDetail, false)));
          }
          SecretSanitizerThreadLocal.addAll(secrets);
        }
        break;

      case S3_HELM:
        S3HelmStoreDelegateConfig s3HelmStoreConfig = (S3HelmStoreDelegateConfig) storeDelegateConfig;
        List<DecryptableEntity> s3DecryptableEntityList = s3HelmStoreConfig.getAwsConnector().getDecryptableEntities();
        if (isNotEmpty(s3DecryptableEntityList)) {
          for (DecryptableEntity decryptableEntity : s3DecryptableEntityList) {
            decryptionService.decrypt(decryptableEntity, s3HelmStoreConfig.getEncryptedDataDetails());
          }
        }
        if (isNotEmpty(s3HelmStoreConfig.getEncryptedDataDetails())) {
          Set<String> secrets = new HashSet<>();
          for (EncryptedDataDetail encryptedDataDetail : s3HelmStoreConfig.getEncryptedDataDetails()) {
            secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedDataDetail, false)));
          }
          SecretSanitizerThreadLocal.addAll(secrets);
        }
        break;

      case GCS_HELM:
        GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig = (GcsHelmStoreDelegateConfig) storeDelegateConfig;
        List<DecryptableEntity> gcsDecryptableEntityList =
            gcsHelmStoreDelegateConfig.getGcpConnector().getDecryptableEntities();
        if (isNotEmpty(gcsDecryptableEntityList)) {
          for (DecryptableEntity decryptableEntity : gcsDecryptableEntityList) {
            decryptionService.decrypt(decryptableEntity, gcsHelmStoreDelegateConfig.getEncryptedDataDetails());
          }
        }
        if (isNotEmpty(gcsHelmStoreDelegateConfig.getEncryptedDataDetails())) {
          Set<String> secrets = new HashSet<>();
          for (EncryptedDataDetail encryptedDataDetail : gcsHelmStoreDelegateConfig.getEncryptedDataDetails()) {
            secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedDataDetail, false)));
          }
          SecretSanitizerThreadLocal.addAll(secrets);
        }
        break;

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Manifest type: [%s]", manifestDelegateConfig.getManifestType().name()));
    }
  }
}

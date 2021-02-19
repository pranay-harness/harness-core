package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.BatchProcessingException;
import io.harness.batch.processing.ccm.AzureStorageSyncRecord;
import io.harness.batch.processing.config.AzureStorageSyncConfig;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.intfc.AzureStorageSyncService;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction0;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

/**
 * Performs Azure container sync.
 */
@Slf4j
public class AzureStorageSyncServiceImpl implements AzureStorageSyncService {
  @Inject BatchMainConfig configuration;

  private static final int SYNC_TIMEOUT_MINUTES = 5;
  private static final String AZURE_STORAGE_SUFFIX = "blob.core.windows.net";
  private static final String AZURE_STORAGE_URL_FORMAT = "https://%s.%s";
  @Override
  public void syncContainer(AzureStorageSyncRecord azureStorageSyncRecord) {
    AzureStorageSyncConfig azureStorageSyncConfig = configuration.getAzureStorageSyncConfig();
    String sourcePath = null;
    String sourceSasToken = null;

    String destinationPath = null;
    String destinationSasToken = null;

    // Retry class config to retry aws commands
    RetryConfig config = RetryConfig.custom()
                             .maxAttempts(5)
                             .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                             .retryExceptions(TimeoutException.class, InterruptedException.class, IOException.class)
                             .build();
    RetryRegistry registry = RetryRegistry.of(config);
    Retry retry = registry.retry("azcopy", config);
    Retry.EventPublisher publisher = retry.getEventPublisher();
    publisher.onRetry(event -> log.info(event.toString()));
    publisher.onSuccess(event -> log.info(event.toString()));
    try {
      // generate SAS token for source
      sourceSasToken = genSasToken(azureStorageSyncRecord.getStorageAccountName(),
          azureStorageSyncRecord.getContainerName(), azureStorageSyncRecord.getTenantId(),
          azureStorageSyncConfig.getAzureAppClientId(), azureStorageSyncConfig.getAzureAppClientSecret(), false);
    } catch (Exception exception) {
      log.error("Error in generating sourceSasToken sas token", exception);
      throw exception;
    }
    try {
      destinationSasToken = azureStorageSyncConfig.getAzureSasToken();
      /* TODO: generate SAS token for destination
      destinationSasToken = genSasToken(azureStorageSyncConfig.getAzureStorageAccountName(),
              azureStorageSyncConfig.getAzureStorageContainerName(),
              azureStorageSyncConfig.getAzureTenantId(),
              azureStorageSyncConfig.getAzureAppClientId(),
              azureStorageSyncConfig.getAzureAppClientSecret(),
              true);
     */
    } catch (Exception exception) {
      log.error("Error in generating destinationSasToken sas token", exception);
      throw exception;
    }
    try {
      // Run the azcopy tool to do the sync
      String storageAccountUrl =
          String.format(AZURE_STORAGE_URL_FORMAT, azureStorageSyncRecord.getStorageAccountName(), AZURE_STORAGE_SUFFIX);
      sourcePath = String.join("/", storageAccountUrl, azureStorageSyncRecord.getContainerName(),
                       azureStorageSyncRecord.getDirectoryName())
          + "?" + sourceSasToken;
      storageAccountUrl = String.format(
          AZURE_STORAGE_URL_FORMAT, azureStorageSyncConfig.getAzureStorageAccountName(), AZURE_STORAGE_SUFFIX);
      destinationPath = String.join("/", storageAccountUrl, azureStorageSyncConfig.getAzureStorageContainerName(),
                            azureStorageSyncRecord.getAccountId(), azureStorageSyncRecord.getSettingId())
          + "?" + destinationSasToken;
      final ArrayList<String> cmd = Lists.newArrayList("azcopy", "sync", sourcePath, destinationPath, "--recursive");
      // TODO: Remove below info logging for security reasons
      log.info("azcopy sync cmd: {}", cmd);

      // Wrap azcopy sync with a retry mechanism.
      CheckedFunction0<ProcessResult> retryingAzcopySync =
          Retry.decorateCheckedSupplier(retry, () -> trySyncStorage(cmd));
      try {
        retryingAzcopySync.apply();
      } catch (Throwable throwable) {
        log.error("azcopy retries are exhausted");
        throw new BatchProcessingException("azcopy sync failed", throwable);
      }
      log.info("azcopy sync completed");

    } catch (InvalidExitValueException | JsonSyntaxException e) {
      log.error("Exception during azcopy sync for src={}, dest={}", sourcePath, destinationPath);
      throw new BatchProcessingException("azcopy sync failed", e);
    }
  }

  private String genSasToken(String storageAccountName, String containerName, String tenantId, String azureAppClientId,
      String azureAppClientSecret, boolean isHarnessAccount) {
    BlobContainerSasPermission blobContainerSasPermission =
        new BlobContainerSasPermission().setReadPermission(true).setListPermission(true);
    if (isHarnessAccount) {
      blobContainerSasPermission.setCreatePermission(true)
          .setAddPermission(true)
          .setWritePermission(true)
          .setExecutePermission(true);
    }
    BlobServiceSasSignatureValues builder =
        new BlobServiceSasSignatureValues(OffsetDateTime.now().plusHours(1), blobContainerSasPermission)
            .setProtocol(SasProtocol.HTTPS_ONLY);
    // Create a BlobServiceClient object which will be used to create a container client
    String endpoint = String.format(AZURE_STORAGE_URL_FORMAT, storageAccountName, AZURE_STORAGE_SUFFIX);
    log.info(endpoint);
    ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                                                        .clientId(azureAppClientId)
                                                        .clientSecret(azureAppClientSecret)
                                                        .tenantId(tenantId)
                                                        .build();

    BlobServiceClient blobServiceClient =
        new BlobServiceClientBuilder().endpoint(endpoint).credential(clientSecretCredential).buildClient();
    BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
    // Get a user delegation key for the Blob service that's valid for one hour.
    // You can use the key to generate any number of shared access signatures over the lifetime of the key.
    OffsetDateTime keyStart = OffsetDateTime.now();
    OffsetDateTime keyExpiry = OffsetDateTime.now().plusHours(1);
    UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(keyStart, keyExpiry);

    String sas = blobContainerClient.generateUserDelegationSas(builder, userDelegationKey);
    return sas;
  }

  public ProcessResult trySyncStorage(ArrayList<String> cmd)
      throws InterruptedException, TimeoutException, IOException {
    log.info("Running the azcopy sync command...");
    return getProcessExecutor()
        .command(cmd)
        .timeout(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .redirectError(Slf4jStream.of(log).asError())
        .redirectOutput(Slf4jStream.of(log).asInfo())
        .exitValue(0)
        .execute();
  }

  ProcessExecutor getProcessExecutor() {
    return new ProcessExecutor();
  }
}
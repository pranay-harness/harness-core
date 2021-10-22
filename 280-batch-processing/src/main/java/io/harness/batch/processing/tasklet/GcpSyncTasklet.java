package io.harness.batch.processing.tasklet;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.hazelcast.util.Preconditions.checkFalse;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class GcpSyncTasklet implements Tasklet {
  public static final String SERVICE_ACCOUNT = "serviceAccount";
  public static final String SOURCE_DATA_SET_ID = "sourceDataSetId";
  public static final String SOURCE_GCP_PROJECT_ID = "sourceGcpProjectId";
  public static final String SOURCE_DATA_SET_REGION = "sourceDataSetRegion";
  public static final String CONNECTOR_ID = "connectorId";
  public static final String ACCOUNT_ID = "accountId";
  public static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";
  public static final String TABLE_NAME = "sourceGcpTableName";
  @Autowired private BatchMainConfig mainConfig;
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  private JobParameters parameters;
  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Long endTime = Long.parseLong(parameters.getString(CCMJobConstants.JOB_END_DATE));
    BillingDataPipelineConfig billingDataPipelineConfig = mainConfig.getBillingDataPipelineConfig();

    if (billingDataPipelineConfig.isGcpSyncEnabled()) {
      List<ConnectorResponseDTO> nextGenGCPConnectorResponses = getNextGenGCPConnectorResponses(accountId);
      for (ConnectorResponseDTO connector : nextGenGCPConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
            (GcpCloudCostConnectorDTO) connectorInfo.getConnectorConfig();
        try {
          processGCPConnector(billingDataPipelineConfig, gcpCloudCostConnectorDTO.getServiceAccountEmail(),
              gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId(), gcpCloudCostConnectorDTO.getProjectId(),
              accountId, connectorInfo.getIdentifier(), endTime);
        } catch (Exception e) {
          log.error("Exception processing NG GCP Connector: {}", connectorInfo.getIdentifier(), e);
        }
      }

      List<GcpBillingAccount> gcpBillingAccounts =
          cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(accountId);
      log.info("Processing batch size of {} in GCP Sync Job for CG Connectors", gcpBillingAccounts.size());
      for (GcpBillingAccount gcpBillingAccount : gcpBillingAccounts) {
        GcpServiceAccount gcpServiceAccount = cloudToHarnessMappingService.getGcpServiceAccount(accountId);
        log.info("gcpServiceAccount.getEmail(): {}, gcpBillingAccount.getBqDatasetId(): {}, "
                + "gcpBillingAccount.getBqProjectId(): {}, "
                + "gcpBillingAccount.getUuid(): {}",
            gcpServiceAccount.getEmail(), gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(),
            gcpBillingAccount.getUuid());
        try {
          processGCPConnector(billingDataPipelineConfig, gcpServiceAccount.getEmail(),
              gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(), accountId,
              gcpBillingAccount.getUuid(), endTime);
        } catch (Exception e) {
          log.error("Exception processing CG GCP Connector: {}", gcpBillingAccount.getUuid(), e);
        }
      }
    }
    return null;
  }

  private void processGCPConnector(BillingDataPipelineConfig billingDataPipelineConfig, String serviceAccountEmail,
      String datasetId, String projectId, String accountId, String connectorId, Long endTime) {
    ServiceAccountCredentials sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, serviceAccountEmail);
    BigQuery bigQuery = BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
    DatasetId datasetIdFullyQualified = DatasetId.of(projectId, datasetId);
    Dataset dataset = bigQuery.getDataset(datasetIdFullyQualified);
    Page<Table> tableList = dataset.list(BigQuery.TableListOption.pageSize(1000));
    tableList.getValues().forEach(table -> {
      if (table.getTableId().getTable().contains(GCP_BILLING_EXPORT_V_1)) {
        TableId tableId = TableId.of(projectId, datasetId, table.getTableId().getTable());
        Table tableGranularData = bigQuery.getTable(tableId);

        Long lastModifiedTime = tableGranularData.getLastModifiedTime();
        lastModifiedTime = lastModifiedTime != null ? lastModifiedTime : table.getCreationTime();
        if (lastModifiedTime > endTime) {
          try {
            publishMessage(billingDataPipelineConfig.getGcpProjectId(),
                billingDataPipelineConfig.getGcpSyncPubSubTopic(), dataset.getLocation(), serviceAccountEmail,
                datasetId, projectId, accountId, connectorId, table.getTableId().getTable());
          } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Exception publishing Pub Sub Message: {}", e);
          }
        }
      }
    });
  }

  public List<ConnectorResponseDTO> getNextGenGCPConnectorResponses(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.GCP_CLOUD_COST))
            .ccmConnectorFilter(CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
            .connectivityStatuses(Arrays.asList(ConnectivityStatus.SUCCESS))
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
    log.info("Processing batch size of {} in GCP Sync Job", nextGenConnectorResponses.size());
    return nextGenConnectorResponses;
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }

  // read the credential path from env variables
  public static ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials credentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      log.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      log.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }

  public static Credentials getImpersonatedCredentials(
      ServiceAccountCredentials sourceCredentials, String impersonatedServiceAccount) {
    if (impersonatedServiceAccount == null) {
      return sourceCredentials;
    } else {
      return ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
    }
  }

  public static void publishMessage(String harnessProjectId, String topicId, String location,
      String serviceAccountEmail, String datasetId, String projectId, String accountId, String connectorId,
      String tableName) throws IOException, ExecutionException, InterruptedException {
    TopicName topicName = TopicName.of(harnessProjectId, topicId);
    Publisher publisher = null;

    try {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName).build();

      ImmutableMap<String, String> customAttributes = ImmutableMap.<String, String>builder()
                                                          .put(SERVICE_ACCOUNT, serviceAccountEmail)
                                                          .put(SOURCE_DATA_SET_ID, datasetId)
                                                          .put(SOURCE_GCP_PROJECT_ID, projectId)
                                                          .put(SOURCE_DATA_SET_REGION, location)
                                                          .put(ACCOUNT_ID, accountId)
                                                          .put(CONNECTOR_ID, connectorId)
                                                          .put(TABLE_NAME, tableName)
                                                          .build();
      ObjectMapper objectMapper = new ObjectMapper();
      String message = objectMapper.writeValueAsString(customAttributes);
      log.info("GCP Sync Pub Sub Event json: {}", message);
      log.info("Sending GCP Sync Pub Sub Event with data: {}", message);
      ByteString data = ByteString.copyFromUtf8(message);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      log.info("Published a message with custom attributes: " + messageId);
    } finally {
      if (publisher != null) {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown();
        publisher.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }
}

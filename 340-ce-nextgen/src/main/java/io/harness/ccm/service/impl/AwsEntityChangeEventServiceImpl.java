package io.harness.ccm.service.impl;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.commons.beans.config.AwsConfig;
import io.harness.ccm.commons.beans.config.GcpConfig;
import io.harness.ccm.commons.dao.AWSConnectorToBucketMappingDao;
import io.harness.ccm.commons.dao.CECloudAccountDao;
import io.harness.ccm.commons.entities.AWSConnectorToBucketMapping;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.service.intf.AWSBucketPolicyHelperService;
import io.harness.ccm.service.intf.AWSOrganizationHelperService;
import io.harness.ccm.service.intf.AwsEntityChangeEventService;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.organizations.model.AWSOrganizationsNotInUseException;
import com.amazonaws.services.organizations.model.AccessDeniedException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class AwsEntityChangeEventServiceImpl implements AwsEntityChangeEventService {
  public static final String ACCOUNT_ID = "accountId";
  public static final String AWS_INFRA_ACCOUNT_ID = "awsInfraAccountId";
  public static final String AWS_CROSS_ACCOUNT_EXTERNAL_ID = "awsCrossAccountExternalId";
  public static final String AWS_CROSS_ACCOUNT_ROLE_ARN = "awsCrossAccountRoleArn";
  public static final String CONNECTOR_ID = "connectorId";
  public static final String ACTION = "action";
  @Inject ConnectorResourceClient connectorResourceClient;
  @Inject AWSOrganizationHelperService awsOrganizationHelperService;
  @Inject AWSBucketPolicyHelperService awsBucketPolicyHelperService;
  @Inject CENextGenConfiguration configuration;
  @Inject CECloudAccountDao cloudAccountDao;
  @Inject AwsClient awsClient;
  @Inject AWSConnectorToBucketMappingDao awsConnectorToBucketMappingDao;

  @Override
  public boolean processAWSEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    AwsConfig awsConfig = configuration.getAwsConfig();
    CEAwsConnectorDTO ceAwsConnectorDTO;
    String destinationBucket = getDestinationBucketName(awsConfig);
    ArrayList<ImmutableMap<String, String>> entityChangeEvents = new ArrayList<>();
    log.info("processAWSEntityChangeEvent action: {}", action);
    switch (action) {
      case CREATE_ACTION:
        ceAwsConnectorDTO =
            (CEAwsConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
        // Update Bucket Policy
        if (isBillingFeatureEnabled(ceAwsConnectorDTO) && validateResourcesExists(awsConfig, ceAwsConnectorDTO)) {
          awsConnectorToBucketMappingDao.upsert(AWSConnectorToBucketMapping.builder()
                                                    .accountId(accountIdentifier)
                                                    .awsConnectorIdentifier(identifier)
                                                    .destinationBucket(destinationBucket)
                                                    .build());
          awsBucketPolicyHelperService.updateBucketPolicy(
              ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(), destinationBucket,
              awsConfig.getAccessKey(), awsConfig.getSecretKey());
        }
        if (isVisibilityFeatureEnabled(ceAwsConnectorDTO)) {
          updateEventData(action, identifier, accountIdentifier,
              ceAwsConnectorDTO.getCrossAccountAccess().getExternalId(),
              ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(), ceAwsConnectorDTO.getAwsAccountId(),
              entityChangeEvents);
          publishMessage(entityChangeEvents);
        }
        log.info("CEAwsConnectorDTO: {}", ceAwsConnectorDTO);
        List<CECloudAccount> awsAccounts = new ArrayList<>();
        try {
          awsAccounts = awsOrganizationHelperService.getAWSAccounts(
              accountIdentifier, identifier, ceAwsConnectorDTO, awsConfig.getAccessKey(), awsConfig.getSecretKey());
          log.info("Number of AWS Accounts: {}, Not Processing Create AWS Account Metadata", awsAccounts.size());
        } catch (AWSOrganizationsNotInUseException ex) {
          log.info("AWSOrganizationsNotInUseException for AWS Connector: {}", ceAwsConnectorDTO.getAwsAccountId(), ex);
        } catch (AccessDeniedException accessDeniedException) {
          log.info("AccessDeniedException for AWS Connector: {}, Not Processing Create AWS Account Metadata",
              ceAwsConnectorDTO.getAwsAccountId(), accessDeniedException);
        } catch (Exception ex) {
          log.info("Exception for AWS Connector:, {}, Not Processing Create AWS Account Metadata",
              ceAwsConnectorDTO.getAwsAccountId(), ex);
        }
        for (CECloudAccount account : awsAccounts) {
          log.info("Inserting CECloudAccount: {}", account);
          cloudAccountDao.create(account);
        }
        break;
      case UPDATE_ACTION:
        ceAwsConnectorDTO =
            (CEAwsConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
        // Update Bucket Policy
        if (isBillingFeatureEnabled(ceAwsConnectorDTO) && validateResourcesExists(awsConfig, ceAwsConnectorDTO)) {
          awsConnectorToBucketMappingDao.upsert(AWSConnectorToBucketMapping.builder()
                                                    .accountId(accountIdentifier)
                                                    .awsConnectorIdentifier(identifier)
                                                    .destinationBucket(destinationBucket)
                                                    .build());
          awsBucketPolicyHelperService.updateBucketPolicy(
              ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(), destinationBucket,
              awsConfig.getAccessKey(), awsConfig.getSecretKey());
        }
        if (isVisibilityFeatureEnabled(ceAwsConnectorDTO)) {
          updateEventData(action, identifier, accountIdentifier,
              ceAwsConnectorDTO.getCrossAccountAccess().getExternalId(),
              ceAwsConnectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(), ceAwsConnectorDTO.getAwsAccountId(),
              entityChangeEvents);
          publishMessage(entityChangeEvents);
        }
        break;
      case DELETE_ACTION:
        updateEventData(action, identifier, accountIdentifier, "", "", "", entityChangeEvents);
        publishMessage(entityChangeEvents);
        break;
      default:
        log.info("Not processing AWS Event, action: {}, entityChangeDTO: {}", action, entityChangeDTO);
    }
    return true;
  }

  private void updateEventData(String action, String identifier, String accountIdentifier,
      String awsCrossAccountExternalId, String awsCrossAccountRoleArn, String awsInfraAccountId,
      ArrayList<ImmutableMap<String, String>> entityChangeEvents) {
    log.info("Visibility feature is enabled. Prepping event for pubsub");
    entityChangeEvents.add(ImmutableMap.<String, String>builder()
                               .put(ACTION, action)
                               .put(ACCOUNT_ID, accountIdentifier)
                               .put(AWS_INFRA_ACCOUNT_ID, awsInfraAccountId)
                               .put(AWS_CROSS_ACCOUNT_EXTERNAL_ID, awsCrossAccountExternalId)
                               .put(AWS_CROSS_ACCOUNT_ROLE_ARN, awsCrossAccountRoleArn)
                               .put(CONNECTOR_ID, identifier)
                               .build());
  }

  private String getDestinationBucketName(AwsConfig awsConfig) {
    return String.format("%s-%s", awsConfig.getDestinationBucket(), awsConfig.getDestinationBucketsCount());
  }

  public ConnectorInfoDTO getConnectorConfigDTO(String accountIdentifier, String connectorIdentifierRef) {
    try {
      Optional<ConnectorDTO> connectorDTO =
          NGRestUtils.getResponse(connectorResourceClient.get(connectorIdentifierRef, accountIdentifier, null, null));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }

      return connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }

  private boolean isBillingFeatureEnabled(CEAwsConnectorDTO ceAwsConnectorDTO) {
    List<CEFeatures> featuresEnabled = ceAwsConnectorDTO.getFeaturesEnabled();
    return featuresEnabled.contains(CEFeatures.BILLING);
  }

  private boolean isVisibilityFeatureEnabled(CEAwsConnectorDTO ceAwsConnectorDTO) {
    List<CEFeatures> featuresEnabled = ceAwsConnectorDTO.getFeaturesEnabled();
    return featuresEnabled.contains(CEFeatures.VISIBILITY);
  }

  private boolean validateResourcesExists(AwsConfig awsConfig, CEAwsConnectorDTO ceAwsConnectorDTO) {
    AWSCredentialsProvider credentialProvider =
        getCredentialProvider(ceAwsConnectorDTO.getCrossAccountAccess(), awsConfig);
    AwsCurAttributesDTO curAttributes = ceAwsConnectorDTO.getCurAttributes();
    // Report Exists
    Optional<ReportDefinition> report =
        awsClient.getReportDefinition(credentialProvider, curAttributes.getReportName());
    if (!report.isPresent()) {
      return false;
    }
    // S3 Bucket Exists
    try {
      ObjectListing s3BucketObject =
          awsClient.getBucket(credentialProvider, curAttributes.getS3BucketName(), curAttributes.getS3Prefix());
      if (CollectionUtils.isEmpty(s3BucketObject.getObjectSummaries())) {
        return false;
      }
    } catch (AmazonS3Exception ex) {
      return false;
    }

    return true;
  }

  public AWSCredentialsProvider getCredentialProvider(
      CrossAccountAccessDTO crossAccountAccessDTO, AwsConfig awsConfig) {
    final AWSCredentialsProvider staticBasicAwsCredentials =
        awsClient.constructStaticBasicAwsCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    final AWSCredentialsProvider credentialsProvider =
        awsClient.getAssumedCredentialsProvider(staticBasicAwsCredentials,
            crossAccountAccessDTO.getCrossAccountRoleArn(), crossAccountAccessDTO.getExternalId());
    credentialsProvider.getCredentials();
    return credentialsProvider;
  }

  public void publishMessage(ArrayList<ImmutableMap<String, String>> entityChangeEvents) {
    if (entityChangeEvents.isEmpty()) {
      log.info("Visibility is not enabled. Not sending event");
      return;
    }
    GcpConfig gcpConfig = configuration.getGcpConfig();
    String harnessGcpProjectId = gcpConfig.getGcpProjectId();
    String inventoryPubSubTopic = gcpConfig.getGcpAwsConnectorCrudPubSubTopic();
    TopicName topicName = TopicName.of(harnessGcpProjectId, inventoryPubSubTopic);
    Publisher publisher = null;
    log.info("Publishing event to topic: {}", topicName);
    try {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName).build();
      ObjectMapper objectMapper = new ObjectMapper();
      String message = objectMapper.writeValueAsString(entityChangeEvents);
      ByteString data = ByteString.copyFromUtf8(message);
      log.info("Sending event with data: {}", data);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      log.info("Published event with data: {}, messageId: {}", message, messageId);
    } catch (Exception e) {
      log.error("Error occurred while sending event in pubsub\n", e);
    }

    if (publisher != null) {
      // When finished with the publisher, shutdown to free up resources.
      publisher.shutdown();
      try {
        publisher.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        log.error("Error occurred while terminating pubsub publisher\n", e);
      }
    }
  }
}

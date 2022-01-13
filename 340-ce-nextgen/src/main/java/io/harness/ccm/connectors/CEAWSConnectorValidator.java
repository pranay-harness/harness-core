/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.connectors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.ErrorDetail;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEAWSConnectorValidator extends io.harness.ccm.connectors.AbstractCEConnectorValidator {
  private final String COMPRESSION = "GZIP";
  private final String TIME_GRANULARITY = "HOURLY";
  private final String REPORT_VERSIONING = "OVERWRITE_REPORT";
  private final String RESOURCES = "RESOURCES";

  private final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{} connectorIdentifier:{}";
  private String lastErrorSummary = "Some of the permissions were missing.";

  @Inject AwsClient awsClient;
  @Inject CENextGenConfiguration configuration;
  @Inject CEConnectorsHelper ceConnectorsHelper;

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier) {
    final CEAwsConnectorDTO ceAwsConnectorDTO =
        (CEAwsConnectorDTO) connectorResponseDTO.getConnector().getConnectorConfig();
    final List<CEFeatures> featuresEnabled = ceAwsConnectorDTO.getFeaturesEnabled();
    final CrossAccountAccessDTO crossAccountAccessDTO = ceAwsConnectorDTO.getCrossAccountAccess();
    final AwsCurAttributesDTO awsCurAttributesDTO = ceAwsConnectorDTO.getCurAttributes();
    String projectIdentifier = connectorResponseDTO.getConnector().getProjectIdentifier();
    String orgIdentifier = connectorResponseDTO.getConnector().getOrgIdentifier();
    String connectorIdentifier = connectorResponseDTO.getConnector().getIdentifier();

    final List<ErrorDetail> errorList = new ArrayList<>();

    try {
      final AWSCredentialsProvider credentialsProvider = getCredentialProvider(crossAccountAccessDTO);

      if (featuresEnabled.contains(CEFeatures.VISIBILITY)) {
        final Policy eventsPolicy = getRequiredEventsPolicy();
        errorList.addAll(validateIfPolicyIsCorrect(
            credentialsProvider, crossAccountAccessDTO.getCrossAccountRoleArn(), CEFeatures.VISIBILITY, eventsPolicy));
      }

      if (featuresEnabled.contains(CEFeatures.OPTIMIZATION)) {
        final Policy optimizationPolicy = getRequiredOptimizationPolicy();
        errorList.addAll(validateIfPolicyIsCorrect(credentialsProvider, crossAccountAccessDTO.getCrossAccountRoleArn(),
            CEFeatures.OPTIMIZATION, optimizationPolicy));
      }

      if (featuresEnabled.contains(CEFeatures.BILLING)) {
        log.info("Destination bucket: {}", configuration.getAwsConfig().getDestinationBucket());
        final Policy curPolicy = getRequiredCurPolicy(
            awsCurAttributesDTO.getS3BucketName(), configuration.getAwsConfig().getDestinationBucket());
        errorList.addAll(validateIfPolicyIsCorrect(
            credentialsProvider, crossAccountAccessDTO.getCrossAccountRoleArn(), CEFeatures.BILLING, curPolicy));

        errorList.addAll(validateResourceExists(credentialsProvider, awsCurAttributesDTO, errorList));
      }
    } catch (AWSSecurityTokenServiceException ex) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(ErrorDetail.builder()
                                       .code(ex.getStatusCode())
                                       .reason(ex.getErrorCode())
                                       .message(ex.getErrorMessage())
                                       .build()))
          .errorSummary("Either the " + crossAccountAccessDTO.getCrossAccountRoleArn()
              + " doesn't exist or Harness isn't a trusted entity on it or wrong externalId.")
          .testedAt(Instant.now().toEpochMilli())
          .build();
    } catch (AmazonIdentityManagementException ex) {
      // assuming only one possible reason for AmazonIdentityManagementException here
      return ConnectorValidationResult.builder()
          .errors(Collections.singletonList(ErrorDetail.builder()
                                                .code(ex.getStatusCode())
                                                .message(ex.getErrorCode())
                                                .reason(ex.getErrorMessage())
                                                .build()))
          .errorSummary("Please allow " + crossAccountAccessDTO.getCrossAccountRoleArn()
              + " to perform 'iam:SimulatePrincipalPolicy' on itself")
          .status(ConnectivityStatus.FAILURE)
          .build();
    } catch (InvalidArgumentsException ex) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(ex.getMessage())
          .testedAt(Instant.now().toEpochMilli())
          .build();
    } catch (Exception ex) {
      // These are unknown errors, they should be identified over time and parsed correctly
      log.error(GENERIC_LOGGING_ERROR, accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, ex);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(ex.getMessage())
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }

    if (!errorList.isEmpty()) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(errorList)
          .errorSummary(lastErrorSummary)
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
    // Check for data at destination only when 24 hrs have elapsed since connector last modified at
    long now = Instant.now().toEpochMilli() - 24 * 60 * 60 * 1000;
    if (featuresEnabled.contains(CEFeatures.BILLING) && connectorResponseDTO.getLastModifiedAt() < now) {
      if (!ceConnectorsHelper.isDataSyncCheck(accountIdentifier, connectorIdentifier, ConnectorType.CE_AWS,
              ceConnectorsHelper.JOB_TYPE_CLOUDFUNCTION)) {
        // Issue with CFs
        return ConnectorValidationResult.builder()
            .errorSummary("Error with processing data. Please contact Harness support")
            .status(ConnectivityStatus.FAILURE)
            .build();
      }
    }
    log.info("Validation successful for connector {}", connectorIdentifier);
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  private Collection<ErrorDetail> validateIfPolicyIsCorrect(AWSCredentialsProvider credentialsProvider,
      String crossAccountRoleArn, CEFeatures feature, @NotNull Policy policy) {
    List<ErrorDetail> errorDetails = new ArrayList<>();

    for (Statement statement : policy.getStatements()) {
      List<String> actions = statement.getActions().stream().map(Action::getActionName).collect(Collectors.toList());
      List<String> resources = statement.getResources().stream().map(Resource::getId).collect(Collectors.toList());

      List<EvaluationResult> evaluationResults =
          awsClient.simulatePrincipalPolicy(credentialsProvider, crossAccountRoleArn, actions, resources)
              .stream()
              .filter(x -> !"allowed".equals(x.getEvalDecision()))
              .collect(Collectors.toList());

      for (EvaluationResult result : evaluationResults) {
        errorDetails.add(ErrorDetail.builder()
                             .reason(result.getEvalDecision())
                             .message("Action: " + result.getEvalActionName()
                                 + " not allowed on Resource: " + result.getEvalResourceName())
                             .code(403)
                             .build());
      }
    }
    return errorDetails;
  }

  @VisibleForTesting
  public AWSCredentialsProvider getCredentialProvider(CrossAccountAccessDTO crossAccountAccessDTO) {
    final AWSCredentialsProvider BasicAwsCredentials = awsClient.constructStaticBasicAwsCredentials(
        configuration.getAwsConfig().getAccessKey(), configuration.getAwsConfig().getSecretKey());
    final AWSCredentialsProvider credentialsProvider = awsClient.getAssumedCredentialsProvider(
        BasicAwsCredentials, crossAccountAccessDTO.getCrossAccountRoleArn(), crossAccountAccessDTO.getExternalId());
    credentialsProvider.getCredentials();
    return credentialsProvider;
  }

  private Collection<ErrorDetail> validateResourceExists(AWSCredentialsProvider credentialsProvider,
      AwsCurAttributesDTO awsCurAttributesDTO, final List<ErrorDetail> errorList) {
    Optional<ReportDefinition> report =
        awsClient.getReportDefinition(credentialsProvider, awsCurAttributesDTO.getReportName());
    if (!report.isPresent()) {
      return ImmutableList.of(
          ErrorDetail.builder()
              .message(String.format("Can't access cost and usage report: %s", awsCurAttributesDTO.getReportName()))
              .reason("Report Not Present")
              .build());
    }
    validateReport(report.get(), awsCurAttributesDTO.getS3BucketName(), errorList);
    String s3PathPrefix = report.get().getS3Prefix() + "/" + awsCurAttributesDTO.getReportName() + "/"
        + ceConnectorsHelper.getReportMonth();
    return validateIfBucketIsPresent(credentialsProvider, awsCurAttributesDTO.getS3BucketName(), s3PathPrefix);
  }

  private void validateReport(
      @NotNull ReportDefinition report, @NotNull String s3BucketName, final List<ErrorDetail> errorList) {
    if (!report.getS3Bucket().equals(s3BucketName)) {
      lastErrorSummary = String.format("Provided s3Bucket Name: %s, Actual s3bucket associated with the report: %s",
          s3BucketName, report.getS3Bucket());
      errorList.add(ErrorDetail.builder().reason(lastErrorSummary).message("Wrong s3Bucket Name").build());
    }
    if (!report.getCompression().equals(COMPRESSION)) {
      errorList.add(ErrorDetail.builder()
                        .reason(String.format("Required: %s, Actual: %s", COMPRESSION, report.getCompression()))
                        .message("Wrong Compression")
                        .build());
    }
    if (!report.getTimeUnit().equals(TIME_GRANULARITY)) {
      errorList.add(ErrorDetail.builder()
                        .reason(String.format("Required: %s, Actual: %s", TIME_GRANULARITY, report.getTimeUnit()))
                        .message("Wrong Time Granularity")
                        .build());
    }
    if (!report.getReportVersioning().equals(REPORT_VERSIONING)) {
      errorList.add(
          ErrorDetail.builder()
              .reason(String.format("Required: %s, Actual: %s", REPORT_VERSIONING, report.getReportVersioning()))
              .message("Wrong Report versioning")
              .build());
    }
    if (!report.isRefreshClosedReports()) {
      errorList.add(
          ErrorDetail.builder()
              .reason(
                  "Required: Automatically refresh your Cost & Usage Report when charges are detected for previous months with closed bills.")
              .message("Wrong Data refresh settings")
              .build());
    }
    if (!report.getAdditionalSchemaElements().contains(RESOURCES)) {
      errorList.add(ErrorDetail.builder()
                        .reason("Required: Include resource IDs")
                        .message("Wrong Additional report details")
                        .build());
    }
  }

  @VisibleForTesting
  public Collection<ErrorDetail> validateIfBucketIsPresent(
      AWSCredentialsProvider credentialsProvider, String s3BucketName, String s3PathPrefix) {
    Date latestFileLastmodifiedTime = Date.from(Instant.EPOCH);
    String latestFileName = "";
    try {
      ObjectListing s3BucketObject = awsClient.getBucket(credentialsProvider, s3BucketName, s3PathPrefix);
      // Caveat: This can be slow for some accounts.
      for (S3ObjectSummary objectSummary : s3BucketObject.getObjectSummaries()) {
        if (objectSummary.getKey().endsWith(".csv.gz")) {
          if (objectSummary.getLastModified().compareTo(latestFileLastmodifiedTime) > 0) {
            latestFileLastmodifiedTime = objectSummary.getLastModified();
            latestFileName = objectSummary.getKey();
          }
        }
      }
      log.info("Latest .csv.gz file in {}/{} latestFileName: {} latestFileLastmodifiedTime: {}", s3BucketName,
          s3PathPrefix, latestFileName, latestFileLastmodifiedTime);
      long now = Instant.now().toEpochMilli() - 24 * 60 * 60 * 1000;
      if (!latestFileName.isEmpty() && latestFileLastmodifiedTime.getTime() < now) {
        lastErrorSummary = String.format("No CUR file is found in last 24 hrs at %s/%s. "
                + "Please verify your billing export config in your AWS account and CCM connector. "
                + "Follow CCM documentation for more information",
            s3BucketName, s3PathPrefix);
        return ImmutableList.of(
            ErrorDetail.builder().message("No CUR file is found in last 24 hrs").reason(lastErrorSummary).build());
      }
    } catch (AmazonS3Exception ex) {
      lastErrorSummary = String.format(
          "Either bucket '%s' doesn't exist or there is a mismatch between bucketName entered in connector and the name present in the role policy.",
          s3BucketName);
      return ImmutableList.of(ErrorDetail.builder().message(ex.getMessage()).reason(lastErrorSummary).build());
    }
    return Collections.emptyList();
  }

  private Policy getRequiredOptimizationPolicy() {
    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Effect\": \"Allow\","
        + "      \"Action\": ["
        + "        \"elasticloadbalancing:*\","
        + "        \"ec2:StopInstances\","
        + "        \"autoscaling:*\","
        + "        \"ec2:Describe*\","
        + "        \"iam:CreateServiceLinkedRole\","
        + "        \"iam:ListInstanceProfiles\","
        + "        \"iam:ListInstanceProfilesForRole\","
        + "        \"iam:AddRoleToInstanceProfile\","
        + "        \"iam:PassRole\","
        + "        \"ec2:StartInstances\","
        + "        \"ec2:*\","
        + "        \"iam:GetUser\","
        + "        \"ec2:ModifyInstanceAttribute\","
        + "        \"iam:ListRoles\","
        + "        \"acm:ListCertificates\","
        + "        \"lambda:*\","
        + "        \"cloudwatch:ListMetrics\","
        + "        \"cloudwatch:GetMetricData\","
        + "        \"route53:GetHostedZone\","
        + "        \"route53:ListHostedZones\","
        + "        \"route53:ListHostedZonesByName\","
        + "        \"route53:ChangeResourceRecordSets\","
        + "        \"route53:ListResourceRecordSets\","
        + "        \"route53:GetHealthCheck\","
        + "        \"route53:GetHealthCheckStatus\","
        + "        \"cloudwatch:GetMetricStatistics\""
        + "      ],"
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";
    return Policy.fromJson(policyDocument);
  }

  private Policy getRequiredCurPolicy(final String customerBucketName, final String destinationBucketName) {
    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Action\": ["
        + "        \"s3:GetBucketLocation\","
        + "        \"s3:ListBucket\","
        + "        \"s3:GetObject\""
        + "      ],"
        + "      \"Resource\": ["
        + "        \"arn:aws:s3:::" + customerBucketName + "\","
        + "        \"arn:aws:s3:::" + customerBucketName + "/*\""
        + "      ],"
        + "      \"Effect\": \"Allow\","
        + "      \"Sid\": \"harnessCustomerS3Policy20200505\""
        + "    },"
        + "    {"
        + "      \"Action\": ["
        + "        \"s3:ListBucket\","
        + "        \"s3:PutObject\","
        + "        \"s3:PutObjectAcl\""
        + "      ],"
        + "      \"Resource\": ["
        + "        \"arn:aws:s3:::" + destinationBucketName + "\","
        + "        \"arn:aws:s3:::" + destinationBucketName + "/*\""
        + "      ],"
        + "      \"Effect\": \"Allow\","
        + "      \"Sid\": \"harnessS3Policy20200505\""
        + "    }"
        + "  ]"
        + "}";
    return Policy.fromJson(policyDocument);
  }

  private Policy getRequiredEventsPolicy() {
    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Sid\": \"VisualEditor0\","
        + "      \"Effect\": \"Allow\","
        + "      \"Action\": ["
        + "        \"ecs:ListClusters*\","
        + "        \"ecs:ListServices\","
        + "        \"ecs:DescribeServices\","
        + "        \"ecs:DescribeContainerInstances\","
        + "        \"ecs:ListTasks\","
        + "        \"ecs:ListContainerInstances\","
        + "        \"ecs:DescribeTasks\","
        + "        \"ec2:DescribeInstances*\","
        + "        \"ec2:DescribeRegions\","
        + "        \"cloudwatch:GetMetricData\","
        + "        \"ec2:DescribeVolumes\","
        + "        \"ec2:DescribeSnapshots\""
        + "      ],"
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";
    return Policy.fromJson(policyDocument);
  }
}

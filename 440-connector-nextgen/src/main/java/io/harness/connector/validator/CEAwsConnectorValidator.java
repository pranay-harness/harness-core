package io.harness.connector.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.remote.CEAwsSetupConfig;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public class CEAwsConnectorValidator extends AbstractConnectorValidator {
  private static final String COMPRESSION = "GZIP";
  private static final String TIME_GRANULARITY = "HOURLY";
  private static final String REPORT_VERSIONING = "OVERWRITE_REPORT";
  private static final String RESOURCES = "RESOURCES";

  private static final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{}";
  private String lastErrorSummary = "Some of the permissions were missing.";

  @Inject private AwsClient awsClient;
  @Inject private CEAwsSetupConfig ceAwsSetupConfig;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorDTO;
    final List<CEFeatures> featuresEnabled = ceAwsConnectorDTO.getFeaturesEnabled();
    final CrossAccountAccessDTO crossAccountAccessDTO = ceAwsConnectorDTO.getCrossAccountAccess();
    final AwsCurAttributesDTO awsCurAttributesDTO = ceAwsConnectorDTO.getCurAttributes();

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
        log.info("Destination bucket: {}", ceAwsSetupConfig.getDestinationBucket());
        final Policy curPolicy =
            getRequiredCurPolicy(awsCurAttributesDTO.getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());
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
      log.error(GENERIC_LOGGING_ERROR, accountIdentifier, orgIdentifier, projectIdentifier, ex);
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
    final AWSCredentialsProvider staticBasicAwsCredentials =
        awsClient.constructStaticBasicAwsCredentials(ceAwsSetupConfig.getAccessKey(), ceAwsSetupConfig.getSecretKey());
    final AWSCredentialsProvider credentialsProvider =
        awsClient.getAssumedCredentialsProvider(staticBasicAwsCredentials,
            crossAccountAccessDTO.getCrossAccountRoleArn(), crossAccountAccessDTO.getExternalId());
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
              .message(String.format("Can't access Report: %s", awsCurAttributesDTO.getReportName()))
              .reason("Report Not Present")
              .build());
    }
    validateReport(report.get(), awsCurAttributesDTO.getS3BucketName(), errorList);
    S3BucketDetails s3BucketDetails = S3BucketDetails.builder()
                                          .s3BucketName(awsCurAttributesDTO.getS3BucketName())
                                          .s3Prefix(report.get().getS3Prefix())
                                          .region(report.get().getS3Region())
                                          .s3BucketName(awsCurAttributesDTO.getS3BucketName())
                                          .build();
    return validateIfBucketIsPresent(credentialsProvider, s3BucketDetails);
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

  private Collection<ErrorDetail> validateIfBucketIsPresent(
      AWSCredentialsProvider credentialsProvider, S3BucketDetails s3BucketDetails) {
    try {
      awsClient.getBucket(credentialsProvider, s3BucketDetails.getS3BucketName(), s3BucketDetails.getS3Prefix());
    } catch (AmazonS3Exception ex) {
      lastErrorSummary = String.format(
          "Either bucket '%s' doesn't exist or, %nthere is a mismatch between bucketName entered in connector and the name present in the role policy.",
          s3BucketDetails.getS3BucketName());
      return ImmutableList.of(ErrorDetail.builder().message(ex.getMessage()).reason(lastErrorSummary).build());
    }
    return Collections.emptyList();
  }

  private static Policy getRequiredOptimizationPolicy() {
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

  private static Policy getRequiredCurPolicy(final String customerBucketName, final String destinationBucketName) {
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

  private static Policy getRequiredEventsPolicy() {
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
package io.harness.connector.validator;

import io.harness.aws.AwsClient;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.remote.CEAwsSetupConfig;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Singleton
public class CEAwsConnectorValidator extends AbstractConnectorValidator {
  private static final String COMPRESSION = "GZIP";
  private static final String TIME_GRANULARITY = "HOURLY";
  private static final String REPORT_VERSIONING = "OVERWRITE_REPORT";
  private static final String RESOURCES = "RESOURCES";

  private static final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{}";

  private static final String ERROR_MSG = "";
  private static final String DELIMITER = "~";

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
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorDTO;
    final List<CEAwsFeatures> featuresEnabled = ceAwsConnectorDTO.getFeaturesEnabled();
    final CrossAccountAccessDTO crossAccountAccessDTO = ceAwsConnectorDTO.getCrossAccountAccess();
    final AwsCurAttributesDTO awsCurAttributesDTO = ceAwsConnectorDTO.getCurAttributes();

    final List<ErrorDetail> errorList = new ArrayList<>();

    try {
      final AWSCredentialsProvider credentialsProvider = getCredentialProvider(crossAccountAccessDTO);
      final HashSet<String> allPermissions =
          getAllPermissions(credentialsProvider, crossAccountAccessDTO.getCrossAccountRoleArn());

      if (featuresEnabled.contains(CEAwsFeatures.CUR)) {
        final Policy curPolicy =
            getCurPolicy(awsCurAttributesDTO.getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());
        errorList.addAll(validateIfPolicyIsCorrect(CEAwsFeatures.CUR, curPolicy, allPermissions));

        errorList.addAll(validateResourceExists(credentialsProvider, awsCurAttributesDTO, errorList));
      }

      if (featuresEnabled.contains(CEAwsFeatures.EVENTS)) {
        final Policy eventsPolicy = getEventsPolicy();
        errorList.addAll(validateIfPolicyIsCorrect(CEAwsFeatures.EVENTS, eventsPolicy, allPermissions));
      }

      if (featuresEnabled.contains(CEAwsFeatures.OPTIMIZATION)) {
        final Policy optimizationPolicy = getOptimizationPolicy();
        errorList.addAll(validateIfPolicyIsCorrect(CEAwsFeatures.OPTIMIZATION, optimizationPolicy, allPermissions));
      }
    } catch (AWSSecurityTokenServiceException ex) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(
              ErrorDetail.builder()
                  .code(ex.getStatusCode())
                  .reason(MessageFormat.format(
                      "Either the {0} doesn't exist or Harness isn't a trusted entity on it or wrong externalId.",
                      crossAccountAccessDTO.getCrossAccountRoleArn()))
                  .message(ex.getErrorMessage())
                  .build()))
          .errorSummary(ex.getMessage())
          .testedAt(Instant.now().toEpochMilli())
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
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }

    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  /**
   * @param policy The policy to validate
   * @param allPermissions All the allowed permissions present in the roleName, in the format 'Action~Resource'
   */
  private List<ErrorDetail> validateIfPolicyIsCorrect(
      CEAwsFeatures feature, @NotNull Policy policy, @NotNull HashSet<String> allPermissions) {
    HashSet<String> requiredPermission = getAllPossibleCombination(policy.getStatements());
    List<ErrorDetail> errorDetails = new ArrayList<>();
    for (String eachPermission : requiredPermission) {
      if (!allPermissions.contains(eachPermission)) {
        String[] splitPermission = eachPermission.split(DELIMITER);
        errorDetails.add(ErrorDetail.builder()
                             .reason(MessageFormat.format(
                                 "{0}: {1} not allowed on {2}", feature.name(), splitPermission[0], splitPermission[1]))
                             .message("")
                             .code(403)
                             .build());
      }
    }

    return errorDetails;
  }

  private HashSet<String> getAllPermissions(AWSCredentialsProvider credentialsProvider, String crossAccountRoleArn) {
    final String roleName = getRoleName(crossAccountRoleArn);

    try {
      // aws iam list-role-policies --role-name harnessCERole
      List<String> policies = awsClient.listRolePolicyNames(credentialsProvider, roleName);
      final HashSet<String> permissions = new HashSet<>();

      for (String policyName : policies) {
        // aws iam get-role-policy --role-name harnessCERole --policy-name harnessCustomS3Policy
        final Collection<Statement> statementList =
            awsClient.getRolePolicy(credentialsProvider, roleName, policyName).getStatements();
        permissions.addAll(getAllPossibleCombination(statementList));
      }

      return permissions;
    } catch (AWSSecurityTokenServiceException ex) {
      log.error(ERROR_MSG, ex);
      throw new InvalidArgumentsException(
          MessageFormat.format("'iam:ListRolePolicies' or/and 'iam:GetRolePolicy' is not allowed on {0}\n{1}",
              crossAccountRoleArn, ex.getErrorMessage()));
    }
  }

  private HashSet<String> getAllPossibleCombination(Collection<Statement> statementList) {
    HashSet<String> permissions = new HashSet<>();
    for (Statement statement : statementList) {
      if (statement.getEffect().equals(Statement.Effect.Allow)) {
        for (Action action : statement.getActions()) {
          for (Resource resource : statement.getResources()) {
            permissions.add(action.getActionName() + DELIMITER + resource.getId());
          }
        }
      }
    }
    return permissions;
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

  private static String getRoleName(String crossAccountRoleArn) {
    return Arn.fromString(crossAccountRoleArn).getResource().getResource();
  }

  private List<ErrorDetail> validateResourceExists(AWSCredentialsProvider credentialsProvider,
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

  private static void validateReport(
      @NotNull ReportDefinition report, @NotNull String s3BucketName, final List<ErrorDetail> errorList) {
    if (!report.getS3Bucket().equals(s3BucketName)) {
      errorList.add(
          ErrorDetail.builder()
              .reason(String.format("Provided s3Bucket Name: %s, Actual s3bucket associated with the report: %s",
                  s3BucketName, report.getS3Bucket()))
              .message("Wrong s3Bucket Name")
              .build());
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

  private List<ErrorDetail> validateIfBucketIsPresent(
      AWSCredentialsProvider credentialsProvider, S3BucketDetails s3BucketDetails) {
    ObjectListing s3BucketObject =
        awsClient.getBucket(credentialsProvider, s3BucketDetails.getS3BucketName(), s3BucketDetails.getS3Prefix());
    if (CollectionUtils.isEmpty(s3BucketObject.getObjectSummaries())) {
      return ImmutableList.of(ErrorDetail.builder()
                                  .message(String.format("Can't access bucket: %s", s3BucketDetails.getS3BucketName()))
                                  .reason("The bucket might not be present.")
                                  .build());
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  public static Policy getOptimizationPolicy() {
    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Action\": ["
        + "        \"rds:StartDBCluster\","
        + "        \"rds:StopDBCluster\","
        + "        \"elasticloadbalancing:*\","
        + "        \"ec2:StopInstances\","
        + "        \"autoscaling:*\","
        + "        \"rds:StopDBInstance\","
        + "        \"rds:StartDBInstance\","
        + "        \"ec2:Describe*\","
        + "        \"iam:CreateServiceLinkedRole\","
        + "        \"iam:ListInstanceProfiles\","
        + "        \"iam:ListInstanceProfilesForRole\","
        + "        \"iam:AddRoleToInstanceProfile\","
        + "        \"iam:PassRole\","
        + "        \"ec2:StartInstances\","
        + "        \"rds:ListTagsForResource\","
        + "        \"rds:DescribeDBInstances\","
        + "        \"ec2:*\","
        + "        \"rds:ModifyDBInstance\","
        + "        \"iam:GetUser\","
        + "        \"ec2:ModifyInstanceAttribute\","
        + "        \"rds:DescribeDBClusters\""
        + "      ],"
        + "      \"Effect\": \"Allow\","
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";
    return Policy.fromJson(policyDocument);
  }

  @VisibleForTesting
  public static Policy getCurPolicy(final String customerBucketName, final String destinationBucketName) {
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

  @VisibleForTesting
  public static Policy getEventsPolicy() {
    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Sid\": \"VisualEditor0\","
        + "      \"Effect\": \"Allow\","
        + "      \"Action\": ["
        + "        \"organizations:Describe*\","
        + "        \"organizations:List*\","
        + "        \"eks:Describe*\","
        + "        \"eks:List*\","
        + "        \"cur:DescribeReportDefinitions\""
        + "      ],"
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";
    return Policy.fromJson(policyDocument);
  }
}
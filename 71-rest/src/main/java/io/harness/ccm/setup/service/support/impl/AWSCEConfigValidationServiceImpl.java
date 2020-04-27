package io.harness.ccm.setup.service.support.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.costandusagereport.AWSCostAndUsageReport;
import com.amazonaws.services.costandusagereport.AWSCostAndUsageReportClientBuilder;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsRequest;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsResult;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.organizations.AWSOrganizationsClient;
import com.amazonaws.services.organizations.AWSOrganizationsClientBuilder;
import com.amazonaws.services.organizations.model.AccessDeniedException;
import com.amazonaws.services.organizations.model.ListAccountsRequest;
import com.amazonaws.services.organizations.model.ListAccountsResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import io.harness.ccm.setup.service.support.AwsCredentialHelper;
import io.harness.ccm.setup.service.support.intfc.AWSCEConfigValidationService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import io.harness.exception.InvalidArgumentsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Singleton
public class AWSCEConfigValidationServiceImpl implements AWSCEConfigValidationService {
  @Inject private AwsCredentialHelper awsCredentialHelper;
  @Inject private AwsEKSHelperService awsEKSHelperService;
  private static final String ceAWSRegion = "us-east-1";
  private static final String compression = "GZIP";
  private static final String timeGranularity = "HOURLY";
  private static final String reportVersioning = "CREATE_NEW_REPORT";
  private static final String curReportKey = "CUR Report";
  private static final String curReportConfigKey = "CUR Report Config";
  private static final String validationFailureKey = "Validation Failed";

  @Override
  public String validateCURReportAccessAndReturnS3Region(CEAwsConfig awsConfig) {
    AwsCrossAccountAttributes awsCrossAccountAttributes = awsConfig.getAwsCrossAccountAttributes();
    AWSCredentialsProvider credentialsProvider = getCredentialProvider(awsCrossAccountAttributes);
    AWSCostAndUsageReport awsCostAndUsageReportClient = AWSCostAndUsageReportClientBuilder.standard()
                                                            .withRegion(ceAWSRegion)
                                                            .withCredentials(credentialsProvider)
                                                            .build();

    ReportDefinition report = getReportDefinitionIfPresent(awsCostAndUsageReportClient, awsConfig.getCurReportName());
    if (report == null) {
      throw new InvalidArgumentsException(ImmutablePair.of(curReportKey, "Invalid CUR Report Name"));
    }
    return validateReportAndGetS3Region(report, awsConfig);
  }

  private String validateReportAndGetS3Region(ReportDefinition report, CEAwsConfig awsConfig) {
    if (!report.getS3Bucket().equals(awsConfig.getS3BucketDetails().getS3BucketName())) {
      throw new InvalidArgumentsException(ImmutablePair.of(curReportConfigKey, "S3 Bucket Name Mismatch"));
    }
    if (!report.getCompression().equals(compression)) {
      throw new InvalidArgumentsException(ImmutablePair.of(curReportConfigKey, "Compression is not GZIP"));
    }
    if (!report.getTimeUnit().equals(timeGranularity)) {
      throw new InvalidArgumentsException(ImmutablePair.of(curReportConfigKey, "Time Granularity is not Hourly"));
    }
    if (!report.getReportVersioning().equals(reportVersioning)) {
      throw new InvalidArgumentsException(
          ImmutablePair.of(curReportConfigKey, "Report versioning should be CREATE_NEW_REPORT"));
    }
    if (!report.isRefreshClosedReports()) {
      throw new InvalidArgumentsException(
          ImmutablePair.of(curReportConfigKey, "Data Refresh setting should be Automatic"));
    }
    return report.getS3Region();
  }

  @VisibleForTesting
  ReportDefinition getReportDefinitionIfPresent(
      AWSCostAndUsageReport awsCostAndUsageReportClient, String curReportName) {
    List<ReportDefinition> reportDefinitionsList = new ArrayList<>();
    String nextToken = null;
    DescribeReportDefinitionsRequest describeReportDefinitionsRequest = new DescribeReportDefinitionsRequest();
    do {
      describeReportDefinitionsRequest.withNextToken(nextToken);
      DescribeReportDefinitionsResult describeReportDefinitionsResult =
          awsCostAndUsageReportClient.describeReportDefinitions(describeReportDefinitionsRequest);
      reportDefinitionsList.addAll(describeReportDefinitionsResult.getReportDefinitions());
      nextToken = describeReportDefinitionsRequest.getNextToken();
    } while (nextToken != null);

    for (ReportDefinition report : reportDefinitionsList) {
      if (report.getReportName().equals(curReportName)) {
        return report;
      }
    }
    return null;
  }

  @Override
  public void verifyCrossAccountAttributes(SettingAttribute settingAttribute) {
    CEAwsConfig awsConfig = (CEAwsConfig) settingAttribute.getValue();
    AwsCrossAccountAttributes awsCrossAccountAttributes = awsConfig.getAwsCrossAccountAttributes();
    AwsS3BucketDetails s3BucketDetails = awsConfig.getS3BucketDetails();
    AWSCredentialsProvider credentialsProvider = getCredentialProvider(awsCrossAccountAttributes);

    boolean isBucketAccessValid = false;

    try {
      isBucketAccessValid = validateIfBucketIsPresent(credentialsProvider, s3BucketDetails);
    } catch (AmazonS3Exception awsS3) {
      throw new InvalidArgumentsException(ImmutablePair.of(validationFailureKey, awsS3.getErrorCode()));
    } catch (Exception ex) {
      logger.error("Exception while validating s3 bucket ", ex);
    }
    if (!isBucketAccessValid) {
      throw new InvalidArgumentsException(ImmutablePair.of(validationFailureKey, "Can not Access S3 bucket"));
    }
    boolean isOrganisationalReadAccessValid = false;

    try {
      isOrganisationalReadAccessValid = validateOrganisationReadOnlyAccess(credentialsProvider);
    } catch (AccessDeniedException accessDeniedException) {
      throw new InvalidArgumentsException(ImmutablePair.of(
          validationFailureKey, accessDeniedException.getServiceName() + " " + accessDeniedException.getErrorCode()));
    }

    if (!isOrganisationalReadAccessValid) {
      throw new InvalidArgumentsException(
          ImmutablePair.of(validationFailureKey, "Issue with Listing Organisational Data"));
    }
    boolean isEksAccessValid = awsEKSHelperService.verifyAccess(ceAWSRegion, awsCrossAccountAttributes);
    if (!isEksAccessValid) {
      throw new InvalidArgumentsException(ImmutablePair.of(validationFailureKey, "Can not Access EKS data"));
    }
  }

  @VisibleForTesting
  boolean validateOrganisationReadOnlyAccess(AWSCredentialsProvider credentialsProvider) {
    AWSOrganizationsClientBuilder builder = AWSOrganizationsClientBuilder.standard().withRegion(ceAWSRegion);
    builder.withCredentials(credentialsProvider);
    AWSOrganizationsClient awsOrganizationsClient = (AWSOrganizationsClient) builder.build();
    ListAccountsResult listAccountsResult = awsOrganizationsClient.listAccounts(new ListAccountsRequest());
    return !listAccountsResult.getAccounts().isEmpty();
  }

  @VisibleForTesting
  boolean validateIfBucketIsPresent(AWSCredentialsProvider credentialsProvider, AwsS3BucketDetails s3BucketDetails) {
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(ceAWSRegion).withForceGlobalBucketAccessEnabled(Boolean.TRUE);
    builder.withCredentials(credentialsProvider);
    AmazonS3Client amazonS3Client = (AmazonS3Client) builder.build();
    List<S3ObjectSummary> objectSummaries =
        amazonS3Client.listObjects(s3BucketDetails.getS3BucketName()).getObjectSummaries();
    return objectSummaries != null;
  }

  @VisibleForTesting
  AWSCredentialsProvider getCredentialProvider(AwsCrossAccountAttributes awsCrossAccountAttributes) {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    return new STSAssumeRoleSessionCredentialsProvider
        .Builder(awsCrossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
        .withExternalId(awsCrossAccountAttributes.getExternalId())
        .withStsClient(awsSecurityTokenService)
        .build();
  }
}

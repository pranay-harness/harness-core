package io.harness.ccm.setup.service.support.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.costandusagereport.AWSCostAndUsageReport;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsRequest;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsResult;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

import java.util.HashMap;

public class AWSCEConfigValidationServiceImplTest {
  @Spy @InjectMocks AWSCEConfigValidationServiceImpl awsceConfigValidationService;
  @Mock AwsEKSHelperService awsEKSHelperService;

  private static final String S3_BUCKET_NAME = "s3BucketName";
  private static final String IAM_ROLE_ARN = "roleArn";
  private static final String EXTERNAL_ID = "externalId";
  private static final String CUR_REPORT_NAME = "curReport";
  private static final String AWS_ACCOUNT_ID = "awsAccount";
  private static final String s3Region = "us-east-1";
  private static final String compression = "GZIP";
  private static final String timeGranularity = "HOURLY";
  private static final String reportVersioning = "OVERWRITE_REPORT";
  private static final String invalidValue = "invalidValue";
  private static CEAwsConfig ceAwsConfig = CEAwsConfig.builder().build();
  private static ReportDefinition reportDefinition = new ReportDefinition();
  private static HashMap<String, String> exceptionParamsMap = new HashMap<>();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    ceAwsConfig =
        CEAwsConfig.builder()
            .s3BucketDetails(AwsS3BucketDetails.builder().s3BucketName(S3_BUCKET_NAME).build())
            .awsCrossAccountAttributes(
                AwsCrossAccountAttributes.builder().crossAccountRoleArn(IAM_ROLE_ARN).externalId(EXTERNAL_ID).build())
            .curReportName(CUR_REPORT_NAME)
            .awsAccountId(AWS_ACCOUNT_ID)
            .build();

    reportDefinition.setReportVersioning(reportVersioning);
    reportDefinition.setCompression(compression);
    reportDefinition.setTimeUnit(timeGranularity);
    reportDefinition.setS3Bucket(S3_BUCKET_NAME);
    reportDefinition.setS3Region(s3Region);
    reportDefinition.setReportName(CUR_REPORT_NAME);
    reportDefinition.setRefreshClosedReports(true);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void validateCURReportAccessAndReturnS3Region() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());
    String s3BucketRegion = awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig);
    assertThat(s3BucketRegion).isEqualTo(s3Region);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void reportNotFoundTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    exceptionParamsMap.put("args", "CUR Report: Invalid CUR Report Name");
    doReturn(null).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidS3BucketTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    reportDefinition.setS3Bucket(invalidValue);
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());

    exceptionParamsMap.put("args", "CUR Report Config: S3 Bucket Name Mismatch");
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidCompressionTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    reportDefinition.setCompression(invalidValue);
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());

    exceptionParamsMap.put("args", "CUR Report Config: Compression is not GZIP");
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidTimeGranularityTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    reportDefinition.setTimeUnit(invalidValue);
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());

    exceptionParamsMap.put("args", "CUR Report Config: Time Granularity is not Hourly");
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidReportVersioningTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    reportDefinition.setReportVersioning(invalidValue);
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());

    exceptionParamsMap.put("args", "CUR Report Config: Report versioning should be OVERWRITE_REPORT");
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidRefreshSettingTestCase() {
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    reportDefinition.setRefreshClosedReports(false);
    doReturn(reportDefinition).when(awsceConfigValidationService).getReportDefinitionIfPresent(any(), anyString());

    exceptionParamsMap.put("args", "CUR Report Config: Data Refresh setting should be Automatic");
    assertThatThrownBy(() -> awsceConfigValidationService.validateCURReportAccessAndReturnS3Region(ceAwsConfig))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetReportDefinitionIfPresentMethod() {
    AWSCostAndUsageReport mockClient = mock(AWSCostAndUsageReport.class);
    doReturn(new DescribeReportDefinitionsResult().withReportDefinitions(reportDefinition).withNextToken(null))
        .when(mockClient)
        .describeReportDefinitions(new DescribeReportDefinitionsRequest().withNextToken(null));
    ReportDefinition report = awsceConfigValidationService.getReportDefinitionIfPresent(mockClient, CUR_REPORT_NAME);
    assertThat(report.getReportName()).isEqualTo(CUR_REPORT_NAME);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetReportDefinitionNotPresentMethod() {
    AWSCostAndUsageReport mockClient = mock(AWSCostAndUsageReport.class);
    reportDefinition.setReportName(CUR_REPORT_NAME + "_1");
    doReturn(new DescribeReportDefinitionsResult().withReportDefinitions(reportDefinition).withNextToken(null))
        .when(mockClient)
        .describeReportDefinitions(new DescribeReportDefinitionsRequest().withNextToken(null));
    ReportDefinition report = awsceConfigValidationService.getReportDefinitionIfPresent(mockClient, CUR_REPORT_NAME);
    assertThat(report).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void verifyCrossAccountAttributesWithIncorrectEKSPermissions() {
    SettingAttribute settingAttributeMock = mock(SettingAttribute.class);
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(ceAwsConfig).when(settingAttributeMock).getValue();
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    doReturn(true)
        .when(awsceConfigValidationService)
        .validateIfBucketIsPresent(mockCredential, ceAwsConfig.getS3BucketDetails());
    doReturn(true).when(awsceConfigValidationService).validateOrganisationReadOnlyAccess(mockCredential);
    doReturn(false).when(awsEKSHelperService).verifyAccess(s3Region, ceAwsConfig.getAwsCrossAccountAttributes());

    exceptionParamsMap.put("args", "Validation Failed: Can not Access EKS data");
    assertThatThrownBy(() -> awsceConfigValidationService.verifyCrossAccountAttributes(settingAttributeMock))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void verifyCrossAccountAttributesWithIncorrectS3Permissions() {
    SettingAttribute settingAttributeMock = mock(SettingAttribute.class);
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(ceAwsConfig).when(settingAttributeMock).getValue();
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    doReturn(false)
        .when(awsceConfigValidationService)
        .validateIfBucketIsPresent(mockCredential, ceAwsConfig.getS3BucketDetails());
    doReturn(true).when(awsceConfigValidationService).validateOrganisationReadOnlyAccess(mockCredential);
    doReturn(true).when(awsEKSHelperService).verifyAccess(s3Region, ceAwsConfig.getAwsCrossAccountAttributes());

    exceptionParamsMap.put("args", "Validation Failed: Can not Access S3 bucket");
    assertThatThrownBy(() -> awsceConfigValidationService.verifyCrossAccountAttributes(settingAttributeMock))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void verifyCrossAccountAttributesWithIncorrectOrganisationsPermissions() {
    SettingAttribute settingAttributeMock = mock(SettingAttribute.class);
    AWSCredentialsProvider mockCredential = mock(AWSCredentialsProvider.class);
    doReturn(ceAwsConfig).when(settingAttributeMock).getValue();
    doReturn(mockCredential)
        .when(awsceConfigValidationService)
        .getCredentialProvider(ceAwsConfig.getAwsCrossAccountAttributes());
    doReturn(true)
        .when(awsceConfigValidationService)
        .validateIfBucketIsPresent(mockCredential, ceAwsConfig.getS3BucketDetails());
    doReturn(false).when(awsceConfigValidationService).validateOrganisationReadOnlyAccess(mockCredential);
    doReturn(true).when(awsEKSHelperService).verifyAccess(s3Region, ceAwsConfig.getAwsCrossAccountAttributes());

    exceptionParamsMap.put("args", "Validation Failed: Issue with Listing Organisational Data");
    assertThatThrownBy(() -> awsceConfigValidationService.verifyCrossAccountAttributes(settingAttributeMock))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasFieldOrPropertyWithValue("params", exceptionParamsMap);
  }
}
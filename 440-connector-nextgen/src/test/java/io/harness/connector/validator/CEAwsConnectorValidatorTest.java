package io.harness.connector.validator;

import static io.harness.connector.utils.AWSConnectorTestHelper.createNonEmptyObjectListing;
import static io.harness.connector.utils.AWSConnectorTestHelper.createReportDefinition;
import static io.harness.connector.validator.CEAwsConnectorValidator.getCurPolicy;
import static io.harness.connector.validator.CEAwsConnectorValidator.getEventsPolicy;
import static io.harness.connector.validator.CEAwsConnectorValidator.getOptimizationPolicy;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.aws.AwsClient;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.rule.Owner;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.s3.model.ObjectListing;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CEAwsConnectorValidatorTest extends CategoryTest {
  @Mock AwsClient awsClient;
  @Mock CEAwsSetupConfig ceAwsSetupConfig;
  @Spy @InjectMocks private CEAwsConnectorValidator connectorValidator;

  private CEAwsConnectorDTO ceAwsConnectorDTO;

  private static final String CUSTOMER_BILLING_DATA_DEV = "customer-billing-data-dev";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    ceAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();

    doReturn(CUSTOMER_BILLING_DATA_DEV).when(ceAwsSetupConfig).getDestinationBucket();
    doReturn(null).when(connectorValidator).getCredentialProvider(any());
    doReturn(ImmutableList.of("p0")).when(awsClient).listRolePolicyNames(any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)

  public void testValidateAllSuccess() throws NoSuchFieldException, IllegalAccessException {
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();
    Policy curPolicy =
        getCurPolicy(ceAwsConnectorDTO.getCurAttributes().getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());

    doReturn(ImmutableList.of("p0", "p1", "p2")).when(awsClient).listRolePolicyNames(any(), any());
    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());
    doReturn(curPolicy)
        .doReturn(getEventsPolicy())
        .doReturn(getOptimizationPolicy())
        .when(awsClient)
        .getRolePolicy(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateEvents() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEAwsFeatures.EVENTS));
    ceAwsConnectorDTO.setCurAttributes(null);

    doReturn(getEventsPolicy()).when(awsClient).getRolePolicy(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateEventsPermissionMissing() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEAwsFeatures.EVENTS));
    ceAwsConnectorDTO.setCurAttributes(null);

    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Sid\": \"VisualEditor0\","
        + "      \"Effect\": \"Allow\","
        + "      \"Action\": ["
        + "        \"organizations:List*\","
        + "        \"eks:Describe*\","
        + "        \"cur:DescribeReportDefinitions\""
        + "      ],"
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";
    doReturn(Policy.fromJson(policyDocument)).when(awsClient).getRolePolicy(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).hasSize(2);
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateCurSuccess() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEAwsFeatures.CUR));
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();
    Policy curPolicy =
        getCurPolicy(ceAwsConnectorDTO.getCurAttributes().getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());

    doReturn(curPolicy).when(awsClient).getRolePolicy(any(), any(), any());
    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateReportNotPresent() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEAwsFeatures.CUR));
    ObjectListing s3Object = createNonEmptyObjectListing();
    Policy curPolicy =
        getCurPolicy(ceAwsConnectorDTO.getCurAttributes().getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());

    doReturn(curPolicy).when(awsClient).getRolePolicy(any(), any(), any());
    doReturn(Optional.empty()).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getReason()).isEqualTo("Report Not Present");
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateBucketNotPresent() {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEAwsFeatures.CUR));
    ObjectListing s3Object = new ObjectListing();
    ReportDefinition report = createReportDefinition();
    Policy curPolicy =
        getCurPolicy(ceAwsConnectorDTO.getCurAttributes().getS3BucketName(), ceAwsSetupConfig.getDestinationBucket());

    doReturn(curPolicy).when(awsClient).getRolePolicy(any(), any(), any());
    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getReason()).isEqualTo("The bucket might not be present.");
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateCurPermissionMissing() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEAwsFeatures.CUR));
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();
    Policy curPolicy = getCurPolicy(UUID.randomUUID().toString(), ceAwsSetupConfig.getDestinationBucket());

    doReturn(curPolicy).when(awsClient).getRolePolicy(any(), any(), any());
    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).isNotEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateOptimizationSuccess() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEAwsFeatures.OPTIMIZATION));

    doReturn(getOptimizationPolicy()).when(awsClient).getRolePolicy(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateOptimizationPermissionMissing() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEAwsFeatures.OPTIMIZATION));

    final String policyDocument = "{"
        + "  \"Version\": \"2012-10-17\","
        + "  \"Statement\": ["
        + "    {"
        + "      \"Action\": ["
        + "        \"rds:DescribeDBClusters\""
        + "      ],"
        + "      \"Effect\": \"Allow\","
        + "      \"Resource\": \"*\""
        + "    }"
        + "  ]"
        + "}";

    doReturn(Policy.fromJson(policyDocument)).when(awsClient).getRolePolicy(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).isNotEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }
}
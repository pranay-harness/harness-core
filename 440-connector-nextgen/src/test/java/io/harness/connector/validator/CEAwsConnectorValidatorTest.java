package io.harness.connector.validator;

import static io.harness.connector.utils.AWSConnectorTestHelper.createNonEmptyObjectListing;
import static io.harness.connector.utils.AWSConnectorTestHelper.createReportDefinition;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.rule.Owner;

import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class CEAwsConnectorValidatorTest extends CategoryTest {
  @Mock AwsClient awsClient;
  @Mock CEAwsSetupConfig ceAwsSetupConfig;
  @Spy @InjectMocks private CEAwsConnectorValidator connectorValidator;

  private static final String REASON = "implicitDeny";
  private static final String ACTION = "s3:PutObject";
  private static final String RESOURCE = "*";
  private static final String MESSAGE = "message";
  private static final String CUSTOMER_BILLING_DATA_DEV = "customer-billing-data-dev";
  private static final EvaluationResult DENY_EVALUATION_RESULT = new EvaluationResult();

  private CEAwsConnectorDTO ceAwsConnectorDTO;

  @BeforeClass
  public static void init() {
    DENY_EVALUATION_RESULT.withEvalDecision(REASON).withEvalActionName(ACTION).withEvalResourceName(RESOURCE);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    ceAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();

    doReturn(CUSTOMER_BILLING_DATA_DEV).when(ceAwsSetupConfig).getDestinationBucket();
    doReturn(null).when(connectorValidator).getCredentialProvider(any());
    doReturn(Collections.singletonList(new EvaluationResult().withEvalDecision("allowed")))
        .when(awsClient)
        .simulatePrincipalPolicy(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)

  public void testValidateAllSuccess() throws NoSuchFieldException, IllegalAccessException {
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();

    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateEventsSuccess() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEFeatures.VISIBILITY));
    ceAwsConnectorDTO.setCurAttributes(null);

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateEventsPermissionMissing() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEFeatures.VISIBILITY));
    ceAwsConnectorDTO.setCurAttributes(null);

    doReturn(Collections.singletonList(DENY_EVALUATION_RESULT))
        .when(awsClient)
        .simulatePrincipalPolicy(any(), any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());

    assertThat(result.getErrors().get(0).getReason()).isEqualTo(REASON);
    assertThat(result.getErrors().get(0).getMessage()).contains(ACTION).contains(RESOURCE);
    assertThat(result.getErrors().get(0).getCode()).isEqualTo(403);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateCurSuccess() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEFeatures.BILLING));
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();

    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateReportNotPresent() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEFeatures.BILLING));
    ObjectListing s3Object = createNonEmptyObjectListing();

    doReturn(Optional.empty()).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getReason()).isEqualTo("Report Not Present");
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateBucketIsPresentIgnoringObjects() {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEFeatures.BILLING));
    ReportDefinition report = createReportDefinition();

    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(null).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNull();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateCurPermissionMissing() throws NoSuchFieldException, IllegalAccessException {
    ceAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEFeatures.BILLING));
    ReportDefinition report = createReportDefinition();
    ObjectListing s3Object = createNonEmptyObjectListing();

    doReturn(Collections.singletonList(DENY_EVALUATION_RESULT))
        .when(awsClient)
        .simulatePrincipalPolicy(any(), any(), any(), any());
    doReturn(Optional.of(report)).when(awsClient).getReportDefinition(any(), any());
    doReturn(s3Object).when(awsClient).getBucket(any(), any(), any());

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrors()).isNotEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());

    assertThat(result.getErrors().get(0).getReason()).isEqualTo(REASON);
    assertThat(result.getErrors().get(0).getMessage()).contains(ACTION).contains(RESOURCE);
    assertThat(result.getErrors().get(0).getCode()).isEqualTo(403);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateOptimizationSuccess() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEFeatures.OPTIMIZATION));

    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(result.getErrors()).isNullOrEmpty();
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateOptimizationPermissionMissing() {
    ceAwsConnectorDTO.setFeaturesEnabled(Collections.singletonList(CEFeatures.OPTIMIZATION));

    doReturn(Collections.singletonList(DENY_EVALUATION_RESULT))
        .when(awsClient)
        .simulatePrincipalPolicy(any(), any(), any(), any());
    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getReason()).isEqualTo(REASON);
    assertThat(result.getErrors().get(0).getMessage()).contains(ACTION).contains(RESOURCE);
    assertThat(result.getErrors().get(0).getCode()).isEqualTo(403);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testSimulatePrincipalPolicyPermissionMissing() {
    doThrow(new AmazonIdentityManagementException(MESSAGE))
        .when(awsClient)
        .simulatePrincipalPolicy(any(), any(), any(), any());
    ConnectorValidationResult result = connectorValidator.validate(ceAwsConnectorDTO, null, null, null, null);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrorSummary()).contains("iam:SimulatePrincipalPolicy");
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getReason()).isEqualTo(MESSAGE);
  }
}
package io.harness.connector.utils;

import static io.harness.delegate.beans.connector.ConnectorType.CE_AWS;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.entities.embedded.ceawsconnector.CURAttributes;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.encryption.Scope;

import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AWSConnectorTestHelper {
  public final String REPORT_NAME = "report_name_utsav";
  public final String S3_BUCKET_NAME = "ce-customer-billing-data-dev";
  public final String AWS_ACCOUNT_ID = "890436954479";
  public final String EXTERNAL_ID = "harness:108817434118:kmpySmUISimoRrJL6NL73w";
  public final String CROSS_ACCOUNT_ROLE_ARN = "arn:aws:iam::890436954479:role/harnessCERole";
  public final String DEFAULT_REGION = "us-east-1";
  public final String PREFIX_NAME = "s3Prefix";

  private final String COMPRESSION = "GZIP";
  private final String TIME_GRANULARITY = "HOURLY";
  private final String REPORT_VERSIONING = "OVERWRITE_REPORT";
  private final String RESOURCES = "RESOURCES";

  public final List<CEAwsFeatures> CE_AWS_FEATURES_ENABLED =
      ImmutableList.of(CEAwsFeatures.CUR, CEAwsFeatures.EVENTS, CEAwsFeatures.OPTIMIZATION);

  public Connector createAWSConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, Scope scope) {
    final String delegateSelector = "delegateSelector";
    final AwsConfig awsConfig = AwsConfig.builder()
                                    .credentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                    .crossAccountAccess(createCrossAccountAccessDTO())
                                    .credential(AwsIamCredential.builder().delegateSelector(delegateSelector).build())
                                    .build();

    awsConfig.setAccountIdentifier(accountIdentifier);
    awsConfig.setOrgIdentifier(orgIdentifier);
    awsConfig.setProjectIdentifier(projectIdentifier);
    awsConfig.setIdentifier(identifier);
    awsConfig.setScope(scope);
    awsConfig.setType(ConnectorType.AWS);

    return awsConfig;
  }

  public CrossAccountAccessDTO createCrossAccountAccessDTO() {
    return CrossAccountAccessDTO.builder().crossAccountRoleArn(CROSS_ACCOUNT_ROLE_ARN).externalId(EXTERNAL_ID).build();
  }

  public CEAwsConnectorDTO createCEAwsConnectorDTO() {
    AwsCurAttributesDTO awsCurAttributesDTO =
        AwsCurAttributesDTO.builder().reportName(REPORT_NAME).s3BucketName(S3_BUCKET_NAME).build();
    return CEAwsConnectorDTO.builder()
        .curAttributes(awsCurAttributesDTO)
        .crossAccountAccess(createCrossAccountAccessDTO())
        .featuresEnabled(CE_AWS_FEATURES_ENABLED)
        .build();
  }

  public ConnectorDTO createConnectorDTOOfCEAws() {
    final String connectorIdentifier = "identifier_ph";
    final String name = "name_ph";
    final String description = "description_ph";
    final String projectIdentifier = "projectIdentifier_ph";
    final String orgIdentifier = "orgIdentifier_ph";
    Map<String, String> tags = ImmutableMap.of("company", "Harness", "env", "dev");

    return ConnectorDTO.builder()
        .connectorInfo(ConnectorInfoDTO.builder()
                           .name(name)
                           .identifier(connectorIdentifier)
                           .description(description)
                           .projectIdentifier(projectIdentifier)
                           .orgIdentifier(orgIdentifier)
                           .tags(tags)
                           .connectorType(CE_AWS)
                           .connectorConfig(createCEAwsConnectorDTO())
                           .build())
        .build();
  }

  public CEAwsConfig createCEAwsConfigEntity() {
    return CEAwsConfig.builder()
        .featuresEnabled(CE_AWS_FEATURES_ENABLED)
        .awsAccountId(AWS_ACCOUNT_ID)
        .crossAccountAccess(createCrossAccountAccessDTO())
        .curAttributes(CURAttributes.builder()
                           .reportName(REPORT_NAME)
                           .s3BucketDetails(S3BucketDetails.builder()
                                                .s3BucketName(S3_BUCKET_NAME)
                                                .region(DEFAULT_REGION)
                                                .s3Prefix(PREFIX_NAME)
                                                .build())
                           .build())
        .build();
  }

  public static ReportDefinition createReportDefinition() {
    return new ReportDefinition()
        .withS3Bucket(S3_BUCKET_NAME)
        .withCompression(COMPRESSION)
        .withTimeUnit(TIME_GRANULARITY)
        .withReportVersioning(REPORT_VERSIONING)
        .withRefreshClosedReports(true)
        .withAdditionalSchemaElements(RESOURCES)
        .withReportName(REPORT_NAME)
        .withS3Prefix(PREFIX_NAME)
        .withS3Region(DEFAULT_REGION);
  }

  /**
   * ObjectListing.class has no setter for modifying private field, 'List<S3ObjectSummary> objectSummaries = new
   * ArrayList();'
   * @return ObjectListing
   * @throws NoSuchFieldException declaredField "objectSummaries" doesn't exist
   * @throws IllegalAccessException field not accessible
   */
  public static ObjectListing createNonEmptyObjectListing() throws NoSuchFieldException, IllegalAccessException {
    ObjectListing objectListing = new ObjectListing();
    Field field = objectListing.getClass().getDeclaredField("objectSummaries");
    field.setAccessible(true);
    field.set(objectListing, Arrays.asList(new S3ObjectSummary()));
    return objectListing;
  }
}

package io.harness.connector.utils;

import static io.harness.delegate.beans.connector.ConnectorType.CE_AWS;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AWSConnectorTestHelper {
  public final String REPORT_NAME = "reportName";
  public final String S3_BUCKET_NAME = "s3BucketName";
  public final String AWS_ACCOUNT_ID = "123456789012";
  public final String EXTERNAL_ID = "harness:987654321091:3vgRlwVBSPKxGMtmH4uOYQ";
  public final String CROSS_ACCOUNT_ROLE_ARN = "arn:aws:iam::123456789012:role/harnessCERole";

  public static final List<CEAwsFeatures> CE_AWS_FEATURES_ENABLED =
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
                                                .region("region")
                                                .s3Prefix("s3Prefix")
                                                .build())
                           .build())
        .build();
  }
}

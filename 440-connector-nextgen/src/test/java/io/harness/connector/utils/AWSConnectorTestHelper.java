package io.harness.connector.utils;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.encryption.Scope;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AWSConnectorTestHelper {
  public Connector createAWSConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, Scope scope) {
    final String crossAccountRoleArn = "crossAccountRoleArn";
    final String externalRoleArn = "externalRoleArn";
    final String delegateSelector = "delegateSelector";
    final CrossAccountAccessDTO crossAccountAccess =
        CrossAccountAccessDTO.builder().crossAccountRoleArn(crossAccountRoleArn).externalId(externalRoleArn).build();
    final AwsConfig awsConfig = AwsConfig.builder()
                                    .credentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                    .crossAccountAccess(crossAccountAccess)
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
}

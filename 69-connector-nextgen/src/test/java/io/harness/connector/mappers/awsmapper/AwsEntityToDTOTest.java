package io.harness.connector.mappers.awsmapper;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccess;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AwsEntityToDTOTest extends CategoryTest {
  @InjectMocks AwsEntityToDTO awsEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    final String crossAccountRoleArn = "crossAccountRoleArn";
    final String externalRoleArn = "externalRoleArn";
    final String delegateSelector = "delegateSelector";
    final CrossAccountAccess crossAccountAccess =
        CrossAccountAccess.builder().crossAccountRoleArn(crossAccountRoleArn).externalId(externalRoleArn).build();
    final AwsConfig awsConfig = AwsConfig.builder()
                                    .credentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                    .crossAccountAccess(crossAccountAccess)
                                    .credential(AwsIamCredential.builder().delegateSelector(delegateSelector).build())
                                    .build();
    final AwsConnectorDTO connectorDTO = awsEntityToDTO.createConnectorDTO(awsConfig);

    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getCredential()).isNotNull();
    assertThat(connectorDTO.getCredential().getAwsCredentialType()).isEqualTo(AwsCredentialType.INHERIT_FROM_DELEGATE);
    assertThat(((AwsInheritFromDelegateSpecDTO) connectorDTO.getCredential().getConfig()).getDelegateSelector())
        .isEqualTo(delegateSelector);
    assertThat(connectorDTO.getCredential().getCrossAccountAccess()).isEqualTo(crossAccountAccess);

    final String accessKey = "accessKey";
    final AwsConfig awsConfig1 =
        AwsConfig.builder()
            .credentialType(AwsCredentialType.MANUAL_CREDENTIALS)
            .crossAccountAccess(crossAccountAccess)
            .credential(AwsAccessKeyCredential.builder().accessKey(accessKey).secretKeyRef("secretKey").build())
            .build();
    final AwsConnectorDTO connectorDTO1 = awsEntityToDTO.createConnectorDTO(awsConfig1);

    assertThat(connectorDTO1).isNotNull();
    assertThat(connectorDTO1.getCredential()).isNotNull();
    assertThat(connectorDTO1.getCredential().getAwsCredentialType()).isEqualTo(AwsCredentialType.MANUAL_CREDENTIALS);
    assertThat(((AwsManualConfigSpecDTO) connectorDTO1.getCredential().getConfig()).getAccessKey())
        .isEqualTo(accessKey);
    assertThat(connectorDTO1.getCredential().getCrossAccountAccess()).isEqualTo(crossAccountAccess);
  }
}
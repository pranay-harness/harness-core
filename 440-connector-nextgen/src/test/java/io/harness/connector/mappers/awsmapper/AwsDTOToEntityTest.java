package io.harness.connector.mappers.awsmapper;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AwsDTOToEntityTest extends CategoryTest {
  @InjectMocks AwsDTOToEntity awsDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    final String crossAccountRoleArn = "crossAccountRoleArn";
    final String externalRoleArn = "externalRoleArn";
    final String delegateSelector = "delegateSelector";
    final CrossAccountAccessDTO crossAccountAccess =
        CrossAccountAccessDTO.builder().crossAccountRoleArn(crossAccountRoleArn).externalId(externalRoleArn).build();
    final AwsCredentialDTO awsCredentialDTO =
        AwsCredentialDTO.builder()
            .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
            .crossAccountAccess(crossAccountAccess)
            .config(AwsInheritFromDelegateSpecDTO.builder().delegateSelector(delegateSelector).build())
            .build();
    final AwsConnectorDTO awsConnectorDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();
    final AwsConfig awsConfig = awsDTOToEntity.toConnectorEntity(awsConnectorDTO);

    assertThat(awsConfig).isNotNull();
    assertThat(awsConfig.getCredentialType()).isEqualTo(AwsCredentialType.INHERIT_FROM_DELEGATE);
    assertThat(awsConfig.getCrossAccountAccess()).isEqualTo(crossAccountAccess);
    assertThat(awsConfig.getCredential()).isNotNull();
    assertThat(((AwsIamCredential) awsConfig.getCredential()).getDelegateSelector()).isEqualTo(delegateSelector);

    final String accessKey = "accessKey";
    final AwsCredentialDTO awsCredentialDTO1 = AwsCredentialDTO.builder()
                                                   .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                                   .crossAccountAccess(crossAccountAccess)
                                                   .config(AwsManualConfigSpecDTO.builder()
                                                               .accessKey(accessKey)
                                                               .secretKeyRef(SecretRefData.builder().build())
                                                               .build())
                                                   .build();
    final AwsConnectorDTO awsConnectorDTO1 = AwsConnectorDTO.builder().credential(awsCredentialDTO1).build();
    final AwsConfig awsConfig1 = awsDTOToEntity.toConnectorEntity(awsConnectorDTO1);

    assertThat(awsConfig1).isNotNull();
    assertThat(awsConfig1.getCredentialType()).isEqualTo(AwsCredentialType.MANUAL_CREDENTIALS);
    assertThat(awsConfig1.getCrossAccountAccess()).isEqualTo(crossAccountAccess);
    assertThat(awsConfig1.getCredential()).isNotNull();
    assertThat(((AwsAccessKeyCredential) awsConfig1.getCredential()).getAccessKey()).isEqualTo(accessKey);
  }
}

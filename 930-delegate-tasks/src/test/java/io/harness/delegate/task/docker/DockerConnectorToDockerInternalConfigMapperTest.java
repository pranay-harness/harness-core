package io.harness.delegate.task.docker;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.USER_PASSWORD;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class DockerConnectorToDockerInternalConfigMapperTest extends CategoryTest {
  @InjectMocks DockerConnectorToDockerInternalConfigMapper dockerConnectorToDockerInternalConfigMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toDockerInternalConfigTest() {
    String dockerRegistryUrl = "https://docker.connector/";
    String dockerUserName = "dockerUserName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    String password = "password";
    SecretRefData passwordSecretRef = SecretRefData.builder()
                                          .identifier(passwordRefIdentifier)
                                          .scope(Scope.ACCOUNT)
                                          .decryptedValue(password.toCharArray())
                                          .build();

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder().username(dockerUserName).passwordRef(passwordSecretRef).build();

    DockerAuthenticationDTO dockerAuthenticationDTO =
        DockerAuthenticationDTO.builder().authType(USER_PASSWORD).credentials(dockerUserNamePasswordDTO).build();
    DockerConnectorDTO dockerConnectorDTO =
        DockerConnectorDTO.builder().dockerRegistryUrl(dockerRegistryUrl).auth(dockerAuthenticationDTO).build();
    DockerInternalConfig dockerConector =
        dockerConnectorToDockerInternalConfigMapper.toDockerInternalConfig(dockerConnectorDTO);
    assertThat(dockerConector).isNotNull();
    assertThat(dockerConector.getDockerRegistryUrl()).isEqualTo(dockerRegistryUrl);
    assertThat(dockerConector.getUsername()).isEqualTo(dockerUserName);
    assertThat(dockerConector.getPassword()).isEqualTo(String.valueOf(passwordSecretRef.getDecryptedValue()));
  }
}

package software.wings.resources.secretsmanagment;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.NgManagerServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.resources.secretsmanagement.NGSecretsResource;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

public class NGSecretsResourceTest extends WingsBaseTest {
  private final String SECRET_NAME = "SECRET_NAME";
  private final String SECRET_ID = "SECRET_ID";
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String USER_ID = "USER_ID";

  @Mock SecretManager secretManager;
  @Mock NgManagerServiceDriver ngManagerServiceDriver;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGet() {
    EncryptedData encryptedData = EncryptedData.builder().name(SECRET_NAME).build();
    when(secretManager.getSecretById(anyString(), anyString())).thenReturn(encryptedData);
    NGSecretsResource ngSecretsResource = new NGSecretsResource(secretManager, ngManagerServiceDriver);
    RestResponse<EncryptedData> encryptedDataRestResponse = ngSecretsResource.get(SECRET_ID, ACCOUNT_ID, USER_ID);
    assertThat(encryptedDataRestResponse).isNotNull();
    assertThat(encryptedDataRestResponse.getResource()).isNotNull();
    assertThat(encryptedDataRestResponse.getResource().getName()).isEqualTo(SECRET_NAME);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskResponse() {
    SendTaskResultResponse sendTaskResultResponse =
        SendTaskResultResponse.newBuilder().setAcknowledgement(true).build();
    when(ngManagerServiceDriver.sendTaskResult(any(SendTaskResultRequest.class))).thenReturn(sendTaskResultResponse);
    NGSecretsResource ngSecretsResource = new NGSecretsResource(secretManager, ngManagerServiceDriver);
    RestResponse<Boolean> restResponse = ngSecretsResource.sendTaskResponse();
    assertThat(restResponse).isNotNull();
    assertThat(restResponse.getResource()).isNotNull();
    assertThat(restResponse.getResource()).isEqualTo(true);
  }
}

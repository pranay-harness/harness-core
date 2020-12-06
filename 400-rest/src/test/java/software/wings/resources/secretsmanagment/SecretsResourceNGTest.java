package software.wings.resources.secretsmanagment;

import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

import software.wings.WingsBaseTest;
import software.wings.resources.secretsmanagement.SecretsResourceNG;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.settings.SettingVariableTypes;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SecretsResourceNGTest extends WingsBaseTest {
  private final String SECRET_NAME = "SECRET_NAME";
  private final String SECRET_IDENTIFIER = "SECRET_ID";
  private final String ACCOUNT_IDENTIFIER = "ACCOUNT_ID";
  private final String ORG_IDENTIFIER = "ACCOUNT_ID";
  private final String PROJECT_IDENTIFIER = "ACCOUNT_ID";
  private SecretsResourceNG secretsResourceNG;
  private NGSecretService ngSecretService;

  @Before
  public void setup() {
    ngSecretService = mock(NGSecretService.class);
    secretsResourceNG = new SecretsResourceNG(ngSecretService);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGet() {
    EncryptedData encryptedData =
        EncryptedData.builder().name(SECRET_NAME).type(SettingVariableTypes.SECRET_TEXT).build();
    when(ngSecretService.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER))
        .thenReturn(Optional.of(encryptedData));
    RestResponse<EncryptedDataDTO> encryptedDataRestResponse =
        secretsResourceNG.get(SECRET_IDENTIFIER, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(encryptedDataRestResponse).isNotNull();
    assertThat(encryptedDataRestResponse.getResource()).isNotNull();
    assertThat(encryptedDataRestResponse.getResource().getName()).isEqualTo(SECRET_NAME);
  }
}

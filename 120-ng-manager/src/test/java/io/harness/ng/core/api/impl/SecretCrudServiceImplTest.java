package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.amazonaws.util.StringInputStream;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.SecretCrudServiceImpl;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.remote.SSHKeyValidationMetadata;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.Mock;
import retrofit2.Response;
import software.wings.app.FileUploadLimit;

import java.io.IOException;

public class SecretCrudServiceImplTest extends CategoryTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private SecretManagerClient secretManagerClient;
  @Mock private SecretModifyService secretTextService;
  @Mock private SecretModifyService secretFileService;
  @Mock private SecretModifyService sshService;
  @Mock private NGSecretServiceV2 ngSecretServiceV2;
  private final FileUploadLimit fileUploadLimit = new FileUploadLimit();
  @Mock private SecretEntityReferenceHelper secretEntityReferenceHelper;
  @Mock private SecretCrudServiceImpl secretCrudService;

  @Before
  public void setup() {
    initMocks(this);
    secretCrudService = new SecretCrudServiceImpl(secretManagerClient, secretTextService, secretFileService, sshService,
        secretEntityReferenceHelper, fileUploadLimit, ngSecretServiceV2);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecret() {
    EncryptedDataDTO encryptedDataDTO = EncryptedDataDTO.builder().type(SecretType.SecretText).build();
    Secret secret = Secret.builder().build();
    when(secretTextService.create(any(), any())).thenReturn(encryptedDataDTO);
    when(ngSecretServiceV2.create(any(), any(), eq(false))).thenReturn(secret);

    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder().type(SecretType.SecretText).build();
    SecretResponseWrapper responseWrapper = secretCrudService.create("account", secretDTOV2);
    assertThat(responseWrapper).isNotNull();

    verify(secretTextService).create(any(), any());
    verify(ngSecretServiceV2).create(any(), any(), eq(false));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretViaYaml_failDueToValueProvided() {
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .type(SecretType.SecretText)
                                  .spec(SecretTextSpecDTO.builder().valueType(ValueType.Inline).value("value").build())
                                  .build();
    try {
      secretCrudService.createViaYaml("account", secretDTOV2);
      fail("Execution should not reach here");
    } catch (InvalidRequestException invalidRequestException) {
      // not required
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdate() {
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder().type(SecretType.SecretText).build();
    when(secretTextService.update(any(), any())).thenReturn(true);
    when(ngSecretServiceV2.update(any(), any(), eq(false))).thenReturn(true);

    boolean success = secretCrudService.update("account", secretDTOV2);

    assertThat(success).isTrue();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateFile() throws IOException {
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder().spec(SecretFileSpecDTO.builder().build()).build();
    Secret secret = Secret.builder().build();
    EncryptedDataDTO encryptedDataDTO = EncryptedDataDTO.builder().build();
    when(secretManagerClient.createSecretFile(any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    when(ngSecretServiceV2.create(any(), any(), eq(false))).thenReturn(secret);
    doNothing().when(secretEntityReferenceHelper).createEntityReferenceForSecret(any());

    SecretResponseWrapper created =
        secretCrudService.createFile("account", secretDTOV2, new StringInputStream("string"));
    assertThat(created).isNotNull();

    verify(secretManagerClient, atLeastOnce()).createSecretFile(any(), any());
    verify(ngSecretServiceV2).create(any(), any(), eq(false));
    verify(secretEntityReferenceHelper).createEntityReferenceForSecret(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateFile_failDueToSecretManagerChangeNotAllowed() throws IOException {
    EncryptedDataDTO encryptedDataDTO =
        EncryptedDataDTO.builder().type(SecretType.SecretFile).secretManager("secretManager2").build();
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .spec(SecretFileSpecDTO.builder().secretManagerIdentifier("secretManager1").build())
                                  .build();

    try {
      secretCrudService.updateFile("account", secretDTOV2, new StringInputStream("string"));
      fail("Execution should not reach here");
    } catch (InvalidRequestException invalidRequestException) {
      // not required
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateFile() throws IOException {
    EncryptedDataDTO encryptedDataDTO =
        EncryptedDataDTO.builder().type(SecretType.SecretFile).secretManager("secretManager1").build();
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .spec(SecretFileSpecDTO.builder().secretManagerIdentifier("secretManager1").build())
                                  .build();
    when(secretManagerClient.updateSecretFile(any(), any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    when(ngSecretServiceV2.update(any(), any(), eq(false))).thenReturn(true);

    boolean success = secretCrudService.updateFile("account", secretDTOV2, new StringInputStream("string"));

    assertThat(success).isTrue();
    verify(secretManagerClient, atLeastOnce()).getSecret(any(), any(), any(), any());
    verify(secretManagerClient, atLeastOnce()).updateSecretFile(any(), any(), any(), any(), any(), any());
    verify(ngSecretServiceV2).update(any(), any(), eq(false));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidateSecret() {
    SecretValidationResultDTO secretValidationResultDTO = SecretValidationResultDTO.builder().success(true).build();
    when(ngSecretServiceV2.validateSecret(any(), any(), any(), any(), any())).thenReturn(secretValidationResultDTO);
    SecretValidationResultDTO resultDTO = secretCrudService.validateSecret(
        "account", "org", "project", "identifier", SSHKeyValidationMetadata.builder().host("host").build());
    assertThat(resultDTO).isNotNull();
  }
}

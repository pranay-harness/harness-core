package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.NGSecretServiceV2;
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

import software.wings.app.FileUploadLimit;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import retrofit2.Response;

public class SecretCrudServiceImplTest extends CategoryTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private SecretManagerClient secretManagerClient;
  @Mock private SecretModifyService secretTextService;
  @Mock private SecretModifyService secretFileService;
  @Mock private SecretModifyService sshService;
  @Mock private NGSecretServiceV2 ngSecretServiceV2;
  private final FileUploadLimit fileUploadLimit = new FileUploadLimit();
  @Mock private SecretEntityReferenceHelper secretEntityReferenceHelper;
  @Mock private SecretCrudServiceImpl secretCrudServiceSpy;
  @Mock private SecretCrudServiceImpl secretCrudService;
  @Mock private Producer eventProducer;

  @Before
  public void setup() {
    initMocks(this);
    secretCrudServiceSpy = new SecretCrudServiceImpl(secretManagerClient, secretTextService, secretFileService,
        sshService, secretEntityReferenceHelper, fileUploadLimit, ngSecretServiceV2, eventProducer);
    secretCrudService = spy(secretCrudServiceSpy);
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
  public void testCreateViaYaml() {
    EncryptedDataDTO encryptedDataDTO = EncryptedDataDTO.builder().type(SecretType.SecretText).build();
    Secret secret = Secret.builder().build();
    when(secretTextService.create(any(), any())).thenReturn(encryptedDataDTO);
    when(ngSecretServiceV2.create(any(), any(), eq(true))).thenReturn(secret);

    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .type(SecretType.SecretText)
                                  .spec(SecretTextSpecDTO.builder().valueType(ValueType.Inline).build())
                                  .build();
    SecretResponseWrapper responseWrapper = secretCrudService.createViaYaml("account", secretDTOV2);
    assertThat(responseWrapper).isNotNull();

    verify(secretTextService).create(any(), any());
    verify(ngSecretServiceV2).create(any(), any(), eq(true));
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
    when(secretTextService.update(any(), any(), any())).thenReturn(true);
    when(ngSecretServiceV2.update(any(), any(), eq(false)))
        .thenReturn(
            Secret.builder().identifier("secret").accountIdentifier("account").identifier("identifier").build());
    doReturn(Optional.ofNullable(SecretResponseWrapper.builder().secret(secretDTOV2).build()))
        .when(secretCrudService)
        .get(any(), any(), any(), any());

    SecretResponseWrapper updatedSecret = secretCrudService.update("account", null, null, "identifier", secretDTOV2);

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (EventsFrameworkDownException e) {
      e.printStackTrace();
    }

    assertThat(updatedSecret).isNotNull();
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
    doNothing().when(secretEntityReferenceHelper).createSetupUsageForSecretManager(any());

    SecretResponseWrapper created =
        secretCrudService.createFile("account", secretDTOV2, new StringInputStream("string"));
    assertThat(created).isNotNull();

    verify(secretManagerClient, atLeastOnce()).createSecretFile(any(), any());
    verify(ngSecretServiceV2).create(any(), any(), eq(false));
    verify(secretEntityReferenceHelper).createSetupUsageForSecretManager(any());
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
    doReturn(Optional.ofNullable(SecretResponseWrapper.builder().secret(secretDTOV2).build()))
        .when(secretCrudService)
        .get(any(), any(), any(), any());

    try {
      secretCrudService.updateFile("account", null, null, "identifier", secretDTOV2, new StringInputStream("string"));
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
    when(ngSecretServiceV2.update(any(), any(), eq(false)))
        .thenReturn(Secret.builder().identifier("secret").accountIdentifier("account").build());
    doReturn(Optional.ofNullable(SecretResponseWrapper.builder().secret(secretDTOV2).build()))
        .when(secretCrudService)
        .get(any(), any(), any(), any());

    SecretResponseWrapper updatedFile =
        secretCrudService.updateFile("account", null, null, "identifier", secretDTOV2, new StringInputStream("string"));

    ArgumentCaptor<Message> producerMessage = ArgumentCaptor.forClass(Message.class);
    try {
      verify(eventProducer, times(1)).send(producerMessage.capture());
    } catch (EventsFrameworkDownException e) {
      e.printStackTrace();
    }

    assertThat(updatedFile).isNotNull();
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

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testGet() {
    when(ngSecretServiceV2.get(any(), any(), any(), any())).thenReturn(Optional.ofNullable(Secret.builder().build()));
    Optional<SecretResponseWrapper> secretResponseWrapper = secretCrudService.get("account", null, null, "identifier");
    assertThat(secretResponseWrapper).isPresent();
    verify(ngSecretServiceV2).get(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testList() {
    when(ngSecretServiceV2.list(any(), anyInt(), anyInt()))
        .thenReturn(new PageImpl<>(Lists.newArrayList(Secret.builder().build()), PageRequest.of(0, 10), 1));
    PageResponse<SecretResponseWrapper> secretPage = secretCrudService.list(
        "account", "org", "proj", Collections.emptyList(), singletonList(SecretType.SSHKey), false, "abc", 0, 100);
    assertThat(secretPage.getContent()).isNotEmpty();
    assertThat(secretPage.getContent().size()).isEqualTo(1);
    verify(ngSecretServiceV2).list(any(), anyInt(), anyInt());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDelete() throws IOException {
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    when(secretManagerClient.deleteSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    when(ngSecretServiceV2.delete(any(), any(), any(), any())).thenReturn(true);
    doNothing().when(secretEntityReferenceHelper).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());
    when(ngSecretServiceV2.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    boolean success = secretCrudService.delete("account", null, null, "identifier");

    assertThat(success).isTrue();
    verify(secretManagerClient, atLeastOnce()).getSecret(any(), any(), any(), any());
    verify(secretManagerClient, atLeastOnce()).deleteSecret(any(), any(), any(), any());
    verify(ngSecretServiceV2, atLeastOnce()).delete(any(), any(), any(), any());
    verify(secretEntityReferenceHelper, atLeastOnce()).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());
  }
}

package io.harness.ng.core.api.impl;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.SECRETS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.entitysetupusageclient.EntitySetupUsageHelper;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
public class SecretEntityReferenceHelperTest extends CategoryTest {
  @InjectMocks SecretEntityReferenceHelper secretEntityReferenceHelper;
  @Mock EntitySetupUsageClient entityReferenceClient;
  @Mock AbstractProducer abstractProducer;
  @Mock EntitySetupUsageHelper entityReferenceHelper;
  @Mock IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(anyString(), anyString(), anyString(), anyString()))
        .thenCallRealMethod();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createEntityReferenceForSecret() {
    String account = "account";
    String org = "org";
    String project = "project";
    String secretName = "secretName";
    String identifier = "identifier";
    String secretManager = "secretManager";
    String secretManagerName = "secretManagerName";
    EncryptedDataDTO encryptedDataDTO = EncryptedDataDTO.builder()
                                            .account(account)
                                            .org(org)
                                            .project(project)
                                            .name(secretName)
                                            .identifier(identifier)
                                            .secretManager(secretManager)
                                            .secretManagerName(secretManagerName)
                                            .build();
    when(entityReferenceHelper.createEntityReference(anyString(), any(), any())).thenCallRealMethod();
    secretEntityReferenceHelper.createSetupUsageForSecretManager(encryptedDataDTO);
    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    try {
      verify(abstractProducer, times(1)).send(argumentCaptor.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }
    EntitySetupUsageCreateDTO entityReferenceDTO = null;
    try {
      entityReferenceDTO = EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());
    } catch (Exception ex) {
      log.error("Unexpected error :", ex);
    }
    assertThat(entityReferenceDTO.getReferredEntity().getName()).isEqualTo(secretManagerName);
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(CONNECTORS.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(account);
    assertThat(entityReferenceDTO.getReferredByEntity().getName()).isEqualTo(secretName);
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(SECRETS.name());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void deleteSecretEntityReferenceWhenSecretGetsDeleted() {
    String account = "account";
    String org = "org";
    String project = "project";
    String secretName = "secretName";
    String identifier = "identifier";
    String secretManager = "secretManager";
    String secretManagerName = "secretManagerName";
    EncryptedDataDTO encryptedDataDTO = EncryptedDataDTO.builder()
                                            .account(account)
                                            .org(org)
                                            .project(project)
                                            .name(secretName)
                                            .identifier(identifier)
                                            .secretManager(secretManager)
                                            .secretManagerName(secretManagerName)
                                            .build();
    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(encryptedDataDTO);
    try {
      verify(abstractProducer, times(1)).send(argumentCaptor.capture());
    } catch (ProducerShutdownException e) {
      e.printStackTrace();
    }
    DeleteSetupUsageDTO deleteSetupUsageDTO = null;
    try {
      deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());
    } catch (Exception ex) {
      log.error("Exception in the event framework");
    }
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(account);
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, identifier));
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, identifier));
  }
}

package io.harness.ng.core.api.impl;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.SECRETS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.entityreferenceclient.remote.EntityReferenceClient;
import io.harness.ng.core.entityReference.EntityReferenceHelper;
import io.harness.ng.core.entityreference.dto.EntityReferenceDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class SecretEntityReferenceHelperTest extends CategoryTest {
  @InjectMocks SecretEntityReferenceHelper secretEntityReferenceHelper;
  @Mock EntityReferenceClient entityReferenceClient;
  @Mock EntityReferenceHelper entityReferenceHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
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
    when(entityReferenceHelper.createEntityReference(
             anyString(), anyString(), Matchers.any(), anyString(), anyString(), anyString(), Matchers.any()))
        .thenCallRealMethod();
    secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedDataDTO);
    ArgumentCaptor<EntityReferenceDTO> argumentCaptor = ArgumentCaptor.forClass(EntityReferenceDTO.class);
    verify(entityReferenceClient, times(1)).save(argumentCaptor.capture());
    EntityReferenceDTO entityReferenceDTO = argumentCaptor.getValue();
    assertThat(entityReferenceDTO.getReferredEntityName()).isEqualTo(secretManagerName);
    assertThat(entityReferenceDTO.getReferredEntityType()).isEqualTo(CONNECTORS);
    assertThat(entityReferenceDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, secretManager));
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(account);
    assertThat(entityReferenceDTO.getReferredByEntityName()).isEqualTo(secretName);
    assertThat(entityReferenceDTO.getReferredByEntityType()).isEqualTo(SECRETS);
    assertThat(entityReferenceDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, identifier));
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
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(encryptedDataDTO);
    verify(entityReferenceClient, times(1)).delete(argumentCaptor.capture(), argumentCaptor.capture());
    List<String> stringArguments = argumentCaptor.getAllValues();
    assertThat(stringArguments.get(0))
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, secretManager));
    assertThat(stringArguments.get(1))
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, identifier));
  }
}
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
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SecretEntityReferenceHelperTest extends CategoryTest {
  @InjectMocks SecretEntityReferenceHelper secretEntityReferenceHelper;
  @Mock EntitySetupUsageClient entityReferenceClient;
  @Mock EntitySetupUsageHelper entityReferenceHelper;

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
    when(entityReferenceHelper.createEntityReference(anyString(), any(), any())).thenCallRealMethod();
    secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedDataDTO);
    ArgumentCaptor<EntitySetupUsageDTO> argumentCaptor = ArgumentCaptor.forClass(EntitySetupUsageDTO.class);
    verify(entityReferenceClient, times(1)).save(argumentCaptor.capture());
    EntitySetupUsageDTO entityReferenceDTO = argumentCaptor.getValue();
    assertThat(entityReferenceDTO.getReferredEntity().getName()).isEqualTo(secretManagerName);
    assertThat(entityReferenceDTO.getReferredEntity().getType()).isEqualTo(CONNECTORS);
    assertThat(entityReferenceDTO.getReferredEntity().getEntityRef().getFullyQualifiedName())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, secretManager));
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(account);
    assertThat(entityReferenceDTO.getReferredByEntity().getName()).isEqualTo(secretName);
    assertThat(entityReferenceDTO.getReferredByEntity().getType()).isEqualTo(SECRETS);
    assertThat(entityReferenceDTO.getReferredByEntity().getEntityRef().getFullyQualifiedName())
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
    verify(entityReferenceClient, times(1))
        .deleteAllReferredByEntityRecords(argumentCaptor.capture(), argumentCaptor.capture());
    List<String> stringArguments = argumentCaptor.getAllValues();
    assertThat(stringArguments.get(0)).isEqualTo(account);
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(account, org, project, identifier);
    assertThat(stringArguments.get(1)).isEqualTo(secretFQN);
  }
}

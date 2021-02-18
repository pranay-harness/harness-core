package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.SecretRefData.SECRET_DOT_DELIMINITER;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SplunkDTOToEntityTest extends CategoryTest {
  @InjectMocks SplunkDTOToEntity splunkDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToSplunkConnector() {
    String username = "username";
    String passwordRefIdentifier = "encryptedPassword";
    String splunkUrl = "splunkUrl";
    String accountId = "accountId";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRefIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());

    SplunkConnectorDTO splunkConnectorDTO = SplunkConnectorDTO.builder()
                                                .username(username)
                                                .passwordRef(passwordSecretRef)
                                                .splunkUrl(splunkUrl)
                                                .accountId(accountId)
                                                .build();

    SplunkConnector splunkConnector = splunkDTOToEntity.toConnectorEntity(
        splunkConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(splunkConnector).isNotNull();
    assertThat(splunkConnector.getUsername()).isEqualTo(splunkConnectorDTO.getUsername());
    assertThat(splunkConnector.getPasswordRef()).isNotNull();
    assertThat(splunkConnector.getPasswordRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + SECRET_DOT_DELIMINITER
            + splunkConnectorDTO.getPasswordRef().getIdentifier());
    assertThat(splunkConnector.getSplunkUrl()).isEqualTo(splunkConnectorDTO.getSplunkUrl());
    assertThat(splunkConnector.getAccountId()).isEqualTo(splunkConnectorDTO.getAccountId());
  }
}

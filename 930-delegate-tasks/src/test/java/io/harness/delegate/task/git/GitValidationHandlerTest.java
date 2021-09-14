/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class GitValidationHandlerTest extends CategoryTest {
  @InjectMocks GitValidationHandler gitValidationHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidationForAccountLevelConnector() {
    ScmValidationParams gitValidationParameters =
        ScmValidationParams.builder()
            .gitConfigDTO(GitConfigDTO.builder()
                              .gitConnectionType(GitConnectionType.ACCOUNT)
                              .gitAuth(GitHTTPAuthenticationDTO.builder()
                                           .username("username")
                                           .passwordRef(SecretRefData.builder().identifier("passwordRef").build())
                                           .build())
                              .gitAuthType(GitAuthType.HTTP)
                              .build())
            .build();
    ConnectorValidationResult validationResult =
        gitValidationHandler.validate(gitValidationParameters, "accountIdentifier");
    assertThat(validationResult.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
  }
}

package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.Resource;
import io.harness.rule.Owner;
import io.harness.scope.ResourceScope;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class AuditFilterPropertiesValidatorTest extends CategoryTest {
  private AuditFilterPropertiesValidator auditFilterPropertiesValidator;

  @Before
  public void setup() {
    auditFilterPropertiesValidator = spy(new AuditFilterPropertiesValidator());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidScopeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidScopeFilter =
        AuditFilterPropertiesDTO.builder()
            .scopes(singletonList(ResourceScope.builder().accountIdentifier(accountIdentifier + "K").build()))
            .build();
    try {
      auditFilterPropertiesValidator.validate(accountIdentifier, invalidScopeFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidResourceAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidResourceFilter =
        AuditFilterPropertiesDTO.builder()
            .resources(singletonList(Resource.builder().identifier(identifier).build()))
            .build();
    try {
      auditFilterPropertiesValidator.validate(accountIdentifier, invalidResourceFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidPrincipalAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidPrincipalFilter =
        AuditFilterPropertiesDTO.builder()
            .principals(singletonList(Principal.builder().identifier(identifier).build()))
            .build();
    try {
      auditFilterPropertiesValidator.validate(accountIdentifier, invalidPrincipalFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidTimeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidTimeFilter = AuditFilterPropertiesDTO.builder().startTime(18L).endTime(17L).build();
    try {
      auditFilterPropertiesValidator.validate(accountIdentifier, invalidTimeFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
  }
}

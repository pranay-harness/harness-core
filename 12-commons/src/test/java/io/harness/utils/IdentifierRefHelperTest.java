package io.harness.utils;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IdentifierRefHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testProjectLevelScopeIdentifierRef() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    IdentifierRef expected = IdentifierRef.builder()
                                 .scope(Scope.PROJECT)
                                 .accountId(accountIdentifier)
                                 .orgIdentifier(orgIdentifier)
                                 .projectIdentifier(projectIdentifier)
                                 .identifier(identifier)
                                 .build();

    assertThat(identifierRef).isEqualTo(expected);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testOrgLevelScopeIdentifierRef() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = null;
    String identifier = "identifier";
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef("org." + identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    IdentifierRef expected = IdentifierRef.builder()
                                 .scope(Scope.ORG)
                                 .accountId(accountIdentifier)
                                 .orgIdentifier(orgIdentifier)
                                 .identifier(identifier)
                                 .build();

    assertThat(identifierRef).isEqualTo(expected);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testAccountLevelScopeIdentifierRef() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = null;
    String projectIdentifier = null;
    String identifier = "identifier";
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef("acc." + identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    IdentifierRef expected = IdentifierRef.builder()
                                 .scope(Scope.ACCOUNT)
                                 .accountId(accountIdentifier)
                                 .orgIdentifier(orgIdentifier)
                                 .projectIdentifier(projectIdentifier)
                                 .identifier(identifier)
                                 .build();

    assertThat(identifierRef).isEqualTo(expected);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testInvalidScopeInIdentifierStringThrowsException() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "proj.identifier";

    assertThatThrownBy(
        () -> IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierRefStringProjLevel() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .scope(Scope.PROJECT)
                                      .accountId(accountIdentifier)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .identifier(identifier)
                                      .build();

    String fullyQualifiedIdentifier = IdentifierRefHelper.getFullyQualifiedIdentifierRefString(identifierRef);
    assertThat(fullyQualifiedIdentifier).isEqualTo("accountIdentifier/orgIdentifier/projectIdentifier/identifier");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierRefStringOrgLevel() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String identifier = "identifier";
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .scope(Scope.PROJECT)
                                      .accountId(accountIdentifier)
                                      .orgIdentifier(orgIdentifier)
                                      .identifier(identifier)
                                      .build();

    String fullyQualifiedIdentifier = IdentifierRefHelper.getFullyQualifiedIdentifierRefString(identifierRef);
    assertThat(fullyQualifiedIdentifier).isEqualTo("accountIdentifier/orgIdentifier/identifier");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierRefStringAccountLevel() {
    String accountIdentifier = "accountIdentifier";
    String identifier = "identifier";
    IdentifierRef identifierRef =
        IdentifierRef.builder().scope(Scope.PROJECT).accountId(accountIdentifier).identifier(identifier).build();

    String fullyQualifiedIdentifier = IdentifierRefHelper.getFullyQualifiedIdentifierRefString(identifierRef);
    assertThat(fullyQualifiedIdentifier).isEqualTo("accountIdentifier/identifier");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testFullyQualifiedIdentifierOfAccountLevelIdentifier() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "acc.identifier";
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    String fullyQualifiedIdentifier = IdentifierRefHelper.getFullyQualifiedIdentifierRefString(identifierRef);
    assertThat(fullyQualifiedIdentifier).isEqualTo("accountIdentifier/identifier");
  }
}
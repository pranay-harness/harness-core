/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
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
                                 .accountIdentifier(accountIdentifier)
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
                                 .accountIdentifier(accountIdentifier)
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
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        "account." + identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    IdentifierRef expected = IdentifierRef.builder()
                                 .scope(Scope.ACCOUNT)
                                 .accountIdentifier(accountIdentifier)
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
        .isInstanceOf(IllegalArgumentException.class);
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
                                      .accountIdentifier(accountIdentifier)
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
                                      .accountIdentifier(accountIdentifier)
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
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .scope(Scope.PROJECT)
                                      .accountIdentifier(accountIdentifier)
                                      .identifier(identifier)
                                      .build();

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
    String identifier = "account.identifier";
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier);

    String fullyQualifiedIdentifier = IdentifierRefHelper.getFullyQualifiedIdentifierRefString(identifierRef);
    assertThat(fullyQualifiedIdentifier).isEqualTo("accountIdentifier/identifier");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testGetIdentifier() {
    String identifierRefAccount = "account.identifier";
    String identifierRefOrg = "org.identifier";
    String identifierRefProj = "identifier";

    assertThat(IdentifierRefHelper.getIdentifier(identifierRefAccount)).isEqualTo("identifier");
    assertThat(IdentifierRefHelper.getIdentifier(identifierRefOrg)).isEqualTo("identifier");
    assertThat(IdentifierRefHelper.getIdentifier(identifierRefProj)).isEqualTo("identifier");
  }
}

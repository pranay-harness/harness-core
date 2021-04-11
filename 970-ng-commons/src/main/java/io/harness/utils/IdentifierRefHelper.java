package io.harness.utils;

import io.harness.beans.IdentifierRef;
import io.harness.beans.IdentifierRef.IdentifierRefBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.encryption.ScopeHelper;
import io.harness.exception.InvalidRequestException;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierRefHelper {
  public final String IDENTIFIER_REF_DELIMITER = "\\."; // check if this is the correct delimiter

  public IdentifierRef createIdentifierRefWithUnknownScope(String accountId, String orgIdentifier,
      String projectIdentifier, String unknownIdentifier, Map<String, String> metadata) {
    return IdentifierRef.builder()
        .scope(Scope.UNKNOWN)
        .metadata(metadata)
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(unknownIdentifier)
        .build();
  }

  public IdentifierRef getIdentifierRef(
      String scopedIdentifierConfig, String accountId, String orgIdentifier, String projectIdentifier) {
    return getIdentifierRef(scopedIdentifierConfig, accountId, orgIdentifier, projectIdentifier, null);
  }

  public IdentifierRef getIdentifierRef(String scopedIdentifierConfig, String accountId, String orgIdentifier,
      String projectIdentifier, Map<String, String> metadata) {
    Scope scope;
    String identifier;
    IdentifierRefBuilder identifierRefBuilder = IdentifierRef.builder().accountIdentifier(accountId);

    if (EmptyPredicate.isNotEmpty(metadata)) {
      identifierRefBuilder.metadata(metadata);
    }

    if (EmptyPredicate.isEmpty(scopedIdentifierConfig)) {
      throw new InvalidRequestException("Identifier reference should not be empty");
    }
    String[] identifierConfigStringSplit = scopedIdentifierConfig.split(IDENTIFIER_REF_DELIMITER);

    if (identifierConfigStringSplit.length == 1) {
      identifier = identifierConfigStringSplit[0];
      scope = Scope.PROJECT;
      return identifierRefBuilder.orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .identifier(identifier)
          .scope(scope)
          .build();
    } else if (identifierConfigStringSplit.length == 2) {
      identifier = identifierConfigStringSplit[1];
      scope = getScope(identifierConfigStringSplit[0]);
      identifierRefBuilder = identifierRefBuilder.identifier(identifier).scope(scope);
      if (scope == Scope.PROJECT || scope == null) {
        throw new InvalidRequestException("Invalid Identifier Reference, Scope.PROJECT invalid.");
      } else if (scope == Scope.ORG) {
        return identifierRefBuilder.orgIdentifier(orgIdentifier).build();
      }
      return identifierRefBuilder.build();
    } else {
      throw new InvalidRequestException("Invalid Identifier Reference.");
    }
  }

  public IdentifierRef getIdentifierRefFromEntityIdentifiers(
      String entityIdentifier, String accountId, String orgIdentifier, String projectIdentifier) {
    return IdentifierRef.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(entityIdentifier)
        .scope(ScopeHelper.getScope(accountId, orgIdentifier, projectIdentifier))
        .build();
  }

  public Scope getScope(String identifierScopeString) {
    if (EmptyPredicate.isEmpty(identifierScopeString)) {
      return null;
    }
    return Scope.fromString(identifierScopeString);
  }

  public String getFullyQualifiedIdentifierRefString(IdentifierRef identifierRef) {
    if (identifierRef == null) {
      return null;
    }

    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }

  public String getIdentifier(String scopedIdentifierConfig) {
    if (EmptyPredicate.isEmpty(scopedIdentifierConfig)) {
      throw new InvalidRequestException("scopedIdentifierConfig is null");
    }
    String identifier;
    String[] identifierConfigStringSplit = scopedIdentifierConfig.split(IDENTIFIER_REF_DELIMITER);
    if (identifierConfigStringSplit.length == 1) {
      identifier = identifierConfigStringSplit[0];
    } else if (identifierConfigStringSplit.length == 2) {
      identifier = identifierConfigStringSplit[1];
      Scope scope = getScope(identifierConfigStringSplit[0]);
      if (scope == Scope.PROJECT || scope == null) {
        throw new InvalidRequestException("Invalid Identifier Reference, Scope.PROJECT invalid.");
      }
    } else {
      throw new InvalidRequestException("Invalid Identifier Reference.");
    }
    return identifier;
  }
}

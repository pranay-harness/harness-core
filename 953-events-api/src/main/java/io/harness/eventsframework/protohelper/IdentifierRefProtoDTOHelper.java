/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.protohelper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.Map;

@OwnedBy(PL)
@Singleton
public class IdentifierRefProtoDTOHelper {
  public IdentifierRefProtoDTO createIdentifierRefProtoDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return createIdentifierRefProtoDTO(accountIdentifier, orgIdentifier, projectIdentifier, identifier, null);
  }

  public IdentifierRefProtoDTO createIdentifierRefProtoDTO(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, Map<String, String> metadata) {
    Scope scope = ScopeHelper.getScope(accountIdentifier, orgIdentifier, projectIdentifier);
    if (scope == null) {
      throw new InvalidRequestException("Scope can't be null");
    }
    IdentifierRefProtoDTO.Builder identifierRefBuilder = IdentifierRefProtoDTO.newBuilder()
                                                             .setIdentifier(StringValue.of(identifier))
                                                             .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                             .setScope(ScopeProtoEnum.valueOf(scope.toString()));
    if (isNotBlank(orgIdentifier)) {
      identifierRefBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
    }

    if (isNotBlank(projectIdentifier)) {
      identifierRefBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
    }

    if (EmptyPredicate.isNotEmpty(metadata)) {
      identifierRefBuilder.putAllMetadata(metadata);
    }

    return identifierRefBuilder.build();
  }
}

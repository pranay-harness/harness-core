/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;

import com.google.protobuf.StringValue;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierRefProtoUtils {
  public IdentifierRefProtoDTO createIdentifierRefProtoFromIdentifierRef(IdentifierRef identifierRef) {
    String accountIdentifier = identifierRef.getAccountIdentifier();
    String projectIdentifier = identifierRef.getProjectIdentifier();
    String orgIdentifier = identifierRef.getOrgIdentifier();
    String identifier = identifierRef.getIdentifier();
    Scope scope = identifierRef.getScope();

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
    if (EmptyPredicate.isNotEmpty(identifierRef.getMetadata())) {
      identifierRefBuilder.putAllMetadata(identifierRef.getMetadata());
    }
    return identifierRefBuilder.build();
  }
}

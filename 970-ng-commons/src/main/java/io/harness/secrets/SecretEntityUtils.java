/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.IdentifierRefProtoUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class SecretEntityUtils {
  public EntityDetailProtoDTO convertSecretToEntityDetailProtoDTO(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String fullQualifiedDomainName, ParameterField<SecretRefData> secretRef) {
    Map<String, String> metadata =
        new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, fullQualifiedDomainName));
    if (!secretRef.isExpression()) {
      SecretRefData secretRefData = secretRef.getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(secretRefData.getScope(),
          secretRefData.getIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.SECRETS)
          .build();
    } else {
      metadata.put(PreFlightCheckMetadata.EXPRESSION, secretRef.getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(
          accountIdentifier, orgIdentifier, projectIdentifier, secretRef.getExpressionValue(), metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.SECRETS)
          .build();
    }
  }
}

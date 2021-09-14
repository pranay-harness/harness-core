/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuthenticationInfoMapper {
  public static AuthenticationInfo fromDTO(AuthenticationInfoDTO dto) {
    List<KeyValuePair> labels = KeyValuePairMapper.convertToList(dto.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return AuthenticationInfo.builder().principal(dto.getPrincipal()).labels(labels).build();
  }

  public static AuthenticationInfoDTO toDTO(AuthenticationInfo dbo) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(dbo.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return AuthenticationInfoDTO.builder().principal(dbo.getPrincipal()).labels(labels).build();
  }
}

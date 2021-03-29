package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.AuthenticationInfoDBO;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuthenticationInfoMapper {
  public static AuthenticationInfoDBO fromDTO(AuthenticationInfo dto) {
    List<KeyValuePair> labels = KeyValuePairMapper.convertToList(dto.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return AuthenticationInfoDBO.builder().principal(dto.getPrincipal()).labels(labels).build();
  }

  public static AuthenticationInfo toDTO(AuthenticationInfoDBO resourceDBO) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(resourceDBO.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return AuthenticationInfo.builder().principal(resourceDBO.getPrincipal()).labels(labels).build();
  }
}

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

  public static AuthenticationInfo toDTO(AuthenticationInfoDBO dbo) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(dbo.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return AuthenticationInfo.builder().principal(dbo.getPrincipal()).labels(labels).build();
  }
}

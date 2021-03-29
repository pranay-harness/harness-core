package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.ResourceScopeDBO;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;
import io.harness.scope.ResourceScope;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceScopeMapper {
  public static ResourceScopeDBO fromDTO(ResourceScope dto) {
    List<KeyValuePair> labels = KeyValuePairMapper.convertToList(dto.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return ResourceScopeDBO.builder()
        .accountIdentifier(dto.getAccountIdentifier())
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .labels(labels)
        .build();
  }

  public static ResourceScope toDTO(ResourceScopeDBO dbo) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(dbo.getLabels());
    if (isEmpty(labels)) {
      labels = null;
    }
    return ResourceScope.builder()
        .accountIdentifier(dbo.getAccountIdentifier())
        .orgIdentifier(dbo.getOrgIdentifier())
        .projectIdentifier(dbo.getProjectIdentifier())
        .labels(labels)
        .build();
  }
}

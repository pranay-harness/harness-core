package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.NGCommonEntityConstants;
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
    if (isNotEmpty(dto.getOrgIdentifier())) {
      labels.add(KeyValuePair.builder().key(NGCommonEntityConstants.ORG_KEY).value(dto.getOrgIdentifier()).build());
    }
    if (isNotEmpty(dto.getProjectIdentifier())) {
      labels.add(
          KeyValuePair.builder().key(NGCommonEntityConstants.PROJECT_KEY).value(dto.getProjectIdentifier()).build());
    }
    if (isEmpty(labels)) {
      labels = null;
    }
    return ResourceScopeDBO.builder().accountIdentifier(dto.getAccountIdentifier()).labels(labels).build();
  }

  public static ResourceScope toDTO(ResourceScopeDBO resourceDBO) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(resourceDBO.getLabels());
    String orgIdentifier = labels.get(NGCommonEntityConstants.ORG_KEY);
    String projectIdentifier = labels.get(NGCommonEntityConstants.PROJECT_KEY);
    labels.remove(NGCommonEntityConstants.ORG_KEY);
    labels.remove(NGCommonEntityConstants.PROJECT_KEY);
    if (isEmpty(labels)) {
      labels = null;
    }
    return ResourceScope.builder()
        .accountIdentifier(resourceDBO.getAccountIdentifier())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .labels(labels)
        .build();
  }
}

package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.AuditCommonConstants;
import io.harness.audit.beans.ResourceDBO;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceMapper {
  public static ResourceDBO fromDTO(Resource dto) {
    List<KeyValuePair> labels = KeyValuePairMapper.convertToList(dto.getLabels());
    labels.add(KeyValuePair.builder().key(AuditCommonConstants.TYPE).value(dto.getType()).build());
    labels.add(KeyValuePair.builder().key(AuditCommonConstants.IDENTIFIER).value(dto.getIdentifier()).build());
    return ResourceDBO.builder().labels(labels).build();
  }

  public static Resource toDTO(ResourceDBO dbo) {
    Map<String, String> labels = KeyValuePairMapper.convertToMap(dbo.getLabels());
    String type = labels.get(AuditCommonConstants.TYPE);
    String identifier = labels.get(AuditCommonConstants.IDENTIFIER);
    labels.remove(AuditCommonConstants.TYPE);
    labels.remove(AuditCommonConstants.IDENTIFIER);
    if (isEmpty(labels)) {
      labels = null;
    }
    return Resource.builder().type(type).identifier(identifier).labels(labels).build();
  }
}

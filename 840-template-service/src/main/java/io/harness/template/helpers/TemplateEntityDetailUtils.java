package io.harness.template.helpers;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.NGTemplateReference;
import io.harness.encryption.ScopeHelper;
import io.harness.ng.core.EntityDetail;
import io.harness.template.entity.TemplateEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDC)
public class TemplateEntityDetailUtils {
  public EntityDetail getEntityDetail(TemplateEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.TEMPLATE)
        .entityRef(NGTemplateReference.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .scope(ScopeHelper.getScope(
                           entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier()))
                       .identifier(entity.getIdentifier())
                       .versionLabel(entity.getVersionLabel())
                       .build())
        .build();
  }
}

package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.ResourceDBO;
import io.harness.ng.core.Resource;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceMapper {
  public static ResourceDBO fromDTO(Resource dto) {
    return ResourceDBO.builder().type(dto.getType()).identifier(dto.getIdentifier()).build();
  }

  public static Resource toDTO(ResourceDBO dbo) {
    return Resource.builder().type(dbo.getType()).identifier(dbo.getIdentifier()).build();
  }
}

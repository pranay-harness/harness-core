package io.harness.filter.mapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;

@OwnedBy(DX)
public interface FilterPropertiesMapper<T extends FilterPropertiesDTO, S extends FilterProperties> {
  FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO);

  FilterPropertiesDTO writeDTO(FilterProperties filterProperties);
}

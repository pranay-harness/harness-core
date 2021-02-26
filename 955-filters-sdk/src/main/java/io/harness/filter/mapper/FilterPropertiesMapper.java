package io.harness.filter.mapper;

import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;

public interface FilterPropertiesMapper<T extends FilterPropertiesDTO, S extends FilterProperties> {
  FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO);
  FilterPropertiesDTO writeDTO(FilterProperties filterProperties);
}

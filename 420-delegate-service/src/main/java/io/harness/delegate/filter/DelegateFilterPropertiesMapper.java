/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.filter;

import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.core.mapper.TagMapper;

import com.google.inject.Singleton;
import org.modelmapper.ModelMapper;

@Singleton
public class DelegateFilterPropertiesMapper
    implements FilterPropertiesMapper<DelegateFilterPropertiesDTO, DelegateFilterProperties> {
  @Override
  public FilterPropertiesDTO writeDTO(FilterProperties filterProperties) {
    ModelMapper modelMapper = new ModelMapper();
    FilterPropertiesDTO filterPropertiesDTO = modelMapper.map(filterProperties, DelegateFilterPropertiesDTO.class);
    filterPropertiesDTO.setTags(TagMapper.convertToMap(filterProperties.getTags()));
    return filterPropertiesDTO;
  }

  @Override
  public FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO) {
    ModelMapper modelMapper = new ModelMapper();
    DelegateFilterProperties filterProperties = modelMapper.map(filterPropertiesDTO, DelegateFilterProperties.class);
    filterProperties.setType(filterPropertiesDTO.getFilterType());
    filterProperties.setTags(TagMapper.convertToList(filterPropertiesDTO.getTags()));
    return filterProperties;
  }
}

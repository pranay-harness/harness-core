/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.filter;

import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.entities.ConnectorFilterProperties;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.core.mapper.TagMapper;

import com.google.inject.Singleton;
import org.modelmapper.ModelMapper;

@Singleton
public class ConnectorFilterPropertiesMapper
    implements FilterPropertiesMapper<ConnectorFilterPropertiesDTO, ConnectorFilterProperties> {
  @Override
  public FilterPropertiesDTO writeDTO(FilterProperties filterProperties) {
    ModelMapper modelMapper = new ModelMapper();
    FilterPropertiesDTO filterPropertiesDTO = modelMapper.map(filterProperties, ConnectorFilterPropertiesDTO.class);
    filterPropertiesDTO.setTags(TagMapper.convertToMap(filterProperties.getTags()));
    return filterPropertiesDTO;
  }

  @Override
  public FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO) {
    ModelMapper modelMapper = new ModelMapper();
    ConnectorFilterProperties filterProperties = modelMapper.map(filterPropertiesDTO, ConnectorFilterProperties.class);
    filterProperties.setType(filterPropertiesDTO.getFilterType());
    filterProperties.setTags(TagMapper.convertToList(filterPropertiesDTO.getTags()));
    return filterProperties;
  }
}

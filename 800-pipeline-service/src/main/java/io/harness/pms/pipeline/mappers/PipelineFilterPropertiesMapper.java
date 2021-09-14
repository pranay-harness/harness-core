/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.mappers;

import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.pipeline.PipelineFilterProperties;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;

import org.modelmapper.ModelMapper;

public class PipelineFilterPropertiesMapper
    implements FilterPropertiesMapper<PipelineFilterPropertiesDto, PipelineFilterProperties> {
  @Override
  public FilterPropertiesDTO writeDTO(FilterProperties pipelineFilterProperties) {
    ModelMapper modelMapper = new ModelMapper();
    FilterPropertiesDTO filterPropertiesDTO =
        modelMapper.map(pipelineFilterProperties, PipelineFilterPropertiesDto.class);
    filterPropertiesDTO.setTags(TagMapper.convertToMap(pipelineFilterProperties.getTags()));
    return filterPropertiesDTO;
  }

  @Override
  public FilterProperties toEntity(FilterPropertiesDTO pipelineFilterPropertiesDto) {
    ModelMapper modelMapper = new ModelMapper();
    PipelineFilterProperties filterProperties =
        modelMapper.map(pipelineFilterPropertiesDto, PipelineFilterProperties.class);
    filterProperties.setType(pipelineFilterPropertiesDto.getFilterType());
    filterProperties.setTags(TagMapper.convertToList(pipelineFilterPropertiesDto.getTags()));
    return filterProperties;
  }
}

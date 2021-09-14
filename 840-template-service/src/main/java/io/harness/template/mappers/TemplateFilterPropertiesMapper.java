/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.template.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.template.beans.TemplateFilterProperties;
import io.harness.template.beans.TemplateFilterPropertiesDTO;

import org.modelmapper.ModelMapper;

@OwnedBy(HarnessTeam.CDC)
public class TemplateFilterPropertiesMapper
    implements FilterPropertiesMapper<TemplateFilterPropertiesDTO, TemplateFilterProperties> {
  @Override
  public FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO) {
    ModelMapper modelMapper = new ModelMapper();
    TemplateFilterProperties templateFilterProperties =
        modelMapper.map(filterPropertiesDTO, TemplateFilterProperties.class);
    templateFilterProperties.setType(filterPropertiesDTO.getFilterType());
    templateFilterProperties.setTags(TagMapper.convertToList(filterPropertiesDTO.getTags()));
    return templateFilterProperties;
  }

  @Override
  public FilterPropertiesDTO writeDTO(FilterProperties filterProperties) {
    ModelMapper modelMapper = new ModelMapper();
    TemplateFilterPropertiesDTO templateFilterPropertiesDTO =
        modelMapper.map(filterProperties, TemplateFilterPropertiesDTO.class);
    templateFilterPropertiesDTO.setFilterType(filterProperties.getType());
    templateFilterPropertiesDTO.setTags(TagMapper.convertToMap(filterProperties.getTags()));
    return templateFilterPropertiesDTO;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.filter.mapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.dto.FilterDTO.FilterDTOBuilder;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.dto.FilterVisibility;
import io.harness.filter.entity.Filter;
import io.harness.filter.entity.Filter.FilterBuilder;
import io.harness.filter.entity.FilterProperties;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
@OwnedBy(DX)
public class FilterMapper {
  @Inject Map<String, FilterPropertiesMapper> filterPropertiesMapperMap;

  public Filter toEntity(FilterDTO filterDTO, String accountId) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountId, filterDTO.getOrgIdentifier(), filterDTO.getProjectIdentifier(), filterDTO.getIdentifier());
    FilterBuilder filterBuilder =
        Filter.builder()
            .name(filterDTO.getName())
            .identifier(filterDTO.getIdentifier())
            .accountIdentifier(accountId)
            .orgIdentifier(filterDTO.getOrgIdentifier())
            .projectIdentifier(filterDTO.getProjectIdentifier())
            .fullyQualifiedIdentifier(fullyQualifiedIdentifier)
            .filterVisibility(
                filterDTO.getFilterVisibility() == null ? FilterVisibility.EVERYONE : filterDTO.getFilterVisibility());
    if (filterDTO.getFilterProperties() == null) {
      return filterBuilder.build();
    }
    filterBuilder.filterType(filterDTO.getFilterProperties().getFilterType());
    filterBuilder.filterProperties(getFilterPropertiesDTO(filterDTO.getFilterProperties()));
    populateUserInfoInTheFilter(filterDTO.getFilterVisibility(), filterBuilder);
    return filterBuilder.build();
  }

  private void populateUserInfoInTheFilter(FilterVisibility filterVisibility, FilterBuilder filterBuilder) {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      EmbeddedUser user = EmbeddedUser.builder().uuid(userPrincipal.getName()).build();
      filterBuilder.createdBy(user);
    }
  }

  private FilterProperties getFilterPropertiesDTO(FilterPropertiesDTO filterProperties) {
    FilterPropertiesMapper filterPropertiesMapper =
        filterPropertiesMapperMap.get(filterProperties.getFilterType().toString());
    return filterPropertiesMapper.toEntity(filterProperties);
  }

  public FilterDTO writeDTO(Filter filterEntity) {
    FilterDTOBuilder filterDTOBuilder = FilterDTO.builder()
                                            .name(filterEntity.getName())
                                            .identifier(filterEntity.getIdentifier())
                                            .orgIdentifier(filterEntity.getOrgIdentifier())
                                            .projectIdentifier(filterEntity.getProjectIdentifier())
                                            .filterVisibility(filterEntity.getFilterVisibility());
    if (filterEntity.getFilterProperties() == null) {
      return filterDTOBuilder.build();
    }
    filterDTOBuilder.filterProperties(getFilterProperties(filterEntity.getFilterProperties()));
    return filterDTOBuilder.build();
  }

  private FilterPropertiesDTO getFilterProperties(FilterProperties filterProperties) {
    FilterPropertiesMapper filterPropertiesMapper =
        filterPropertiesMapperMap.get(filterProperties.getType().toString());
    return filterPropertiesMapper.writeDTO(filterProperties);
  }
}

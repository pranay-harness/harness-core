package io.harness.filter.mapper;

import io.harness.beans.EmbeddedUser;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.dto.FilterDTO.FilterDTOBuilder;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.dto.FilterVisibility;
import io.harness.filter.entity.Filter;
import io.harness.filter.entity.Filter.FilterBuilder;
import io.harness.filter.entity.FilterProperties;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Singleton;

@Singleton
public class FilterMapper {
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
    if (SecurityContextBuilder.getPrincipal() != null
        && SecurityContextBuilder.getPrincipal().getType() == PrincipalType.USER) {
      UserPrincipal userPrincipal = (UserPrincipal) SecurityContextBuilder.getPrincipal();
      EmbeddedUser user = EmbeddedUser.builder().uuid(userPrincipal.getName()).build();
      filterBuilder.createdBy(user);
    }
  }

  private FilterProperties getFilterPropertiesDTO(FilterPropertiesDTO filterProperties) {
    return filterProperties.toEntity();
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
    return filterProperties.writeDTO();
  }
}

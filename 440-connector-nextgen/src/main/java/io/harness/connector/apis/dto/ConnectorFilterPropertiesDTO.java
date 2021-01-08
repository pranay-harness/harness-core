package io.harness.connector.apis.dto;

import static io.harness.filter.FilterConstants.CONNECTOR_FILTER;

import io.harness.connector.entities.ConnectorFilterProperties;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.mapper.TagMapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("ConnectorFilterProperties")
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(CONNECTOR_FILTER)
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorFilterPropertiesDTO extends FilterPropertiesDTO {
  List<String> connectorNames;
  List<String> connectorIdentifiers;
  String description;
  List<ConnectorType> types;
  List<ConnectorCategory> categories;
  List<ConnectivityStatus> connectivityStatuses;
  Boolean inheritingCredentialsFromDelegate;

  @Override
  public FilterProperties toEntity() {
    ModelMapper modelMapper = new ModelMapper();
    ConnectorFilterProperties filterProperties = modelMapper.map(this, ConnectorFilterProperties.class);
    filterProperties.setType(getFilterType());
    filterProperties.setTags(TagMapper.convertToList(getTags()));
    return filterProperties;
  }

  @Override
  public FilterType getFilterType() {
    return FilterType.CONNECTOR;
  }
}
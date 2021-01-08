package io.harness.connector.entities;

import io.harness.connector.apis.dto.ConnectorFilterPropertiesDTO;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.mapper.TagMapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("io.harness.connector.entities.ConnectorFilterProperties")
public class ConnectorFilterProperties extends FilterProperties {
  List<String> connectorNames;
  List<String> connectorIdentifiers;
  String description;
  List<ConnectorType> types;
  List<ConnectorCategory> categories;
  List<ConnectivityStatus> connectivityStatuses;
  Boolean inheritingCredentialsFromDelegate;

  @Override
  public FilterPropertiesDTO writeDTO() {
    ModelMapper modelMapper = new ModelMapper();
    FilterPropertiesDTO filterPropertiesDTO = modelMapper.map(this, ConnectorFilterPropertiesDTO.class);
    filterPropertiesDTO.setTags(TagMapper.convertToMap(getTags()));
    return filterPropertiesDTO;
  }
}
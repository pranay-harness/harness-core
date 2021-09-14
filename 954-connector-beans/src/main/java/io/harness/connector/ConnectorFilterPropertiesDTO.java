/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.filter.FilterConstants.CONNECTOR_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(CONNECTOR_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ConnectorFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(DX)
public class ConnectorFilterPropertiesDTO extends FilterPropertiesDTO {
  List<String> connectorNames;
  List<String> connectorIdentifiers;
  String description;
  List<ConnectorType> types;
  List<ConnectorCategory> categories;
  List<ConnectivityStatus> connectivityStatuses;
  Boolean inheritingCredentialsFromDelegate;
  CcmConnectorFilter ccmConnectorFilter;

  @Override
  public FilterType getFilterType() {
    return FilterType.CONNECTOR;
  }
}

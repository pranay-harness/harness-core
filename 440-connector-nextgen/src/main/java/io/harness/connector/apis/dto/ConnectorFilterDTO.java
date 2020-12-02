package io.harness.connector.apis.dto;

import io.harness.connector.entities.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("ConnectorListFilter")
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorFilterDTO {
  @NotNull String name;
  @NotNull String identifier;
  String orgIdentifier;
  String projectIdentifier;
  String searchTerm;
  List<String> connectorNames;
  List<String> connectorIdentifiers;
  List<String> descriptions;
  List<ConnectorType> types;
  List<Scope> scopes;
  List<ConnectorCategory> categories;
  List<ConnectivityStatus> connectivityStatuses;
  Boolean inheritingCredentialsFromDelegate;
}
package io.harness.connector.apis.dto;

import io.harness.connector.entities.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
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
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorListFilter {
  private static final String CATEGORY_KEY = "category";
  private static final String ORG_IDENTIFIER_OF_FILTER = "filterOrgIdentifier";
  private static final String PROJECT_IDENTIFIER_OF_FILTER = "filterProjectIdentifier";
  List<String> orgIdentifier;
  List<String> projectIdentifier;
  String searchTerm;
  List<String> name;
  List<String> connectorIdentifier;
  List<String> description;
  List<ConnectorType> type;
  List<Scope> scope;
  List<ConnectorCategory> category;
  List<ConnectivityStatus> connectivityStatus;
  String filterIdentifier;
  String filterOrgIdentifier;
  String filterProjectIdentifier;
  Boolean inheritingCredentialsFromDelegate;
}

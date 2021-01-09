package io.harness.connector.helper;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.apis.dto.ConnectorCatalogueItem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CatalogueHelper {
  public List<ConnectorCatalogueItem> getConnectorTypeToCategoryMapping() {
    return Arrays.stream(ConnectorCategory.values())
        .map(item -> ConnectorCatalogueItem.builder().category(item).connectors(item.getConnectors()).build())
        .collect(Collectors.toList());
  }
}

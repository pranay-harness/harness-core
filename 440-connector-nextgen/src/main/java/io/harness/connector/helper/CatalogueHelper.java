package io.harness.connector.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCatalogueItem;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorRegistryFactory;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(DX)
public class CatalogueHelper {
  public List<ConnectorCatalogueItem> getConnectorTypeToCategoryMapping() {
    final Map<ConnectorCategory, List<ConnectorType>> connectorCategoryListMap =
        Arrays.stream(ConnectorType.values())
            .collect(Collectors.groupingBy(ConnectorRegistryFactory::getConnectorCategory));
    return connectorCategoryListMap.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry
            -> ConnectorCatalogueItem.builder()
                   .category(entry.getKey())
                   .connectors(new HashSet<>(entry.getValue()))
                   .build())
        .collect(Collectors.toList());
  }
}

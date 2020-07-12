package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEData implements QLData {
  List<QLCEDataEntry> data;
}

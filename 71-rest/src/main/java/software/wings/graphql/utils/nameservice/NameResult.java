package software.wings.graphql.utils.nameservice;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
@Builder
public class NameResult {
  Map<String, String> nameMap;
  String type;
  Set<String> notFoundNames;
}

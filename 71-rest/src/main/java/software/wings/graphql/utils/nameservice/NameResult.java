package software.wings.graphql.utils.nameservice;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class NameResult {
  public static final String DELETED = "DELETED";
  Map<String, String> idNameMap;
  String type;
}

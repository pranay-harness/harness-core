package software.wings.graphql.utils.nameservice;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NameResult {
  public static final String DELETED = "DELETED";
  Map<String, String> idNameMap;
  String type;
}

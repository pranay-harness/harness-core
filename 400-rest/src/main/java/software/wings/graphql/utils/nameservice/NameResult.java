package software.wings.graphql.utils.nameservice;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class NameResult {
  public static final String DELETED = "DELETED";
  Map<String, String> idNameMap;
  String type;
}

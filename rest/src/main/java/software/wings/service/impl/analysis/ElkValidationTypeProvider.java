package software.wings.service.impl.analysis;

import static java.util.stream.Collectors.toMap;

import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.stream.Stream;

public class ElkValidationTypeProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Stream.of(ElkValidationType.values()).collect(toMap(ElkValidationType::name, ElkValidationType::getName));
  }
}

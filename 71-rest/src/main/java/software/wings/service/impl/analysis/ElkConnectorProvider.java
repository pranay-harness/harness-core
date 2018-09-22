package software.wings.service.impl.analysis;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Singleton;

import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by sriram_parthasarathy on 10/5/17.
 */
@Singleton
public class ElkConnectorProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Stream.of(ElkConnector.values()).collect(toMap(ElkConnector::name, ElkConnector::getName));
  }
}

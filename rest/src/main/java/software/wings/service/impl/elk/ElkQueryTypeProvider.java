package software.wings.service.impl.elk;

import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 10/26/17.
 */
public class ElkQueryTypeProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    final Map<String, String> rv = new HashMap<>();
    rv.put(ElkQueryType.TERM.name(), ElkQueryType.TERM.name());
    rv.put(ElkQueryType.MATCH.name(), ElkQueryType.MATCH.name());
    return rv;
  }
}

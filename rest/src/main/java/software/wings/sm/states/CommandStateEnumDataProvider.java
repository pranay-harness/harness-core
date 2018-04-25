package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.DataProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by peeyushaggarwal on 6/30/16.
 */
@Singleton
public class CommandStateEnumDataProvider implements DataProvider {
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    if (appId != null) {
      List<Service> services;
      if (isNotEmpty(params)) {
        services = serviceResourceService.list(aPageRequest().addFilter("appId", EQ, appId).build(), false, true)
                       .getResponse();
      } else {
        if (params.get("NONE") != null) {
          return Maps.newHashMap();
        }
        services =
            Collections.singletonList(serviceResourceService.get(appId, params.get(EntityType.SERVICE.name()), true));
      }
      return services.stream()
          .flatMap(service -> service.getServiceCommands().stream())
          .map(command -> command.getName())
          .distinct()
          .collect(toMap(Function.identity(), Function.identity()));
    }
    return Maps.newHashMap();
  }
}

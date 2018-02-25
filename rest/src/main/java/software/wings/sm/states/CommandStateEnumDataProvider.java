package software.wings.sm.states;

import static java.util.stream.Collectors.toMap;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
  public Map<String, String> getData(String appId, String... params) {
    if (appId != null) {
      List<Service> services;
      if (params.length == 0) {
        services = serviceResourceService.list(aPageRequest().addFilter("appId", EQ, appId).build(), false, true)
                       .getResponse();
      } else {
        if (params[0].equals("NONE")) {
          return Maps.newHashMap();
        }
        services = Collections.singletonList(serviceResourceService.get(appId, params[0], true));
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

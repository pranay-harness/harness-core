package software.wings.sm.states;

import static java.util.stream.Collectors.toMap;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.DataProvider;

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
    List<Service> services =
        serviceResourceService
            .list(aPageRequest().addFilter(aSearchFilter().withField("appId", EQ, appId).build()).build(), false, true)
            .getResponse();

    return services.stream()
        .flatMap(service -> service.getServiceCommands().stream())
        .map(command -> command.getName())
        .distinct()
        .collect(toMap(Function.identity(), Function.identity()));
  }
}

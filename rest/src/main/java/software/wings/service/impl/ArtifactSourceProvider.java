package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by sgurubelli on 11/15/17.
 */
@Singleton
public class ArtifactSourceProvider implements DataProvider {
  @Inject ArtifactStreamService artifactStreamService;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    List<ArtifactStream> artifactStreams = artifactStreamService.list(
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, appId).build());
    if (isEmpty(artifactStreams)) {
      return new HashMap<>();
    }
    List<Service> services = serviceResourceService.list(
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, appId).build(), false, false);
    Map<String, String> serviceIdToName = services.stream().collect(toMap(Service::getUuid, Service::getName));
    return artifactStreams.stream().collect(
        toMap(o -> o.getUuid(), o -> o.getSourceName() + " (" + serviceIdToName.get(o.getServiceId()) + ")"));
  }
}

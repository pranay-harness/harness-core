package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class ArtifactStreamServiceBindingServiceImpl implements ArtifactStreamServiceBindingService {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public ArtifactStream create(String appId, String serviceId, String artifactStreamId) {
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact stream does not exist", USER);
    }

    List<String> artifactStreamIds = service.getArtifactStreamIds();
    if (artifactStreamIds == null) {
      artifactStreamIds = new ArrayList<>();
      artifactStreamIds.add(artifactStreamId);
    } else if (!artifactStreamIds.contains(artifactStreamId)) {
      artifactStreamIds.add(artifactStreamId);
    } else {
      return artifactStream;
    }

    serviceResourceService.updateArtifactStreamIds(service, artifactStreamIds);
    return artifactStream;
  }

  @Override
  public boolean delete(String appId, String serviceId, String artifactStreamId) {
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    return delete(service, artifactStreamId);
  }

  @Override
  public List<String> listArtifactStreamIds(String appId, String serviceId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return listArtifactStreamIds(serviceId);
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null || service.getArtifactStreamIds() == null) {
      return new ArrayList<>();
    }

    return service.getArtifactStreamIds();
  }

  @Override
  public List<String> listArtifactStreamIds(String serviceId) {
    Service service = serviceResourceService.get(serviceId);
    if (service == null || service.getArtifactStreamIds() == null) {
      return new ArrayList<>();
    }

    return service.getArtifactStreamIds();
  }

  @Override
  public List<ArtifactStream> listArtifactStreams(String appId, String serviceId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return listArtifactStreams(serviceId);
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      return new ArrayList<>();
    }

    return artifactStreamService.listByIds(service.getArtifactStreamIds());
  }

  @Override
  public List<ArtifactStream> listArtifactStreams(String serviceId) {
    Service service = serviceResourceService.get(serviceId);
    if (service == null) {
      return new ArrayList<>();
    }

    return artifactStreamService.listByIds(service.getArtifactStreamIds());
  }

  // TODO: ASR: most invocations of the methods below will use setting instead of service after refactoring.

  @Override
  public List<String> listServiceIds(String appId, String artifactStreamId) {
    return listServices(appId, artifactStreamId).stream().map(Service::getUuid).collect(Collectors.toList());
  }

  @Override
  public List<String> listServiceIds(String artifactStreamId) {
    return listServices(artifactStreamId).stream().map(Service::getUuid).collect(Collectors.toList());
  }

  @Override
  public List<Service> listServices(String appId, String artifactStreamId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return listServices(artifactStreamId);
    }
    return serviceResourceService.listByArtifactStreamId(appId, artifactStreamId);
  }

  @Override
  public List<Service> listServices(String artifactStreamId) {
    return serviceResourceService.listByArtifactStreamId(artifactStreamId);
  }

  // TODO: ASR: make sure throwException is false after refactor to connector level artifact

  @Override
  public Service getService(String appId, String artifactStreamId, boolean throwException) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return getService(artifactStreamId, throwException);
    }

    return getService(listServices(appId, artifactStreamId), artifactStreamId, throwException);
  }

  @Override
  public Service getService(String artifactStreamId, boolean throwException) {
    return getService(listServices(artifactStreamId), artifactStreamId, throwException);
  }

  private Service getService(List<Service> services, String artifactStreamId, boolean throwException) {
    if (isEmpty(services)) {
      if (throwException) {
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER)
            .addParam("message", format("Artifact stream %s is a zombie.", artifactStreamId));
      }
      return null;
    }

    return services.get(0);
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    List<Service> services = listServices(artifactStreamId);
    if (isEmpty(services)) {
      return;
    }

    services.forEach(service -> delete(service, artifactStreamId));
  }

  private boolean delete(Service service, String artifactStreamId) {
    List<String> artifactStreamIds = service.getArtifactStreamIds();
    if (artifactStreamIds == null || !artifactStreamIds.remove(artifactStreamId)) {
      return false;
    }

    serviceResourceService.updateArtifactStreamIds(service, artifactStreamIds);
    return true;
  }
}

package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableKeys;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamBinding;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;

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
  @Inject private ServiceVariableService serviceVariableService;

  @Override
  public ArtifactStreamBinding create(
      @NotEmpty String appId, @NotEmpty String serviceId, ArtifactStreamBinding artifactStreamBinding) {
    // TODO: ASR: add validations for artifact streams - types, permissions, etc.
    Service service = serviceResourceService.get(appId, serviceId);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    ArtifactStreamBinding existingArtifactStreamBinding =
        getInternal(appId, serviceId, artifactStreamBinding.getName());
    if (existingArtifactStreamBinding != null) {
      throw new InvalidRequestException("Artifact stream binding already exists", USER);
    }

    List<String> allowedList = new ArrayList<>();
    if (artifactStreamBinding.getArtifactStreams() != null) {
      for (ArtifactStreamSummary streamSummary : artifactStreamBinding.getArtifactStreams()) {
        if (streamSummary != null && streamSummary.getArtifactStreamId() != null
            && !allowedList.contains(streamSummary.getArtifactStreamId())) {
          allowedList.add(streamSummary.getArtifactStreamId());
        }
      }
    }

    ServiceVariable variable = ServiceVariable.builder()
                                   .name(artifactStreamBinding.getName())
                                   .type(Type.ARTIFACT)
                                   .entityType(EntityType.SERVICE)
                                   .entityId(serviceId)
                                   .allowedList(allowedList)
                                   .build();
    serviceVariableService.saveWithChecks(appId, variable);
    return artifactStreamBinding;
  }

  @Override
  public ArtifactStreamBinding update(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name,
      ArtifactStreamBinding artifactStreamBinding) {
    // TODO: ASR: add validations for artifact streams - types, permissions, etc.
    Service service = serviceResourceService.get(appId, serviceId);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    // check if artifact variable being updated exists
    List<ServiceVariable> variables = getServiceVariablesByName(appId, serviceId, name);
    if (isEmpty(variables)) {
      throw new InvalidRequestException("Artifact stream binding does not exist", USER);
    }

    // check if new artifact variable name provided is unique within the service
    if (!name.equals(artifactStreamBinding.getName())) {
      List<ServiceVariable> collidingVariables =
          getServiceVariablesByName(appId, serviceId, artifactStreamBinding.getName());
      if (isNotEmpty(collidingVariables)) {
        throw new InvalidRequestException(
            format("Artifact variable with name [%s] already exists in service", artifactStreamBinding.getName()),
            USER);
      }
    }

    List<String> allowedList = new ArrayList<>();
    if (artifactStreamBinding.getArtifactStreams() != null) {
      for (ArtifactStreamSummary streamSummary : artifactStreamBinding.getArtifactStreams()) {
        if (streamSummary != null && streamSummary.getArtifactStreamId() != null
            && !allowedList.contains(streamSummary.getArtifactStreamId())) {
          allowedList.add(streamSummary.getArtifactStreamId());
        }
      }
    }

    ServiceVariable variable = variables.get(0);
    variable.setName(artifactStreamBinding.getName());
    variable.setAllowedList(allowedList);
    serviceVariableService.updateWithChecks(appId, variable.getUuid(), variable);
    return artifactStreamBinding;
  }

  @Override
  public void delete(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name) {
    Service service = serviceResourceService.get(appId, serviceId);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    List<ServiceVariable> variables = getServiceVariablesByName(appId, serviceId, name);
    if (isEmpty(variables)) {
      throw new InvalidRequestException("Artifact stream binding does not exist", USER);
    }

    serviceVariableService.deleteWithChecks(appId, variables.get(0).getUuid());
  }

  @Override
  public List<ArtifactStreamBinding> list(@NotEmpty String appId, @NotEmpty String serviceId) {
    List<ServiceVariable> variables = getServiceVariables(appId, serviceId);
    return variables.stream()
        .map(variable -> {
          List<ArtifactStreamSummary> artifactStreams = new ArrayList<>();
          if (variable.getAllowedList() != null) {
            for (String artifactStreamId : variable.getAllowedList()) {
              ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
              if (artifactStream == null) {
                continue;
              }

              artifactStreams.add(ArtifactStreamSummary.fromArtifactStream(artifactStream));
            }
          }

          return ArtifactStreamBinding.builder().name(variable.getName()).artifactStreams(artifactStreams).build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public ArtifactStreamBinding get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name) {
    ArtifactStreamBinding variable = getInternal(appId, serviceId, name);
    if (variable == null) {
      throw new InvalidRequestException("Artifact stream binding does not exist", USER);
    }

    return variable;
  }

  private ArtifactStreamBinding getInternal(String appId, String serviceId, String name) {
    List<ServiceVariable> variables = getServiceVariablesByName(appId, serviceId, name);
    if (isEmpty(variables)) {
      return null;
    }

    ServiceVariable variable = variables.get(0);
    List<ArtifactStreamSummary> artifactStreams = new ArrayList<>();
    if (variable.getAllowedList() != null) {
      for (String artifactStreamId : variable.getAllowedList()) {
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
        if (artifactStream == null) {
          continue;
        }

        artifactStreams.add(ArtifactStreamSummary.fromArtifactStream(artifactStream));
      }
    }

    return ArtifactStreamBinding.builder().name(variable.getName()).artifactStreams(artifactStreams).build();
  }

  @Override
  public List<ServiceVariable> getServiceVariables(String appId, String serviceId) {
    return serviceVariableService.list(aPageRequest()
                                           .addFilter(ServiceVariableKeys.appId, Operator.EQ, appId)
                                           .addFilter(ServiceVariableKeys.entityType, Operator.EQ, EntityType.SERVICE)
                                           .addFilter(ServiceVariableKeys.entityId, Operator.EQ, serviceId)
                                           .addFilter(ServiceVariableKeys.type, Operator.EQ, Type.ARTIFACT)
                                           .build());
  }

  @Override
  public List<ServiceVariable> getServiceVariablesByName(String appId, String serviceId, String name) {
    return serviceVariableService.list(aPageRequest()
                                           .addFilter(ServiceVariableKeys.appId, Operator.EQ, appId)
                                           .addFilter(ServiceVariableKeys.entityType, Operator.EQ, EntityType.SERVICE)
                                           .addFilter(ServiceVariableKeys.entityId, Operator.EQ, serviceId)
                                           .addFilter(ServiceVariableKeys.type, Operator.EQ, Type.ARTIFACT)
                                           .addFilter(ServiceVariableKeys.name, Operator.EQ, name)
                                           .build());
  }

  @Override
  public ArtifactStream createOld(String appId, String serviceId, String artifactStreamId) {
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
  public boolean deleteOld(String appId, String serviceId, String artifactStreamId) {
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }

    return deleteOld(service, artifactStreamId);
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

    services.forEach(service -> deleteOld(service, artifactStreamId));
  }

  private boolean deleteOld(Service service, String artifactStreamId) {
    List<String> artifactStreamIds = service.getArtifactStreamIds();
    if (artifactStreamIds == null || !artifactStreamIds.remove(artifactStreamId)) {
      return false;
    }

    serviceResourceService.updateArtifactStreamIds(service, artifactStreamIds);
    return true;
  }
}

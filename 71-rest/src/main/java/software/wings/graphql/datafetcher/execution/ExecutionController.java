package software.wings.graphql.datafetcher.execution;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import graphql.GraphQLContext;
import io.harness.beans.CreatedByType;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.govern.Switch;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.execution.input.QLArtifactIdInput;
import software.wings.graphql.schema.mutation.execution.input.QLArtifactInputType;
import software.wings.graphql.schema.mutation.execution.input.QLArtifactValueInput;
import software.wings.graphql.schema.mutation.execution.input.QLBuildNumberInput;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.type.QLExecutionStatus;
import software.wings.resources.graphql.TriggeredByType;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
@Slf4j
public class ExecutionController {
  @Inject ArtifactService artifactService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;
  @Inject ServiceResourceService serviceResourceService;

  public static QLExecutionStatus convertStatus(ExecutionStatus status) {
    switch (status) {
      case ABORTED:
      case ABORTING:
        return QLExecutionStatus.ABORTED;
      case DISCONTINUING:
      case RUNNING:
        return QLExecutionStatus.RUNNING;
      case ERROR:
        return QLExecutionStatus.ERROR;
      case FAILED:
        return QLExecutionStatus.FAILED;
      case PAUSED:
      case PAUSING:
        return QLExecutionStatus.PAUSED;
      case QUEUED:
      case NEW:
      case SCHEDULED:
      case STARTING:
        return QLExecutionStatus.QUEUED;
      case RESUMED:
        return QLExecutionStatus.RESUMED;
      case SUCCESS:
        return QLExecutionStatus.SUCCESS;
      case WAITING:
        return QLExecutionStatus.WAITING;
      case SKIPPED:
        return QLExecutionStatus.SKIPPED;
      case REJECTED:
        return QLExecutionStatus.REJECTED;
      case EXPIRED:
        return QLExecutionStatus.EXPIRED;
      default:
        Switch.unhandled(status);
    }
    return null;
  }

  public static List<ExecutionStatus> convertStatus(List<QLExecutionStatus> statuses) {
    return statuses.stream().map(ExecutionController::convertStatus).flatMap(Collection::stream).collect(toList());
  }

  public static List<ExecutionStatus> convertStatus(QLExecutionStatus status) {
    switch (status) {
      case ABORTED:
        return asList(ExecutionStatus.ABORTED, ExecutionStatus.ABORTING);
      case RUNNING:
        return asList(ExecutionStatus.RUNNING, ExecutionStatus.DISCONTINUING);
      case ERROR:
        return asList(ExecutionStatus.ERROR);
      case FAILED:
        return asList(ExecutionStatus.FAILED);
      case PAUSED:
        return asList(ExecutionStatus.PAUSED, ExecutionStatus.PAUSING);
      case QUEUED:
        return asList(ExecutionStatus.QUEUED, ExecutionStatus.STARTING, ExecutionStatus.SCHEDULED, ExecutionStatus.NEW);
      case RESUMED:
        return asList(ExecutionStatus.RESUMED);
      case SUCCESS:
        return asList(ExecutionStatus.SUCCESS);
      case WAITING:
        return asList(ExecutionStatus.WAITING);
      case SKIPPED:
        return asList(ExecutionStatus.SKIPPED);
      case REJECTED:
        return asList(ExecutionStatus.REJECTED);
      case EXPIRED:
        return asList(ExecutionStatus.EXPIRED);
      default:
        Switch.unhandled(status);
    }
    return null;
  }

  public void populateExecutionArgs(Map<String, String> variableValues, List<Artifact> artifacts,
      QLStartExecutionInput triggerExecutionInput, MutationContext mutationContext, ExecutionArgs executionArgs) {
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setExcludeHostsWithSameArtifact(triggerExecutionInput.isExcludeHostsWithSameArtifact());
    executionArgs.setNotes(triggerExecutionInput.getNotes());
    executionArgs.setTargetToSpecificHosts(triggerExecutionInput.isTargetToSpecificHosts());
    executionArgs.setHosts(triggerExecutionInput.getSpecificHosts());
    executionArgs.setWorkflowVariables(variableValues);
    if (mutationContext.getDataFetchingEnvironment() != null
        && mutationContext.getDataFetchingEnvironment().getContext() != null) {
      GraphQLContext graphQLContext = (GraphQLContext) mutationContext.getDataFetchingEnvironment().getContext();
      TriggeredByType triggeredByType = graphQLContext.get("triggeredByType");
      if (triggeredByType == TriggeredByType.USER) {
        executionArgs.setCreatedByType(CreatedByType.USER);
      } else {
        executionArgs.setCreatedByType(CreatedByType.API_KEY);
        executionArgs.setTriggeringApiKeyId(graphQLContext.get("triggeredById"));
      }
    }
  }

  private Artifact getArtifactFromId(QLArtifactIdInput artifactId, Service service) {
    String artifactIdVal = artifactId.getArtifactId();
    Artifact artifact = artifactService.get(service.getAccountId(), artifactIdVal);
    notNullCheck("Cannot find artifact for specified Id: " + artifactIdVal + ". Might be deleted", artifact, USER);
    if (!artifact.getServiceIds().contains(service.getUuid())) {
      throw new InvalidRequestException(
          "Artifact Id: " + artifactIdVal + " does not belong to specified service: " + service.getName(), USER);
    }
    return artifact;
  }

  private Artifact getArtifactFromBuildNumber(QLBuildNumberInput buildNumber, Service service) {
    String artifactSourceName = buildNumber.getArtifactSourceName();
    ArtifactStream artifactStream =
        artifactStreamService.getArtifactStreamByName(service.getAppId(), service.getUuid(), artifactSourceName);
    notNullCheck("Cannot find artifact Source: " + artifactSourceName + " in specified service: " + service.getName(),
        artifactStream, USER);
    Artifact artifact = getArtifactForBuildNumber(service.getAppId(), artifactStream, buildNumber.getBuildNumber());
    notNullCheck(
        "Cannot find or collect artifact for specified Build Number: " + buildNumber.getBuildNumber(), artifact, USER);
    return artifact;
  }

  private Artifact getArtifactForBuildNumber(String appId, ArtifactStream artifactStream, String buildNumber) {
    Artifact collectedArtifactForBuildNumber =
        artifactService.getArtifactByBuildNumber(artifactStream, buildNumber, false);

    return collectedArtifactForBuildNumber != null
        ? collectedArtifactForBuildNumber
        : collectNewArtifactForBuildNumber(appId, artifactStream, buildNumber);
  }

  private Artifact collectNewArtifactForBuildNumber(String appId, ArtifactStream artifactStream, String buildNumber) {
    Artifact artifact = artifactCollectionServiceAsync.collectNewArtifacts(appId, artifactStream, buildNumber);
    if (artifact != null) {
      logger.info("Artifact {} collected for the build number {} of stream id {}", artifact, buildNumber,
          artifactStream.getUuid());
    } else {
      logger.warn(
          "Artifact collection invoked. However, Artifact not yet collected for the build number {} of stream id {}",
          buildNumber, artifactStream.getUuid());
    }
    return artifact;
  }

  public void getArtifactsFromServiceInputs(List<QLServiceInput> serviceInputs, String appId,
      List<String> artifactNeededServiceIds, List<Artifact> artifacts) {
    for (String serviceId : artifactNeededServiceIds) {
      Service service = serviceResourceService.get(appId, serviceId);
      notNullCheck(
          "Something went wrong while checking required service Inputs. Associated Service might be deleted", service);
      String serviceName = service.getName();
      QLServiceInput serviceInput =
          serviceInputs.stream().filter(t -> serviceName.equals(t.getName())).findFirst().orElse(null);
      if (serviceInput == null) {
        throw new InvalidRequestException("ServiceInput required for service: " + serviceName, USER);
      }
      QLArtifactValueInput artifactValueInput = serviceInput.getArtifactValueInput();
      QLArtifactInputType type = artifactValueInput.getValueType();
      switch (type) {
        case ARTIFACT_ID:
          artifacts.add(getArtifactFromId(artifactValueInput.getArtifactId(), service));
          continue;
        case BUILD_NUMBER:
          artifacts.add(getArtifactFromBuildNumber(artifactValueInput.getBuildNumber(), service));
          continue;
        default:
          throw new UnsupportedOperationException("Unexpected artifact value type: " + type);
      }
    }
  }
}

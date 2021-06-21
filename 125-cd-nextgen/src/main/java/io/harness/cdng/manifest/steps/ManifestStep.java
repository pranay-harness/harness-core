package io.harness.cdng.manifest.steps;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.mappers.ManifestOutcomeMapper;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.service.steps.ServiceStepsHelper;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedTypeException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ManifestStep implements SyncExecutable<ManifestStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.MANIFEST.getName()).build();

  @Inject private ServiceStepsHelper serviceStepsHelper;
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;

  @Override
  public Class<ManifestStepParameters> getStepParametersClass() {
    return ManifestStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, ManifestStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    NGLogCallback logCallback = serviceStepsHelper.getServiceLogCallback(ambiance);
    logCallback.saveExecutionLog(format("Processing manifest [%s]...", stepParameters.getIdentifier()));
    ManifestAttributes finalManifest = applyManifestsOverlay(stepParameters);
    getConnector(finalManifest, ambiance);
    logCallback.saveExecutionLog(format("Processed manifest [%s]", stepParameters.getIdentifier()));
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .name("output")
                         .outcome(ManifestOutcomeMapper.toManifestOutcome(finalManifest))
                         .build())
        .build();
  }

  private ManifestAttributes applyManifestsOverlay(ManifestStepParameters stepParameters) {
    List<ManifestAttributes> manifestList = new LinkedList<>();
    // 1. Original manifests
    if (stepParameters.getSpec() != null) {
      manifestList.add(stepParameters.getSpec());
    }
    // 2. Override sets
    if (stepParameters.getOverrideSets() != null) {
      manifestList.addAll(stepParameters.getOverrideSets());
    }
    // 3. Stage Overrides
    if (stepParameters.getStageOverride() != null) {
      manifestList.add(stepParameters.getStageOverride());
    }
    if (EmptyPredicate.isEmpty(manifestList)) {
      throw new InvalidArgumentsException("No manifests defined");
    }
    ManifestAttributes resultantManifest = manifestList.get(0);
    for (ManifestAttributes manifest : manifestList.subList(1, manifestList.size())) {
      if (!manifest.getKind().equals(resultantManifest.getKind())) {
        throw new UnexpectedTypeException(
            format("Unable to apply manifest override of type '%s' to manifest of type '%s' with identifier '%s'",
                manifest.getKind(), resultantManifest.getKind(), resultantManifest.getIdentifier()));
      }

      resultantManifest = resultantManifest.applyOverrides(manifest);
    }
    return resultantManifest;
  }

  private void getConnector(ManifestAttributes manifestAttributes, Ambiance ambiance) {
    if (ParameterField.isNull(manifestAttributes.getStoreConfig().getConnectorRef())) {
      throw new InvalidRequestException(
          "Connector ref field not present in manifest with identifier " + manifestAttributes.getIdentifier());
    }
    String connectorIdentifierRef = manifestAttributes.getStoreConfig().getConnectorRef().getValue();
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifierRef,
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier : [%s]", connectorIdentifierRef));
    }
  }
}

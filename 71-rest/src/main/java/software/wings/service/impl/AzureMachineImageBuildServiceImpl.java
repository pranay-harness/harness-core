package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.equalCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.tuple.ImmutablePair;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.AzureMachineImageArtifactStream;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.AzureMachineImageBuildService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
public class AzureMachineImageBuildServiceImpl implements AzureMachineImageBuildService {
  @Inject private AzureHelperService azureHelperService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes streamAttributes, AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(streamAttributes.getArtifactStreamType(), ArtifactStreamType.AZURE_MACHINE_IMAGE.name());

    Map<String, String> metadata = new HashMap<>();
    metadata.put("galleryName", streamAttributes.getAzureImageGalleryName());
    metadata.put("osType", streamAttributes.getOsType());
    metadata.put("resourceGroup", streamAttributes.getAzureResourceGroup());
    metadata.put("subscriptionId", streamAttributes.getSubscriptionId());
    metadata.put("imageDefinitionName", streamAttributes.getAzureImageDefinition());
    metadata.put("imageType", streamAttributes.getImageType());
    return azureHelperService
        .listImageDefinitionVersions(azureConfig, encryptionDetails, streamAttributes.getSubscriptionId(),
            streamAttributes.getAzureResourceGroup(), streamAttributes.getAzureImageGalleryName(),
            streamAttributes.getAzureImageDefinition())
        .stream()
        .map(version
            -> BuildDetails.Builder.aBuildDetails()
                   .withMetadata(metadata)
                   .withNumber(version.getName())
                   .withRevision(version.getName())
                   .withUiDisplayName("Image: " + version.getName())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public ArtifactStreamAttributes validateThenInferAttributes(AzureConfig config,
      List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes) {
    ArtifactStreamAttributes streamAttributes = artifactStreamAttributes.toBuilder().build();
    if (Objects.equals(
            AzureMachineImageArtifactStream.ImageType.IMAGE_GALLERY.name(), artifactStreamAttributes.getImageType())) {
      final String imageName = artifactStreamAttributes.getAzureImageDefinition();
      // Note: Using list instead of get method as get method throws NULL pointer exception if fields are invalid
      final Optional<AzureImageDefinition> definition =
          azureHelperService
              .listImageDefinitions(config, encryptionDetails, artifactStreamAttributes.getSubscriptionId(),
                  artifactStreamAttributes.getAzureResourceGroup(), artifactStreamAttributes.getAzureImageGalleryName())
              .stream()
              .filter(def -> def.getName().equals(imageName))
              .findFirst();
      if (!definition.isPresent()) {
        throw new InvalidArgumentsException(
            ImmutablePair.of("args",
                imageName + " does not exists for the given combination of subscription "
                    + streamAttributes.getSubscriptionId() + " resource group "
                    + streamAttributes.getAzureResourceGroup() + " and image gallery "
                    + streamAttributes.getAzureImageGalleryName()),
            null, USER);
      }
      streamAttributes.setOsType(definition.get().getOsType());
    }
    return streamAttributes;
  }
}

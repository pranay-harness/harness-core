package software.wings.api;

import software.wings.utils.ArtifactType;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by rishi on 12/22/16.
 */
public enum DeploymentType {
  SSH("Secure Shell (SSH)"),
  AWS_CODEDEPLOY("AWS CodeDeploy"),
  ECS("Amazon EC2 Container Services (ECS)"),
  SPOTINST("SPOTINST"),
  KUBERNETES("Kubernetes"),
  HELM("Helm"),
  AWS_LAMBDA("AWS Lambda"),
  AMI("AMI"),
  WINRM("Windows Remote Management (WinRM)"),
  PCF("Pivotal Cloud Foundry"),
  AZURE_VMSS("Azure Virtual Machine Image"),
  AZURE_WEBAPP("Azure Web App"),
  CUSTOM("Custom");

  private String displayName;

  DeploymentType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static final ImmutableMap<DeploymentType, List<ArtifactType>> supportedArtifactTypes =
      ImmutableMap.<DeploymentType, List<ArtifactType>>builder()
          .put(SSH,
              Arrays.asList(ArtifactType.DOCKER, ArtifactType.JAR, ArtifactType.WAR, ArtifactType.RPM,
                  ArtifactType.OTHER, ArtifactType.TAR, ArtifactType.ZIP))
          .put(ECS, Collections.singletonList(ArtifactType.DOCKER))
          .put(KUBERNETES, Collections.singletonList(ArtifactType.DOCKER))
          .put(HELM, Collections.singletonList(ArtifactType.DOCKER))
          .put(AWS_CODEDEPLOY, Collections.singletonList(ArtifactType.AWS_CODEDEPLOY))
          .put(AWS_LAMBDA, Collections.singletonList(ArtifactType.AWS_LAMBDA))
          .put(AMI, Collections.singletonList(ArtifactType.AMI))
          .put(WINRM, Arrays.asList(ArtifactType.IIS, ArtifactType.IIS_APP, ArtifactType.IIS_VirtualDirectory))
          .put(AZURE_VMSS, Collections.singletonList(ArtifactType.AZURE_MACHINE_IMAGE))
          .put(AZURE_WEBAPP, Collections.singletonList(ArtifactType.AZURE_WEBAPP))
          .put(PCF, Collections.singletonList(ArtifactType.PCF))
          .put(CUSTOM,
              Arrays.asList(ArtifactType.DOCKER, ArtifactType.JAR, ArtifactType.WAR, ArtifactType.RPM,
                  ArtifactType.OTHER, ArtifactType.TAR, ArtifactType.ZIP))
          .build();
}

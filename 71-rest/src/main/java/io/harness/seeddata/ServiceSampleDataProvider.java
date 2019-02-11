package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_DESC;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_NAME;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.container.LogConfiguration;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

@Singleton
public class ServiceSampleDataProvider {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ApplicationManifestService applicationManifestService;

  public Service createKubeService(String appId) {
    Service service = serviceResourceService.save(Service.builder()
                                                      .appId(appId)
                                                      .artifactType(ArtifactType.DOCKER)
                                                      .name(K8S_SERVICE_NAME)
                                                      .description(K8S_SERVICE_DESC)
                                                      .build());

    // Create container task
    KubernetesContainerTask containerTask = new KubernetesContainerTask();
    containerTask.setAppId(appId);
    containerTask.setServiceId(service.getUuid());
    containerTask.setContainerDefinitions(
        asList(ContainerDefinition.builder()
                   .name("DEFAULT_NAME")
                   .commands(asList("")) // UI mandates it .. do not remove it
                   .storageConfigurations(asList()) // UI looking for empty list
                   .logConfiguration(LogConfiguration.builder().logDriver("").build()) // UI looking for empty list
                   .build()));

    final ContainerTask savedContainerTask = serviceResourceService.createContainerTask(containerTask, true);

    // Update the YAML Spec for BusyBox  --> TODO: Move it to files
    String advancedConfig = "# Enter your Controller YAML spec below.\n"
        + "#\n"
        + "# Supported Controllers:\n"
        + "#   ReplicationController\n"
        + "#   Deployment\n"
        + "#   ReplicaSet\n"
        + "#   StatefulSet\n"
        + "#   DaemonSet\n"
        + "#\n"
        + "# Placeholders:\n"
        + "#\n"
        + "# Required: ${DOCKER_IMAGE_NAME}\n"
        + "#   - Replaced with the Docker image name and tag\n"
        + "#\n"
        + "# Optional: ${CONTAINER_NAME}\n"
        + "#   - Replaced with a container name based on the image name\n"
        + "#\n"
        + "# Optional: ${CONFIG_MAP_NAME}\n"
        + "#   - Replaced with the ConfigMap name (same as controller name)\n"
        + "#     Config map contains all unencrypted service variables and\n"
        + "#     all unencrypted config files, unless a custom\n"
        + "#     config map is provided\n"
        + "#\n"
        + "# Optional: ${SECRET_MAP_NAME}\n"
        + "#   - Replaced with the Secret name (same as controller name)\n"
        + "#     Secret map contains all encrypted service variables and\n"
        + "#     all encrypted config files\n"
        + "#\n"
        + "# Optional: ${SECRET_NAME}\n"
        + "#   - Replaced with the name of the generated image pull\n"
        + "#     secret when pulling from a private Docker registry\n"
        + "#\n"
        + "# Harness will set the controller name, namespace,\n"
        + "# selector labels, and number of replicas.\n"
        + "#\n"
        + "# Service variables will be merged into environment\n"
        + "# variables for all containers, overriding values if\n"
        + "# the name is the same.\n"
        + "#\n"
        + "# ---\n"
        + "apiVersion: \"extensions/v1beta1\"\n"
        + "kind: \"Deployment\"\n"
        + "metadata:\n"
        + "  annotations: {}\n"
        + "  finalizers: []\n"
        + "  labels: {}\n"
        + "  ownerReferences: []\n"
        + "spec:\n"
        + "  replicas: 0\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      annotations: {}\n"
        + "      finalizers: []\n"
        + "      labels: {}\n"
        + "      ownerReferences: []\n"
        + "    spec:\n"
        + "      containers:\n"
        + "      - args: []\n"
        + "        command: []\n"
        + "        env: []\n"
        + "        envFrom: []\n"
        + "        image: \"${DOCKER_IMAGE_NAME}\"\n"
        + "        name: \"${CONTAINER_NAME}\"\n"
        + "        ports: []\n"
        + "        resources:\n"
        + "          limits:\n"
        + "            memory: \"750Mi\"\n"
        + "            cpu: \"1\"\n"
        + "          requests:\n"
        + "            memory: \"100Mi\"\n"
        + "            cpu: \"50m\"\n"
        + "        volumeMounts: []\n"
        + "      hostAliases: []\n"
        + "      initContainers: []\n"
        + "      nodeSelector: {}\n"
        + "      tolerations: []\n"
        + "      volumes: []";

    KubernetesPayload kubernetesPayload = KubernetesPayload.builder().advancedConfig(advancedConfig).build();
    serviceResourceService.updateContainerTaskAdvanced(
        appId, savedContainerTask.getServiceId(), savedContainerTask.getUuid(), kubernetesPayload, false);

    return service;
  }

  public Service createK8sV2Service(String appId) {
    Service service = serviceResourceService.save(Service.builder()
                                                      .appId(appId)
                                                      .artifactType(ArtifactType.DOCKER)
                                                      .deploymentType(DeploymentType.KUBERNETES)
                                                      .isK8sV2(true)
                                                      .name(K8S_SERVICE_NAME)
                                                      .description(K8S_SERVICE_DESC)
                                                      .build());

    ManifestFile namespaceManifest = ManifestFile.builder()
                                         .fileName("templates/namespace.yaml")
                                         .fileContent("apiVersion: v1\n"
                                             + "kind: Namespace\n"
                                             + "metadata:\n"
                                             + "  name: {{.Values.namespace}}")
                                         .build();
    namespaceManifest.setAppId(appId);

    ManifestFile serviceManifest = ManifestFile.builder()
                                       .fileName("templates/service.yaml")
                                       .fileContent("apiVersion: v1\n"
                                           + "kind: Service\n"
                                           + "metadata:\n"
                                           + "  name: {{.Values.name}}-svc\n"
                                           + "spec:\n"
                                           + "  type: LoadBalancer\n"
                                           + "  ports:\n"
                                           + "  - port: 80\n"
                                           + "    protocol: TCP\n"
                                           + "  selector:\n"
                                           + "    app: {{.Values.name}}")
                                       .build();
    serviceManifest.setAppId(appId);

    applicationManifestService.createManifestFileByServiceId(namespaceManifest, service.getUuid());

    applicationManifestService.createManifestFileByServiceId(serviceManifest, service.getUuid());

    ApplicationManifest appManifest = applicationManifestService.getByServiceId(appId, service.getUuid());

    ManifestFile valuesFile =
        applicationManifestService.getManifestFileByFileName(appManifest.getUuid(), "values.yaml");
    valuesFile.setFileContent("name: harness-example\n"
        + "replicas: 1\n"
        + "image: ${artifact.metadata.image}\n"
        + "namespace: ${infra.kubernetes.namespace}");

    applicationManifestService.updateManifestFileByServiceId(valuesFile, service.getUuid());

    return service;
  }
}

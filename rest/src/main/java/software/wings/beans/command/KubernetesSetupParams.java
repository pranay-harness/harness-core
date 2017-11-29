package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;

@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesSetupParams extends ContainerSetupParams {
  private KubernetesServiceType serviceType;
  private Integer port;
  private Integer targetPort;
  private KubernetesPortProtocol protocol;
  private String clusterIP;
  private String externalIPs;
  private String loadBalancerIP;
  private Integer nodePort;
  private String externalName;
  private String namespace;
  private String rcNamePrefix;

  public static final class KubernetesSetupParamsBuilder {
    private String serviceName;
    private String appName;
    private String envName;
    private ImageDetails imageDetails;
    private ContainerTask containerTask;
    private String infraMappingId;
    private KubernetesServiceType serviceType;
    private Integer port;
    private Integer targetPort;
    private KubernetesPortProtocol protocol;
    private String clusterIP;
    private String externalIPs;
    private String loadBalancerIP;
    private Integer nodePort;
    private String externalName;
    private String namespace;
    private String rcNamePrefix;

    private KubernetesSetupParamsBuilder() {}

    public static KubernetesSetupParamsBuilder aKubernetesSetupParams() {
      return new KubernetesSetupParamsBuilder();
    }

    public KubernetesSetupParamsBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public KubernetesSetupParamsBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public KubernetesSetupParamsBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public KubernetesSetupParamsBuilder withImageDetails(ImageDetails imageDetails) {
      this.imageDetails = imageDetails;
      return this;
    }

    public KubernetesSetupParamsBuilder withContainerTask(ContainerTask containerTask) {
      this.containerTask = containerTask;
      return this;
    }

    public KubernetesSetupParamsBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public KubernetesSetupParamsBuilder withServiceType(KubernetesServiceType serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public KubernetesSetupParamsBuilder withPort(Integer port) {
      this.port = port;
      return this;
    }

    public KubernetesSetupParamsBuilder withTargetPort(Integer targetPort) {
      this.targetPort = targetPort;
      return this;
    }

    public KubernetesSetupParamsBuilder withProtocol(KubernetesPortProtocol protocol) {
      this.protocol = protocol;
      return this;
    }

    public KubernetesSetupParamsBuilder withClusterIP(String clusterIP) {
      this.clusterIP = clusterIP;
      return this;
    }

    public KubernetesSetupParamsBuilder withExternalIPs(String externalIPs) {
      this.externalIPs = externalIPs;
      return this;
    }

    public KubernetesSetupParamsBuilder withLoadBalancerIP(String loadBalancerIP) {
      this.loadBalancerIP = loadBalancerIP;
      return this;
    }

    public KubernetesSetupParamsBuilder withNodePort(Integer nodePort) {
      this.nodePort = nodePort;
      return this;
    }

    public KubernetesSetupParamsBuilder withExternalName(String externalName) {
      this.externalName = externalName;
      return this;
    }

    public KubernetesSetupParamsBuilder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public KubernetesSetupParamsBuilder withRcNamePrefix(String rcNamePrefix) {
      this.rcNamePrefix = rcNamePrefix;
      return this;
    }

    public KubernetesSetupParamsBuilder but() {
      return aKubernetesSetupParams()
          .withServiceName(serviceName)
          .withAppName(appName)
          .withEnvName(envName)
          .withImageDetails(imageDetails)
          .withContainerTask(containerTask)
          .withInfraMappingId(infraMappingId)
          .withServiceType(serviceType)
          .withPort(port)
          .withTargetPort(targetPort)
          .withProtocol(protocol)
          .withClusterIP(clusterIP)
          .withExternalIPs(externalIPs)
          .withLoadBalancerIP(loadBalancerIP)
          .withNodePort(nodePort)
          .withExternalName(externalName)
          .withNamespace(namespace)
          .withRcNamePrefix(rcNamePrefix);
    }

    public KubernetesSetupParams build() {
      KubernetesSetupParams kubernetesSetupParams = new KubernetesSetupParams();
      kubernetesSetupParams.setServiceName(serviceName);
      kubernetesSetupParams.setAppName(appName);
      kubernetesSetupParams.setEnvName(envName);
      kubernetesSetupParams.setImageDetails(imageDetails);
      kubernetesSetupParams.setContainerTask(containerTask);
      kubernetesSetupParams.setInfraMappingId(infraMappingId);
      kubernetesSetupParams.setServiceType(serviceType);
      kubernetesSetupParams.setPort(port);
      kubernetesSetupParams.setTargetPort(targetPort);
      kubernetesSetupParams.setProtocol(protocol);
      kubernetesSetupParams.setClusterIP(clusterIP);
      kubernetesSetupParams.setExternalIPs(externalIPs);
      kubernetesSetupParams.setLoadBalancerIP(loadBalancerIP);
      kubernetesSetupParams.setNodePort(nodePort);
      kubernetesSetupParams.setExternalName(externalName);
      kubernetesSetupParams.setNamespace(namespace);
      kubernetesSetupParams.setRcNamePrefix(rcNamePrefix);
      return kubernetesSetupParams;
    }
  }
}

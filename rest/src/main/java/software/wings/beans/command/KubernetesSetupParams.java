package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesSetupParams extends ContainerSetupParams {
  private KubernetesServiceType serviceType;
  private Integer port;
  private Integer targetPort;
  private String portName;
  private KubernetesPortProtocol protocol;
  private String clusterIP;
  private String externalIPs;
  private String loadBalancerIP;
  private Integer nodePort;
  private String externalName;
  private String namespace;
  private String controllerNamePrefix;
  private String previousDaemonSetYaml;
  private List<String> previousActiveAutoscalers;
  private boolean rollback;
  private int serviceSteadyStateTimeout;
  private boolean useAutoscaler;
  private int minAutoscaleInstances;
  private int maxAutoscaleInstances;
  private int targetCpuUtilizationPercentage;
  private String customMetricYamlConfig;
  private String subscriptionId;
  private String resourceGroup;
  private boolean useIngress;
  private String ingressYaml;
  private boolean useIstioRouteRule;
  private String configMapYaml;
  private List<String[]> plainConfigFiles;
  private List<String[]> encryptedConfigFiles;

  public static final class KubernetesSetupParamsBuilder {
    private String serviceName;
    private String clusterName;
    private String appName;
    private String envName;
    private ImageDetails imageDetails;
    private ContainerTask containerTask;
    private String infraMappingId;
    private KubernetesServiceType serviceType;
    private Integer port;
    private Integer targetPort;
    private String portName;
    private KubernetesPortProtocol protocol;
    private String clusterIP;
    private String externalIPs;
    private String loadBalancerIP;
    private Integer nodePort;
    private String externalName;
    private String namespace;
    private String controllerNamePrefix;
    private String previousDaemonSetYaml;
    private List<String> previousActiveAutoscalers;
    private boolean rollback;
    private int serviceSteadyStateTimeout;
    private boolean useAutoscaler;
    private int minAutoscaleInstances;
    private int maxAutoscaleInstances;
    private int targetCpuUtilizationPercentage;
    private String customMetricYamlConfig;
    private String subscriptionId;
    private String resourceGroup;
    private boolean useIngress;
    private String ingressYaml;
    private boolean useIstioRouteRule;
    private String configMapYaml;
    private List<String[]> plainConfigFiles;
    private List<String[]> encryptedConfigFiles;

    private KubernetesSetupParamsBuilder() {}

    public static KubernetesSetupParamsBuilder aKubernetesSetupParams() {
      return new KubernetesSetupParamsBuilder();
    }

    public KubernetesSetupParamsBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public KubernetesSetupParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
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

    public KubernetesSetupParamsBuilder withPortName(String portName) {
      this.portName = portName;
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

    public KubernetesSetupParamsBuilder withControllerNamePrefix(String controllerNamePrefix) {
      this.controllerNamePrefix = controllerNamePrefix;
      return this;
    }

    public KubernetesSetupParamsBuilder withPreviousDaemonSetYaml(String previousDaemonSetYaml) {
      this.previousDaemonSetYaml = previousDaemonSetYaml;
      return this;
    }

    public KubernetesSetupParamsBuilder withPreviousActiveAutoscalers(List<String> previousActiveAutoscalers) {
      this.previousActiveAutoscalers = previousActiveAutoscalers;
      return this;
    }

    public KubernetesSetupParamsBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesSetupParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public KubernetesSetupParamsBuilder withUseAutoscaler(boolean useAutoscaler) {
      this.useAutoscaler = useAutoscaler;
      return this;
    }

    public KubernetesSetupParamsBuilder withMinAutoscaleInstances(int minAutoscaleInstances) {
      this.minAutoscaleInstances = minAutoscaleInstances;
      return this;
    }

    public KubernetesSetupParamsBuilder withMaxAutoscaleInstances(int maxAutoscaleInstances) {
      this.maxAutoscaleInstances = maxAutoscaleInstances;
      return this;
    }

    public KubernetesSetupParamsBuilder withTargetCpuUtilizationPercentage(int targetCpuUtilizationPercentage) {
      this.targetCpuUtilizationPercentage = targetCpuUtilizationPercentage;
      return this;
    }

    public KubernetesSetupParamsBuilder withCustomMetricYamlConfig(String customMetricYamlConfig) {
      this.customMetricYamlConfig = customMetricYamlConfig;
      return this;
    }

    public KubernetesSetupParamsBuilder withSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public KubernetesSetupParamsBuilder withResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public KubernetesSetupParamsBuilder withUseIngress(boolean useIngress) {
      this.useIngress = useIngress;
      return this;
    }

    public KubernetesSetupParamsBuilder withIngressYaml(String ingressYaml) {
      this.ingressYaml = ingressYaml;
      return this;
    }

    public KubernetesSetupParamsBuilder withUseIstioRouteRule(boolean useIstioRouteRule) {
      this.useIstioRouteRule = useIstioRouteRule;
      return this;
    }

    public KubernetesSetupParamsBuilder withConfigMapYaml(String configMapYaml) {
      this.configMapYaml = configMapYaml;
      return this;
    }

    public KubernetesSetupParamsBuilder withPlainConfigFiles(List<String[]> plainConfigFiles) {
      this.plainConfigFiles = plainConfigFiles;
      return this;
    }

    public KubernetesSetupParamsBuilder withEncryptedConfigFiles(List<String[]> encryptedConfigFiles) {
      this.encryptedConfigFiles = encryptedConfigFiles;
      return this;
    }

    public KubernetesSetupParamsBuilder but() {
      return aKubernetesSetupParams()
          .withServiceName(serviceName)
          .withClusterName(clusterName)
          .withAppName(appName)
          .withEnvName(envName)
          .withImageDetails(imageDetails)
          .withContainerTask(containerTask)
          .withInfraMappingId(infraMappingId)
          .withServiceType(serviceType)
          .withPort(port)
          .withTargetPort(targetPort)
          .withPortName(portName)
          .withProtocol(protocol)
          .withClusterIP(clusterIP)
          .withExternalIPs(externalIPs)
          .withLoadBalancerIP(loadBalancerIP)
          .withNodePort(nodePort)
          .withExternalName(externalName)
          .withNamespace(namespace)
          .withControllerNamePrefix(controllerNamePrefix)
          .withPreviousDaemonSetYaml(previousDaemonSetYaml)
          .withPreviousActiveAutoscalers(previousActiveAutoscalers)
          .withRollback(rollback)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withUseAutoscaler(useAutoscaler)
          .withMinAutoscaleInstances(minAutoscaleInstances)
          .withMaxAutoscaleInstances(maxAutoscaleInstances)
          .withTargetCpuUtilizationPercentage(targetCpuUtilizationPercentage)
          .withCustomMetricYamlConfig(customMetricYamlConfig)
          .withSubscriptionId(subscriptionId)
          .withResourceGroup(resourceGroup)
          .withUseIngress(useIngress)
          .withIngressYaml(ingressYaml)
          .withUseIstioRouteRule(useIstioRouteRule)
          .withConfigMapYaml(configMapYaml)
          .withPlainConfigFiles(plainConfigFiles)
          .withEncryptedConfigFiles(encryptedConfigFiles);
    }

    public KubernetesSetupParams build() {
      KubernetesSetupParams kubernetesSetupParams = new KubernetesSetupParams();
      kubernetesSetupParams.setServiceName(serviceName);
      kubernetesSetupParams.setClusterName(clusterName);
      kubernetesSetupParams.setAppName(appName);
      kubernetesSetupParams.setEnvName(envName);
      kubernetesSetupParams.setImageDetails(imageDetails);
      kubernetesSetupParams.setContainerTask(containerTask);
      kubernetesSetupParams.setInfraMappingId(infraMappingId);
      kubernetesSetupParams.setServiceType(serviceType);
      kubernetesSetupParams.setPort(port);
      kubernetesSetupParams.setTargetPort(targetPort);
      kubernetesSetupParams.setPortName(portName);
      kubernetesSetupParams.setProtocol(protocol);
      kubernetesSetupParams.setClusterIP(clusterIP);
      kubernetesSetupParams.setExternalIPs(externalIPs);
      kubernetesSetupParams.setLoadBalancerIP(loadBalancerIP);
      kubernetesSetupParams.setNodePort(nodePort);
      kubernetesSetupParams.setExternalName(externalName);
      kubernetesSetupParams.setNamespace(namespace);
      kubernetesSetupParams.setControllerNamePrefix(controllerNamePrefix);
      kubernetesSetupParams.setPreviousDaemonSetYaml(previousDaemonSetYaml);
      kubernetesSetupParams.setPreviousActiveAutoscalers(previousActiveAutoscalers);
      kubernetesSetupParams.setRollback(rollback);
      kubernetesSetupParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      kubernetesSetupParams.setUseAutoscaler(useAutoscaler);
      kubernetesSetupParams.setMinAutoscaleInstances(minAutoscaleInstances);
      kubernetesSetupParams.setMaxAutoscaleInstances(maxAutoscaleInstances);
      kubernetesSetupParams.setTargetCpuUtilizationPercentage(targetCpuUtilizationPercentage);
      kubernetesSetupParams.setCustomMetricYamlConfig(customMetricYamlConfig);
      kubernetesSetupParams.setSubscriptionId(subscriptionId);
      kubernetesSetupParams.setResourceGroup(resourceGroup);
      kubernetesSetupParams.setUseIngress(useIngress);
      kubernetesSetupParams.setIngressYaml(ingressYaml);
      kubernetesSetupParams.setUseIstioRouteRule(useIstioRouteRule);
      kubernetesSetupParams.setConfigMapYaml(configMapYaml);
      kubernetesSetupParams.setPlainConfigFiles(plainConfigFiles);
      kubernetesSetupParams.setEncryptedConfigFiles(encryptedConfigFiles);
      return kubernetesSetupParams;
    }
  }
}

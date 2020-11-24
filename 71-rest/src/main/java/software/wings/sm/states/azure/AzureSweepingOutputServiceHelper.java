package software.wings.sm.states.azure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.deployment.InstanceDetails.InstanceType.AZURE_VMSS;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import static java.util.stream.Collectors.toList;

import io.harness.beans.SweepingOutputInstance;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;
import io.harness.deployment.InstanceDetails;

import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.common.InfrastructureConstants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
public class AzureSweepingOutputServiceHelper {
  public static final String HOST = "host";

  @Inject private ServiceTemplateHelper serviceTemplateHelper;
  @Inject private HostService hostService;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private SweepingOutputService sweepingOutputService;

  public List<InstanceElement> generateInstanceElements(
      ExecutionContext context, AzureVMSSInfrastructureMapping infraMapping, List<AzureVMInstanceData> vmInstances) {
    return vmInstances.stream()
        .filter(Objects::nonNull)
        .map(instance -> {
          Host host = aHost()
                          .withHostName(instance.getPrivateDnsName())
                          .withPublicDns(instance.getPublicDnsName())
                          .withAppId(infraMapping.getAppId())
                          .withEnvId(infraMapping.getEnvId())
                          .withHostConnAttr(infraMapping.getHostConnectionAttrs())
                          .withInfraMappingId(infraMapping.getUuid())
                          .withInfraDefinitionId(infraMapping.getInfrastructureDefinitionId())
                          .withServiceTemplateId(serviceTemplateHelper.fetchServiceTemplateId(infraMapping))
                          .build();
          Host savedHost = hostService.saveHost(host);
          HostElement hostElement = HostElement.builder()
                                        .uuid(savedHost.getUuid())
                                        .publicDns(instance.getPublicDnsName())
                                        .ip(instance.getPrivateIpAddress())
                                        .azureVMInstance(instance)
                                        .instanceId(instance.getInstanceId())
                                        .build();
          final Map<String, Object> contextMap = context.asMap();
          contextMap.put(HOST, hostElement);
          String hostName = getHostnameFromConvention(contextMap, "");
          hostElement.setHostName(hostName);
          return anInstanceElement()
              .uuid(instance.getInstanceId())
              .hostName(hostName)
              .displayName(instance.getPublicDnsName())
              .host(hostElement)
              .build();
        })
        .collect(toList());
  }

  public List<InstanceDetails> generateAzureVMSSInstanceDetails(List<InstanceElement> allInstanceElements) {
    return allInstanceElements.stream()
        .filter(Objects::nonNull)
        .map(instanceElement
            -> InstanceDetails.builder()
                   .instanceType(AZURE_VMSS)
                   .newInstance(instanceElement.isNewInstance())
                   .hostName(instanceElement.getHostName())
                   .azureVmss(
                       InstanceDetails.AZURE_VMSS.builder()
                           .publicDns(
                               instanceElement.getHost() != null ? instanceElement.getHost().getPublicDns() : null)
                           .instanceId(
                               instanceElement.getHost() != null ? instanceElement.getHost().getInstanceId() : null)
                           .ip(instanceElement.getHost() != null ? instanceElement.getHost().getIp() : null)
                           .build())
                   .build())
        .collect(toList());
  }

  public String getHostnameFromConvention(Map<String, Object> context, String hostNameConvention) {
    if (isEmpty(hostNameConvention)) {
      hostNameConvention = InfrastructureConstants.DEFAULT_AZURE_VM_HOST_NAME_CONVENTION;
    }
    return expressionEvaluator.substitute(hostNameConvention, context);
  }

  public void saveInstanceDetails(
      ExecutionContext context, List<InstanceElement> instanceElements, List<InstanceDetails> instanceDetails) {
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                                   .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
                                   .value(InstanceInfoVariables.builder()
                                              .instanceElements(instanceElements)
                                              .instanceDetails(instanceDetails)
                                              .skipVerification(isEmpty(instanceDetails))
                                              .build())
                                   .build());
  }

  public void saveInstanceInfoToSweepingOutput(ExecutionContext context, int trafficShift) {
    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
            .value(InstanceInfoVariables.builder().newInstanceTrafficPercent(trafficShift).build())
            .build());
  }
}

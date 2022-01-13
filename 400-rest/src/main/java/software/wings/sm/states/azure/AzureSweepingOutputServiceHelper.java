/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.deployment.InstanceDetails.InstanceType.AZURE_VMSS;
import static io.harness.deployment.InstanceDetails.InstanceType.AZURE_WEBAPP;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import static java.util.stream.Collectors.toList;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidRequestException;

import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.common.InfrastructureConstants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
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
          String hostName =
              getHostnameFromConvention(contextMap, InfrastructureConstants.DEFAULT_AZURE_VM_HOST_NAME_CONVENTION);
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

  private String getHostnameFromConvention(Map<String, Object> context, String hostNameConvention) {
    if (isEmpty(hostNameConvention)) {
      throw new InvalidRequestException("Instance element host name convention is empty or null");
    }

    return expressionEvaluator.substitute(hostNameConvention, context);
  }

  public void saveInstanceDetails(
      ExecutionContext context, List<InstanceElement> instanceElements, List<InstanceDetails> instanceDetails) {
    boolean skipVerification = instanceDetails.stream().noneMatch(InstanceDetails::isNewInstance);
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                                   .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
                                   .value(InstanceInfoVariables.builder()
                                              .instanceElements(instanceElements)
                                              .instanceDetails(instanceDetails)
                                              .skipVerification(skipVerification)
                                              .build())
                                   .build());
  }

  public void saveTrafficShiftInfoToSweepingOutput(ExecutionContext context, double trafficShift) {
    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
            .value(InstanceInfoVariables.builder().newInstanceTrafficPercent((int) trafficShift).build())
            .build());
  }

  public List<InstanceElement> generateAzureAppInstanceElements(ExecutionContext context,
      AzureWebAppInfrastructureMapping infraMapping, List<AzureAppDeploymentData> appDeploymentData) {
    return appDeploymentData.stream()
        .filter(Objects::nonNull)
        .map(webAppInstance -> {
          Host host = aHost()
                          .withHostName(webAppInstance.getAppName())
                          .withPublicDns(webAppInstance.getHostName())
                          .withAppId(infraMapping.getAppId())
                          .withEnvId(infraMapping.getEnvId())
                          .withInfraMappingId(infraMapping.getUuid())
                          .withInfraDefinitionId(infraMapping.getInfrastructureDefinitionId())
                          .withServiceTemplateId(serviceTemplateHelper.fetchServiceTemplateId(infraMapping))
                          .build();
          Host savedHost = hostService.saveHost(host);
          HostElement hostElement = HostElement.builder()
                                        .uuid(savedHost.getUuid())
                                        .publicDns(webAppInstance.getHostName())
                                        .ip(webAppInstance.getInstanceIp())
                                        .webAppInstance(webAppInstance)
                                        .instanceId(webAppInstance.getInstanceId())
                                        .build();
          final Map<String, Object> contextMap = context.asMap();
          contextMap.put(HOST, hostElement);
          String hostName =
              getHostnameFromConvention(contextMap, InfrastructureConstants.DEFAULT_WEB_APP_HOST_NAME_CONVENTION);
          hostElement.setHostName(hostName);
          return anInstanceElement()
              .uuid(webAppInstance.getInstanceId())
              .hostName(hostName)
              .displayName(webAppInstance.getInstanceName())
              .host(hostElement)
              .newInstance(true)
              .build();
        })
        .collect(toList());
  }

  public List<InstanceDetails> generateAzureAppServiceInstanceDetails(List<AzureAppDeploymentData> appDeploymentData) {
    return appDeploymentData.stream()
        .filter(Objects::nonNull)
        .map(instanceElement
            -> InstanceDetails.builder()
                   .instanceType(AZURE_WEBAPP)
                   .newInstance(true)
                   .hostName(instanceElement.getHostName())
                   .azureWebapp(InstanceDetails.AZURE_WEBAPP.builder()
                                    .ip(instanceElement.getInstanceIp())
                                    .appName(instanceElement.getAppName())
                                    .appServicePlanId(instanceElement.getAppServicePlanId())
                                    .deploySlot(instanceElement.getDeploySlot())
                                    .deploySlotId(instanceElement.getDeploySlotId())
                                    .instanceHostName(instanceElement.getHostName())
                                    .instanceId(instanceElement.getInstanceId())
                                    .instanceName(instanceElement.getInstanceName())
                                    .instanceType(instanceElement.getInstanceType())
                                    .build())
                   .build())
        .collect(toList());
  }

  public SweepingOutput getInfoFromSweepingOutput(ExecutionContext context, String prefix) {
    String sweepingOutputName = getSweepingOutputName(context, prefix);
    SweepingOutputInquiry inquiry = context.prepareSweepingOutputInquiryBuilder().name(sweepingOutputName).build();
    return sweepingOutputService.findSweepingOutput(inquiry);
  }

  public void saveToSweepingOutPut(SweepingOutput value, String prefix, ExecutionContext context) {
    String sweepingOutputName = getSweepingOutputName(context, prefix);
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                                   .name(sweepingOutputName)
                                   .value(value)
                                   .build());
  }

  protected String getSweepingOutputName(ExecutionContext context, String prefix) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String suffix = phaseElement.getServiceElement().getUuid().trim();
    return prefix + suffix;
  }

  public boolean dataExist(ExecutionContext context, String prefix) {
    String sweepingOutputName = getSweepingOutputName(context, prefix);
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(sweepingOutputName).build());
    return sweepingOutputInstance != null;
  }
}

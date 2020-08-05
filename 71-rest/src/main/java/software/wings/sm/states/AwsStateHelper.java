package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.deployment.InstanceDetails.InstanceType.AWS;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.context.ContextElementType;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class AwsStateHelper {
  public static final String HOST = "host";
  @Inject private AwsUtils awsUtils;
  @Inject private HostService hostService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private ServiceTemplateService templateService;
  @Inject private ServiceTemplateHelper serviceTemplateHelper;

  public List<InstanceElement> generateInstanceElements(
      List<Instance> ec2InstancesAded, InfrastructureMapping infraMapping, ExecutionContext context) {
    List<InstanceElement> instanceElementList = emptyList();
    if (isNotEmpty(ec2InstancesAded)) {
      instanceElementList =
          ec2InstancesAded.stream()
              .map(instance -> {
                Host host = aHost()
                                .withHostName(awsUtils.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                                .withPublicDns(instance.getPublicDnsName())
                                .withEc2Instance(instance)
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
                                              .ec2Instance(instance)
                                              .instanceId(instance.getInstanceId())
                                              .build();

                final Map<String, Object> contextMap = context.asMap();
                contextMap.put(HOST, hostElement);
                String hostName = awsHelperService.getHostnameFromConvention(contextMap, "");
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

    return instanceElementList;
  }

  int fetchRequiredAsgCapacity(Map<String, Integer> currentCapacities, String asgName) {
    Integer capacity = currentCapacities.get(asgName);
    if (capacity == null) {
      throw new InvalidRequestException(format("Current Capacity of Asg: [%s] not found", asgName));
    }
    return capacity;
  }

  public List<InstanceDetails> generateAmInstanceDetails(List<InstanceElement> allInstanceElements) {
    if (isEmpty(allInstanceElements)) {
      return emptyList();
    }

    return allInstanceElements.stream()
        .filter(instanceElement -> instanceElement != null)
        .map(instanceElement
            -> InstanceDetails.builder()
                   .instanceType(AWS)
                   .newInstance(instanceElement.isNewInstance())
                   .hostName(instanceElement.getHostName())
                   .aws(InstanceDetails.AWS.builder()
                            .ec2Instance(
                                instanceElement.getHost() != null ? instanceElement.getHost().getEc2Instance() : null)
                            .publicDns(
                                instanceElement.getHost() != null ? instanceElement.getHost().getPublicDns() : null)
                            .instanceId(
                                instanceElement.getHost() != null ? instanceElement.getHost().getInstanceId() : null)
                            .ip(instanceElement.getHost() != null ? instanceElement.getHost().getIp() : null)
                            .build())
                   .build())
        .collect(toList());
  }

  public Integer getAmiStateTimeoutFromContext(ExecutionContext context) {
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    if (serviceSetupElement == null || serviceSetupElement.getAutoScalingSteadyStateTimeout() == null
        || Integer.valueOf(0).equals(serviceSetupElement.getAutoScalingSteadyStateTimeout())) {
      return null;
    }
    return getTimeout(serviceSetupElement.getAutoScalingSteadyStateTimeout());
  }

  @Nullable
  public Integer getTimeout(Integer timeoutInMinutes) {
    try {
      return Ints.checkedCast(TimeUnit.MINUTES.toMillis(timeoutInMinutes));
    } catch (Exception e) {
      logger.warn("Could not convert {} minutes to millis, falling back to default timeout", timeoutInMinutes);
      return null;
    }
  }
}

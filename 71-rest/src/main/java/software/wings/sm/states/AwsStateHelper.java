package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.exception.InvalidRequestException;
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
}

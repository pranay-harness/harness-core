package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.service.impl.aws.model.AwsConstants.PHASE_PARAM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.context.ContextElementType;
import org.mongodb.morphia.Key;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsUtils;
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

  public List<InstanceElement> generateInstanceElements(
      List<Instance> ec2InstancesAded, InfrastructureMapping infraMapping, ExecutionContext context) {
    List<InstanceElement> instanceElementList = emptyList();
    if (isNotEmpty(ec2InstancesAded)) {
      Key<ServiceTemplate> serviceTemplateKey = templateService
                                                    .getTemplateRefKeysByService(infraMapping.getAppId(),
                                                        infraMapping.getServiceId(), infraMapping.getEnvId())
                                                    .get(0);
      PhaseElement phaseEle = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);

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
                                .withServiceTemplateId(infraMapping.getServiceTemplateId())
                                .build();
                Host savedHost = hostService.saveHost(host);
                HostElement hostElement = aHostElement()
                                              .withUuid(savedHost.getUuid())
                                              .withPublicDns(instance.getPublicDnsName())
                                              .withIp(instance.getPrivateIpAddress())
                                              .withEc2Instance(instance)
                                              .withInstanceId(instance.getInstanceId())
                                              .build();

                final Map<String, Object> contextMap = context.asMap();
                contextMap.put(HOST, hostElement);
                String hostName = awsHelperService.getHostnameFromConvention(contextMap, "");
                hostElement.setHostName(hostName);
                return anInstanceElement()
                    .withUuid(instance.getInstanceId())
                    .withHostName(hostName)
                    .withDisplayName(instance.getPublicDnsName())
                    .withHost(hostElement)
                    .withServiceTemplateElement(aServiceTemplateElement()
                                                    .withUuid(serviceTemplateKey.getId().toString())
                                                    .withServiceElement(phaseEle.getServiceElement())
                                                    .build())
                    .build();
              })
              .collect(toList());
    }

    return instanceElementList;
  }
}

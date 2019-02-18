package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.api.ecs.EcsRoute53WeightUpdateStateExecutionData;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53DNSWeightUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53ServiceSetupRequest;
import software.wings.helpers.ext.ecs.request.EcsBGServiceSetupRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53DNSWeightUpdateResponse;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53ServiceSetupResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceSetupResponse;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sDeleteResponse;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesRequest;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesResponse;
import software.wings.service.impl.aws.model.AwsRoute53Request;
import software.wings.service.impl.aws.model.AwsRoute53Request.AwsRoute53RequestType;

public class ManagerKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(EcsBGRoute53ServiceSetupRequest.class, 7101);
    kryo.register(EcsBGRoute53ServiceSetupResponse.class, 7102);
    kryo.register(EcsBGRoute53DNSWeightUpdateRequest.class, 7103);
    kryo.register(EcsBGRoute53DNSWeightUpdateResponse.class, 7104);
    kryo.register(EcsRoute53WeightUpdateStateExecutionData.class, 7105);
    kryo.register(AwsRoute53Request.class, 7106);
    kryo.register(AwsRoute53RequestType.class, 7107);
    kryo.register(AwsRoute53ListHostedZonesRequest.class, 7108);
    kryo.register(AwsRoute53ListHostedZonesResponse.class, 7109);
    kryo.register(AwsRoute53HostedZoneData.class, 7110);
    kryo.register(Action.class, 7111);
    kryo.register(ShellScriptApprovalExecutionData.class, 7112);
    kryo.register(K8sDeleteTaskParameters.class, 7113);
    kryo.register(K8sDeleteResponse.class, 7114);
    kryo.register(NotificationChannelType.class, 7115);
    kryo.register(AwsLambdaFunctionRequest.class, 7116);
    kryo.register(AwsLambdaFunctionResponse.class, 7117);
    kryo.register(PcfInfraMappingDataRequest.ActionType.class, 7118);
    kryo.register(SlackNotificationSetting.class, 7119);
    kryo.register(AwsAmiSetupExecutionData.class, 7120);
    kryo.register(EcsServiceSetupRequest.class, 7121);
    kryo.register(EcsServiceSetupResponse.class, 7122);
    kryo.register(EcsBGServiceSetupRequest.class, 7123);
  }
}
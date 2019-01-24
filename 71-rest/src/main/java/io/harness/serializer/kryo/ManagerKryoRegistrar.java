package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import software.wings.api.ecs.EcsRoute53WeightUpdateStateExecutionData;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53DNSWeightUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53ServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53DNSWeightUpdateResponse;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53ServiceSetupResponse;
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
  }
}

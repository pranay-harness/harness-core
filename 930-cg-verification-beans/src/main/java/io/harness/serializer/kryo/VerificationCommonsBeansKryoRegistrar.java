package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.AppDynamicsConfig;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CV)
public class VerificationCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ThirdPartyApiCallLog.class, 5377);
    kryo.register(ThirdPartyApiCallLog.ThirdPartyApiCallField.class, 71100);
    kryo.register(ThirdPartyApiCallLog.FieldType.class, 71101);
    kryo.register(AppDynamicsConfig.class, 5074);
    kryo.register(SetupTestNodeData.class, 5530);
    kryo.register(AppdynamicsSetupTestNodeData.class, 5531);
    kryo.register(SetupTestNodeData.Instance.class, 7470);
  }
}

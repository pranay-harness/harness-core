package io.harness.serializer.kryo;

import io.harness.capability.AwsRegionParameters;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.ChartMuseumParameters;
import io.harness.capability.HelmInstallationParameters;
import io.harness.capability.HttpConnectionParameters;
import io.harness.capability.ProcessExecutorParameters;
import io.harness.capability.SftpCapabilityParameters;
import io.harness.capability.SmbConnectionParameters;
import io.harness.capability.SocketConnectivityParameters;
import io.harness.capability.SystemEnvParameters;
import io.harness.capability.TestingCapability;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.UnknownFieldSet;

public class CapabilityKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CapabilityParameters.class, 10001);
    kryo.register(PermissionResult.class, 10002);
    kryo.register(UnknownFieldSet.class, 10003);
    kryo.register(TestingCapability.class, 10004);
    kryo.register(SmbConnectionParameters.class, 10005);
    kryo.register(SftpCapabilityParameters.class, 10006);
    kryo.register(AwsRegionParameters.class, 10007);
    kryo.register(ChartMuseumParameters.class, 10008);
    kryo.register(HttpConnectionParameters.class, 10009);
    kryo.register(ProcessExecutorParameters.class, 10010);
    kryo.register(SocketConnectivityParameters.class, 10011);
    kryo.register(SystemEnvParameters.class, 10012);
    kryo.register(HelmInstallationParameters.class, 10013);
  }
}

package io.harness.serializer.kryo;

import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.YamlDiff;
import io.harness.audit.beans.YamlRecord;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGAuditCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AuditEventData.class, 17360);
    kryo.register(AuthenticationInfo.class, 17361);
    kryo.register(YamlRecord.class, 17362);
    kryo.register(YamlDiff.class, 17363);
    kryo.register(AuditEventDTO.class, 17364);
  }
}

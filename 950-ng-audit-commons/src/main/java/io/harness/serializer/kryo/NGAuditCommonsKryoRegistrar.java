package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.beans.YamlDiff;
import io.harness.audit.beans.YamlRecord;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
public class NGAuditCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AuditEventData.class, 17360);
    kryo.register(Principal.class, 17361);
    kryo.register(AuthenticationInfoDTO.class, 17362);
    kryo.register(YamlRecord.class, 17363);
    kryo.register(YamlDiff.class, 17364);
    kryo.register(AuditEventDTO.class, 17365);
    kryo.register(AuditEntry.class, 17366);
    kryo.register(ResourceScopeDTO.class, 17367);
    kryo.register(ResourceDTO.class, 17368);
  }
}

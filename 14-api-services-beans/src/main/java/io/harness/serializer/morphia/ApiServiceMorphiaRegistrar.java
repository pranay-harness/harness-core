package io.harness.serializer.morphia;

import io.harness.deployment.InstanceDetails;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import java.util.Set;

public class ApiServiceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(EncryptionConfig.class);
    set.add(EncryptedRecord.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
    h.put("globalcontex.PurgeGlobalContextData", PurgeGlobalContextData.class);
    h.put("deployment.InstanceDetails", InstanceDetails.class);
  }
}

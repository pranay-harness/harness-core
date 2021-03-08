package io.harness.serializer.morphia;

import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.HttpRequestInfo;
import io.harness.audit.beans.RequestMetadata;
import io.harness.audit.beans.Resource;
import io.harness.audit.beans.YamlDiff;
import io.harness.audit.beans.YamlRecord;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class NGAuditCommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AuditEventData.class);
    set.add(AuthenticationInfo.class);
    set.add(HttpRequestInfo.class);
    set.add(RequestMetadata.class);
    set.add(Resource.class);
    set.add(YamlRecord.class);
    set.add(YamlDiff.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes for registration
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import io.harness.deployment.InstanceDetails;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.shell.ShellExecutionData;

import software.wings.annotation.EncryptableSetting;

import java.util.Set;

public class ApiServiceBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(EncryptionConfig.class);
    set.add(EncryptedRecord.class);
    set.add(EncryptableSetting.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
    h.put("globalcontex.PurgeGlobalContextData", PurgeGlobalContextData.class);
    h.put("deployment.InstanceDetails", InstanceDetails.class);
    h.put("shell", ShellExecutionData.class);
    w.put("beans.command.ShellExecutionData", ShellExecutionData.class);
  }
}

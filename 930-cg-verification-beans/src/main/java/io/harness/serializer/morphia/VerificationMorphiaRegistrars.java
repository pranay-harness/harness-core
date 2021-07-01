
package io.harness.serializer.morphia;

import io.harness.cv.WorkflowVerificationResult;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.verification.CVActivityLog;

import java.util.Set;

public class VerificationMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
	  set.add(CVActivityLog.class);
	  set.add(WorkflowVerificationResult.class);
	  set.add(ThirdPartyApiCallLog.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}

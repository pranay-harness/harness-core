package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.registries.ResolverRegistry;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.steps.io.ResolvedRefInput;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepInputPackage.StepInputPackageBuilder;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
public class EngineObtainmentHelper {
  @Inject private ResolverRegistry resolverRegistry;

  public StepInputPackage obtainInputPackage(Ambiance ambiance, List<RefObject> refObjects) {
    StepInputPackageBuilder inputPackageBuilder = StepInputPackage.builder();

    if (!isEmpty(refObjects)) {
      for (RefObject refObject : refObjects) {
        Resolver resolver = resolverRegistry.obtain(refObject.getRefType());
        inputPackageBuilder.input(
            ResolvedRefInput.builder().transput(resolver.resolve(ambiance, refObject)).refObject(refObject).build());
      }
    }
    return inputPackageBuilder.build();
  }
}

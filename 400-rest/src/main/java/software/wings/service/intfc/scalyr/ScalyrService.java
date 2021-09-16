package software.wings.service.intfc.scalyr;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.util.Map;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ScalyrService {
  Map<String, Map<String, ResponseMapper>> createLogCollectionMapping(
      String hostnameField, String messageField, String timestampField);
}

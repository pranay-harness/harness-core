package software.wings.service.intfc.scalyr;

import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.util.Map;

public interface ScalyrService {
  Map<String, Map<String, ResponseMapper>> createLogCollectionMapping(
      String hostnameField, String messageField, String timestampField);
}

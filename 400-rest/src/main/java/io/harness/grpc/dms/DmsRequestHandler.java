package io.harness.grpc.dms;

import io.harness.dms.CrudEventRequest;
import io.harness.dms.CrudEventResponse;

public interface DmsRequestHandler {
  boolean canHandle(CrudEventRequest request);

  CrudEventResponse handle(CrudEventRequest request);
}

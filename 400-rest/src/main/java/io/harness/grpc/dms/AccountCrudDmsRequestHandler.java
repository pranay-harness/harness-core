package io.harness.grpc.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dms.Action;
import io.harness.dms.CrudEventRequest;
import io.harness.dms.CrudEventResponse;
import io.harness.dms.EntityType;
import io.harness.exception.InvalidRequestException;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DEL)
@Slf4j
public class AccountCrudDmsRequestHandler implements DmsRequestHandler {
  @Override
  public boolean canHandle(CrudEventRequest request) {
    return request.getEntityType().equals(EntityType.ACCOUNT);
  }

  @Override
  public CrudEventResponse handle(CrudEventRequest request) {
    final Action action = request.getAction();
    switch (action) {
      case CREATE:
      case DELETE:
      default:
        throw new InvalidRequestException(String.format("No handler for action type [%s]", action));
    }
  }
}

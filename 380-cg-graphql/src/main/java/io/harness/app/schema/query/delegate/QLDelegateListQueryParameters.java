package io.harness.app.schema.query.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(DEL)
@Value
public class QLDelegateListQueryParameters {
  String accountId;
}

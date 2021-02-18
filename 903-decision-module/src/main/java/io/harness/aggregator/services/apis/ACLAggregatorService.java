package io.harness.aggregator.services.apis;

import io.harness.accesscontrol.Principal;

public interface ACLAggregatorService {
  boolean aggregate(Principal principal);
}

package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceBlockingStub;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// todo(@NamanVerma): add test for this class when the proper method implementations are there
@OwnedBy(PIPELINE)
public class JsonExpander {
  @Inject Map<ModuleType, JsonExpansionServiceBlockingStub> jsonExpansionServiceBlockingStubMap;

  public Set<ExpansionResponseBatch> fetchExpansionResponses(Set<ExpansionRequest> expansionRequests) {
    Map<ModuleType, ExpansionRequestBatch> expansionRequestBatches = batchExpansionRequests(expansionRequests);
    // todo(@NamanVerma): each batch should go in a different thread
    Set<ExpansionResponseBatch> expansionResponseBatches = new HashSet<>();
    expansionRequestBatches.keySet().forEach(module -> {
      JsonExpansionServiceBlockingStub blockingStub = jsonExpansionServiceBlockingStubMap.get(module);
      ExpansionResponseBatch expansionResponseBatch =
          PmsGrpcClientUtils.retryAndProcessException(blockingStub::expand, expansionRequestBatches.get(module));
      expansionResponseBatches.add(expansionResponseBatch);
    });
    return expansionResponseBatches;
  }

  private Map<ModuleType, ExpansionRequestBatch> batchExpansionRequests(Set<ExpansionRequest> expansionRequests) {
    // todo(@NamanVerma): implement this!
    return Collections.emptyMap();
  }
}

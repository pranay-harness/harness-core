package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.governance.ExpansionPlacementStrategy;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.ExpansionResponseProto;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.yaml.YamlNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

// todo(@NamanVerma): write test
@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class ExpansionsMerger {
  public String mergeExpansions(String pipelineYaml, Set<ExpansionResponseBatch> responseBatches) {
    Map<String, String> fqnToUpdateMap = new HashMap<>();
    List<String> toBeRemovedFQNs = new ArrayList<>();
    for (ExpansionResponseBatch expansionResponseBatch : responseBatches) {
      List<ExpansionResponseProto> expansionResponseProtoList = expansionResponseBatch.getExpansionResponseProtoList();
      for (ExpansionResponseProto response : expansionResponseProtoList) {
        if (!response.getSuccess()) {
          log.warn("Failed to get expansion for: " + response.getFqn() + ". Error: " + response.getErrorMessage());
          continue;
        }
        String key = response.getKey();
        if (EmptyPredicate.isEmpty(key)) {
          log.warn("No key provided for expansion for: " + response.getFqn());
          continue;
        }
        String newFQN = getNewFQN(response.getFqn(), key, response.getPlacement());
        String value = response.getValue();
        fqnToUpdateMap.put(newFQN, value);
        if (response.getPlacement().equals(ExpansionPlacementStrategy.REPLACE)) {
          toBeRemovedFQNs.add(response.getFqn());
        }
      }
    }
    String jsonWithUpdates = MergeHelper.mergeUpdatesIntoJson(pipelineYaml, fqnToUpdateMap);
    return MergeHelper.removeFQNs(jsonWithUpdates, toBeRemovedFQNs);
  }

  String getNewFQN(String fqn, String key, ExpansionPlacementStrategy placementStrategy) {
    switch (placementStrategy) {
      case MOVE_UP:
        List<String> split = Arrays.asList(fqn.split(YamlNode.PATH_SEP));
        split.remove(split.size() - 1);
        split.add(split.size() - 1, key);
        return String.join(YamlNode.PATH_SEP, split);
      case REPLACE:
      case PARALLEL:
        String[] fqnSplit = fqn.split(YamlNode.PATH_SEP);
        fqnSplit[fqnSplit.length - 1] = key;
        return String.join(YamlNode.PATH_SEP, fqnSplit);
      default:
        throw new InvalidRequestException(placementStrategy.name() + " placement strategy not supported");
    }
  }
}

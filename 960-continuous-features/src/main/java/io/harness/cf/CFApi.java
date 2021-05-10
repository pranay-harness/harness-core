package io.harness.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.openapi.ApiClient;
import io.harness.cf.openapi.api.DefaultApi;
import io.harness.cf.openapi.model.Clause;
import io.harness.cf.openapi.model.FeatureState;
import io.harness.cf.openapi.model.PatchInstruction;
import io.harness.cf.openapi.model.Serve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@OwnedBy(HarnessTeam.CF)
public class CFApi extends DefaultApi {
  public CFApi() {}

  public CFApi(ApiClient apiClient) {
    super(apiClient);
  }

  public List<PatchInstruction> getFeatureFlagRulesForTargetingAccounts(
      Collection<String> accountIDs, int startPriority) {
    int priority = startPriority;
    List<PatchInstruction> patchInstructions = new ArrayList<>();
    for (String account : accountIDs) {
      PatchInstruction patchInstruction = PatchInstruction.builder()
                                              .kind("addRule")
                                              .parameters(AddRuleParam.getParamsForAccountID(account, priority))
                                              .build();
      patchInstructions.add(patchInstruction);
      priority = priority + 100;
    }
    return patchInstructions;
  }

  public PatchInstruction removeRule(String ruleID) {
    return PatchInstruction.builder().kind("removeRule").parameters(new RemoveRuleParam(ruleID)).build();
  }

  public PatchInstruction getFeatureFlagDefaultServePatchInstruction(boolean variation) {
    String variationString;
    if (variation) {
      variationString = "true";
    } else {
      variationString = "false";
    }

    return PatchInstruction.builder()
        .kind("updateDefaultServe")
        .parameters(new UpdateDefaultServeParams(variationString))
        .build();
  }

  public PatchInstruction getFeatureFlagOnPatchInstruction(boolean state) {
    String stateString;
    if (state) {
      stateString = FeatureState.ON.getValue();
    } else {
      stateString = FeatureState.OFF.getValue();
    }

    return PatchInstruction.builder()
        .kind("setFeatureFlagState")
        .parameters(new FeatureFlagStateParams(stateString))
        .build();
  }

  public PatchInstruction getAddTargetToVariationMapParams(String variation, List<String> targets) {
    return PatchInstruction.builder()
        .kind("addTargetsToVariationTargetMap")
        .parameters(new AddTargetToVariationMapParams(variation, targets))
        .build();
  }

  public PatchInstruction getRemoveTargetToVariationMapParams(String variation, List<String> targets) {
    return PatchInstruction.builder()
        .kind("removeTargetsToVariationTargetMap")
        .parameters(new RemoveTargetToVariationMapParams(variation, targets))
        .build();
  }

  public PatchInstruction getAddSegmentToVariationMapParams(String variation, List<String> segments) {
    return PatchInstruction.builder()
        .kind("addSegmentToVariationTargetMap")
        .parameters(new AddSegmentToVariationMapParams(variation, segments))
        .build();
  }

  public PatchInstruction getRemoveSegmentToVariationMapParams(String variation, List<String> segments) {
    return PatchInstruction.builder()
        .kind("removeSegmentToVariationTargetMap")
        .parameters(new RemoveSegmentToVariationMapParams(variation, segments))
        .build();
  }
}

class FeatureFlagStateParams {
  String state;
  FeatureFlagStateParams(String state) {
    this.state = state;
  }
}

class UpdateDefaultServeParams {
  String variation;

  UpdateDefaultServeParams(String variation) {
    this.variation = variation;
  }
}

class AddTargetToVariationMapParams {
  String variation;
  List<String> targets;

  AddTargetToVariationMapParams(String variation, List<String> targets) {
    this.variation = variation;
    this.targets = targets;
  }
}

class RemoveTargetToVariationMapParams {
  String variation;
  List<String> targets;

  RemoveTargetToVariationMapParams(String variation, List<String> targets) {
    this.variation = variation;
    this.targets = targets;
  }
}

class AddSegmentToVariationMapParams {
  String variation;
  List<String> segments;

  AddSegmentToVariationMapParams(String variation, List<String> segments) {
    this.variation = variation;
    this.segments = segments;
  }
}

class RemoveSegmentToVariationMapParams {
  String variation;
  List<String> segments;

  RemoveSegmentToVariationMapParams(String variation, List<String> segments) {
    this.variation = variation;
    this.segments = segments;
  }
}

class RemoveRuleParam {
  String ruleID;

  RemoveRuleParam(String ruleID) {
    this.ruleID = ruleID;
  }
}

class AddRuleParam {
  List<io.harness.cf.openapi.model.Clause> clauses = new ArrayList<>();
  int priority;
  io.harness.cf.openapi.model.Serve serve;
  String uuid;

  public static AddRuleParam getParamsForAccountID(String accountID, int priority) {
    AddRuleParam param = new AddRuleParam();
    param.priority = priority;
    param.serve = new Serve();
    param.serve.variation("true");
    param.uuid = UUID.randomUUID().toString();
    io.harness.cf.openapi.model.Clause clause = new Clause();
    clause.attribute("identifier");
    clause.op("equal");
    clause.values(Arrays.asList(accountID));
    param.clauses = Arrays.asList(clause);
    return param;
  }
}

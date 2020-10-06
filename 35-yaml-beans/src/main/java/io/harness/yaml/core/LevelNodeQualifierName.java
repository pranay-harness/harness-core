package io.harness.yaml.core;

import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;

public interface LevelNodeQualifierName {
  String EXECUTION_ELEMENT = "execution";
  String GRAPH = "graph";
  String STAGES_ELEMENT = "stages";
  String PARALLEL_STAGE_ELEMENT = "parallel";
  String PARALLEL_STEP_ELEMENT = "parallel";
  String STEP_ELEMENT = "step";
  String STEP_GROUP = "stepGroup";
  String NG_VARIABLES = "variables";
  String PATH_CONNECTOR = VisitorParentPathUtils.PATH_CONNECTOR;
}

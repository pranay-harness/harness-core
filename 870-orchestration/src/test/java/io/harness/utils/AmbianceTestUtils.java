package io.harness.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;

public class AmbianceTestUtils {
  public static final String ACCOUNT_ID = generateUuid();
  public static final String APP_ID = generateUuid();
  public static final String EXECUTION_INSTANCE_ID = generateUuid();
  public static final String PHASE_RUNTIME_ID = generateUuid();
  public static final String PHASE_SETUP_ID = generateUuid();
  public static final String SECTION_RUNTIME_ID = generateUuid();
  public static final String SECTION_SETUP_ID = generateUuid();

  public static Ambiance buildAmbiance() {
    Level phaseLevel = Level.newBuilder()
                           .setRuntimeId(PHASE_RUNTIME_ID)
                           .setSetupId(PHASE_SETUP_ID)
                           .setStepType(StepType.newBuilder().setType("DEPLOY_PHASE").build())
                           .setGroup("PHASE")
                           .build();
    Level sectionLevel = Level.newBuilder()
                             .setRuntimeId(SECTION_RUNTIME_ID)
                             .setSetupId(SECTION_SETUP_ID)
                             .setStepType(StepType.newBuilder().setType("DEPLOY_SECTION").build())
                             .setGroup("SECTION")
                             .build();
    List<Level> levels = new ArrayList<>();
    levels.add(phaseLevel);
    levels.add(sectionLevel);
    return Ambiance.newBuilder()
        .setPlanExecutionId(EXECUTION_INSTANCE_ID)
        .putAllSetupAbstractions(ImmutableMap.of("accountId", ACCOUNT_ID, "appId", APP_ID))
        .addAllLevels(levels)
        .build();
  }
}

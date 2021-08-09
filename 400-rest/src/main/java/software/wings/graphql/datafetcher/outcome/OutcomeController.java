package software.wings.graphql.datafetcher.outcome;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class OutcomeController {}

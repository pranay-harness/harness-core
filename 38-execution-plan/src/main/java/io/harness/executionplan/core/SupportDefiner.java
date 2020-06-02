package io.harness.executionplan.core;

import java.util.List;

/**
 *  define the support definition for a plan creator.
 *  registry lookup for plan creators would match if type is in supported types and supports method returns true
 *  this gives flexibility to provide dynamic matching criteria instead of only static type based strategy
 */
public interface SupportDefiner {
  boolean supports(PlanCreatorSearchContext<?> searchContext);
  List<String> getSupportedTypes();
}

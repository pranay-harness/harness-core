package io.harness.yaml.core.auxiliary.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This is the base interface to define the stage type
 *
 * wrapper object for stage element.
 * stages:
 *  - stage:
 *      identifier:
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StageElement.class, name = "stage")
  , @JsonSubTypes.Type(value = ParallelStageElement.class, name = "parallel")
})
public interface StageElementWrapper {}

package io.harness.yaml.core.auxiliary.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * wrapper object for phase element.
 * phases:
 *      - phase:
 *              identifier:
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface PhaseWrapper {}

package io.harness.yaml.core.intfc;

import javax.validation.constraints.NotNull;

/**
 *  Any class that has type property should implement this interface
 *  This is main source of polymorphic type information.
 */
public interface WithType { @NotNull String getType(); }

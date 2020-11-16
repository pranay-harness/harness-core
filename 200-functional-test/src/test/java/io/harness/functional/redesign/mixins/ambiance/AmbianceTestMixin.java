package io.harness.functional.redesign.mixins.ambiance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = AmbianceTestDeserializer.class)
public abstract class AmbianceTestMixin {}

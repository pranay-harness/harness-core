package io.harness.functional.redesign.mixins.advisertype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = AdviserTypeTestDeserializer.class)
public abstract class AdviserTypeTestMixin {}

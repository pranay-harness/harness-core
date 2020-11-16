package io.harness.functional.redesign.mixins.adviserobtainment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = AdviserObtainmentTestDeserializer.class)
public abstract class AdviserObtainmentTestMixin {}

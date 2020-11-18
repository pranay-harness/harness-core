package io.harness.functional.redesign.mixins.reftype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = RefTypeTestDeserializer.class)
public abstract class RefTypeTestMixin {}

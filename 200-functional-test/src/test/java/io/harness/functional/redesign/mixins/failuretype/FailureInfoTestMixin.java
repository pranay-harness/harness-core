package io.harness.functional.redesign.mixins.failuretype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = FailureInfoTestDeserializer.class)
public abstract class FailureInfoTestMixin {}

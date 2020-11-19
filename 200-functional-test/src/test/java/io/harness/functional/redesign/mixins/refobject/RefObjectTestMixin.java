package io.harness.functional.redesign.mixins.refobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = RefObjectTestDeserializer.class)
public abstract class RefObjectTestMixin {}

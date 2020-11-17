package io.harness.functional.redesign.mixins.facilitatortype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = FacilitatorTypeTestDeserializer.class)
public abstract class FacilitatorTypeTestMixin {}

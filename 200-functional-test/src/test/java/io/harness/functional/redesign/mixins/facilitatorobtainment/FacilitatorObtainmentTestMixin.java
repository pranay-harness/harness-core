package io.harness.functional.redesign.mixins.facilitatorobtainment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = FacilitatorObtainmentTestDeserializer.class)
public abstract class FacilitatorObtainmentTestMixin {}

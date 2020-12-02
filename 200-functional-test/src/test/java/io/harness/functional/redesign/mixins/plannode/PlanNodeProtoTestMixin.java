package io.harness.functional.redesign.mixins.plannode;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = PlanNodeProtoTestDeserializer.class)
public abstract class PlanNodeProtoTestMixin {}

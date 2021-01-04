package io.harness.functional.redesign.mixins.executionmetadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ExecutionMetadataTestDeserializer.class)
public abstract class ExecutionMetadataTestMixin {}

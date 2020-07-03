package io.harness.annotations;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.BindingAnnotation;

import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@OwnedBy(CDC)
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface Redesign {}

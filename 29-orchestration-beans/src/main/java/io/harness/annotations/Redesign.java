package io.harness.annotations;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@OwnedBy(CDC)
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface Redesign {}

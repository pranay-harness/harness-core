package io.harness.gitopsprovider;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import static java.lang.annotation.ElementType.FIELD;

import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@OwnedBy(GITOPS)
public @interface SearchTerm {}

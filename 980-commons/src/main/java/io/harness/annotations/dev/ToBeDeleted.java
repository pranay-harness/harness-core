package io.harness.annotations.dev;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@OwnedBy(HarnessTeam.PIPELINE)
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ToBeDeleted {}

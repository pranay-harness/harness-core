package software.wings.service.intfc.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marker interface to specify Guice binding
 */
@OwnedBy(PL)
@BindingAnnotation
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface RateLimitCheck {}

package io.harness.pcf.command.option;

import io.harness.pcf.model.PcfCliVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedVersions {
  PcfCliVersion[] values();
}

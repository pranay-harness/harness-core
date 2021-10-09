package io.harness.account.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by peeyushaggarwal on 3/22/17.
 *
 * Moved from 400-rest due to aeriform check. No usages for the licenseKey in CG or NG.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@OwnedBy(HarnessTeam.GTM)
public @interface LicenseKey {}

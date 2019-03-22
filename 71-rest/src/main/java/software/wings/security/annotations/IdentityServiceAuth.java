package software.wings.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Auth Rule will apply to APIs in manager that are only exposed to Identity Service. These APIs are needed
 * for Identity Service to be able to finish the authentication process or for accountId based routing.
 *
 * @author marklu on 2019-03-21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface IdentityServiceAuth {}

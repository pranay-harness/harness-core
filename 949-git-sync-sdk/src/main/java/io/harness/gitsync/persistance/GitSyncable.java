package io.harness.gitsync.persistance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@NameBinding
public @interface GitSyncable {}

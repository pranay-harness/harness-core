package io.harness.ng.core.remote;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    String name;
    boolean hasHarnessAccount;
    boolean hasSlackAccount;
}
package software.wings.app;

/**
 * Enum to identify the deploymode. Hazelcast , delegate may use this enum for their custom behaviors in respective
 * modes
 */
public enum DeployMode { AWS, ONPREM, KUBERNETES, KUBERNETES_ONPREM }

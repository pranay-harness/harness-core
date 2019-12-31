package io.harness.perpetualtask.k8s.cronjobs.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.client.CustomResource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CronJob extends CustomResource {}

package io.harness.perpetualtask;

/**
 * Used on the manager side to handle CRUD of a specific type of perpetual tasks.
 * @param <T> The params type of the perpetual task type being managed.
 */
public interface PerpetualTaskServiceInprocClient<T extends PerpetualTaskClientParams> {
  String create(String accountId, T clientParams);
}

package io.harness.feature.bases;

import io.harness.feature.interfaces.Feature;
import io.harness.feature.interfaces.SlidingWindow;

public abstract class RateLimit<T> implements Feature<T>, SlidingWindow<T> {}

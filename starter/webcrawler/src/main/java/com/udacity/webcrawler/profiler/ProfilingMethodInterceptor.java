package com.udacity.webcrawler.profiler;

        import java.lang.reflect.InvocationHandler;
        import java.lang.reflect.InvocationTargetException;
        import java.lang.reflect.Method;
        import java.time.Clock;
        import java.time.Duration;
        import java.time.Instant;
        import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Check if the method is annotated with @Profiled.
    if (method.getAnnotation(Profiled.class) == null) {
      // If not, just invoke the original method on the delegate and return.
      return method.invoke(delegate, args);
    }

    // If the method is profiled, start timing
    Instant startTime = clock.instant();
    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      // exception handling
      throw e.getTargetException();
    } finally {
      // 'finally' block ensuring that the timing is recorded even if the method throws an exception
      Instant endTime = clock.instant();
      Duration duration = Duration.between(startTime, endTime);
      state.record(delegate.getClass(), method, duration);
    }
  }
}
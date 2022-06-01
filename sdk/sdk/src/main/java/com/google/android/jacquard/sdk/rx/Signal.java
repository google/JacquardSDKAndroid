/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.rx;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A naive reimplementation of rxjava's Observable&lt;T&gt;
 *
 * <p>Produces an observable stream of T. All operations are single-threaded, but should otherwise
 * be thread-safe.
 *
 * @param <T> the type of object emitted by this signal
 */
public class Signal<T> {

  final SubscriptionFactory<T> subscriptionFactory;
  private final List<Observer<? super T>> observers = new CopyOnWriteArrayList<>();
  private boolean completed = false;
  private boolean errored = false;
  private Throwable ex = null;

  private Signal(@NonNull final SubscriptionFactory<T> sf) {
    subscriptionFactory = sf;
  }

  /**
   * Signal factory method
   */
  public static <T> Signal<T> create() {
    return new Signal<>(Signal.noopFactory());
  }

  /**
   * signal factory method
   */
  public static <T> Signal<T> create(SubscriptionFactory<T> sf) {
    return new Signal<>(sf);
  }

  /**
   * empty signal factory
   */
  @NonNull
  public static <T> Signal<T> empty() {
    Signal<T> e = Signal.create();
    e.complete();
    return e;
  }

  /**
   * errored signal factory
   */
  @NonNull
  public static <T> Signal<T> empty(@NonNull final Throwable error) {
    Signal<T> e = Signal.create();
    e.error(error);
    return e;
  }

  /**
   * create Signal[T] from Iterable[T]
   */
  @NonNull
  public static <T> Signal<T> from(@NonNull Iterable<T> xs) {
    return new CannedSignal<>(xs);
  }

  /**
   * create Signal[T] from a T
   */
  @NonNull
  public static <T> Signal<T> from(@NonNull T t) {
    return from(Collections.singletonList(t));
  }

  /**
   * create Signal[T] from a T (alias to allow Collections)
   */
  @NonNull
  public static <T> Signal<T> just(@NonNull T t) {
    return from(t);
  }

  /**
   * combine multiple signals of the same type into one
   */
  @NonNull
  public static <T> Signal<T> merge(
      @NonNull Signal<? extends T> signal1, @NonNull Signal<? extends T> signal2) {
    return new MergedSignal<>(signal1, signal2);
  }

  /**
   * merge a list of signals of the same type into one
   */
  @NonNull
  public static <T> Signal<T> merge(@NonNull Iterable<Signal<T>> signals) {
    Signal<T> last = Signal.empty();
    for (Signal<T> sig : signals) {
      last = Signal.merge(last, sig);
    }
    return last;
  }

  private static <T> SubscriptionFactory<T> noopFactory() {
    return signal -> new Subscription();
  }

  public boolean hasObservers() {
    return !observers.isEmpty();
  }

  /**
   * error out this signal
   */
  public synchronized void error(@NonNull Throwable t) {
    for (Observer<? super T> obs : observers) {
      obs.onError(t);
    }
    observers.clear();
    ex = t;
    errored = true;
  }

  /**
   * provide a new value to this signal
   */
  public void next(@NonNull T t) {
    for (Observer<? super T> obs : observers) {
      obs.onNext(t);
    }
  }

  public synchronized boolean hasError() {
    return errored;
  }

  public synchronized boolean isComplete() {
    return !errored && completed;
  }

  /**
   * mark this signal as being completed
   */
  public synchronized void complete() {
    for (Observer<? super T> obs : observers) {
      obs.onComplete();
    }
    observers.clear();
    completed = true;
  }

  /**
   * transform Signal[T] to Signal[U] using T -> U
   */
  @NonNull
  public <U> Signal<U> map(@NonNull final Fn<? super T, ? extends U> f) {
    return new MappedSignal<>(this, f);
  }

  /**
   * transform Signal[T] to Signal[U] using T -> Signal[U]
   */
  @NonNull
  public <U> Signal<U> flatMap(@NonNull final Fn<? super T, ? extends Signal<? extends U>> f) {
    return new FMappedSignal<>(this, f);
  }

  /**
   * transform Signal[T] to Signal[U] using T -> Signal[U] and complete previous inner observable
   * before emitting.
   */
  @NonNull
  public <U> Signal<U> switchMap(@NonNull final Fn<? super T, ? extends Signal<? extends U>> f) {
    return new SwitchMappedSignal<>(this, f);
  }
  
  /**
   * remove elements from this Signal that do not match the predicate
   */
  @NonNull
  public Signal<T> filter(@NonNull final Pred<? super T> f) {
    return new FilteredSignal<>(this, f);
  }

  /**
   * retrieve the beginning of this signal while the predicate is true
   */
  @NonNull
  public Signal<T> takeWhile(@NonNull final Pred<? super T> f) {
    return new TakeWhileSignal<>(this, f);
  }

  /**
   * retrieve the first count items from this signal
   */
  @NonNull
  public Signal<T> take(final int count) {
    return new TakeSignal<>(this, count);
  }

  /**
   * retrieve the first element of this signal
   */
  @NonNull
  public Signal<T> first() {
    return take(1);
  }

  /**
   * ignore the first count items from this signal
   */
  @NonNull
  public Signal<T> drop(final int count) {
    return new DropWhileSignal<>(
        this,
        new Pred<T>() {
          private int x = 0;

          @Override
          public boolean apply(T t) {
            return x++ < count;
          }
        });
  }

  /**
   * ignore the first items of this signal while the predicate holds
   */
  @NonNull
  public Signal<T> dropWhile(@NonNull final Pred<? super T> f) {
    return new DropWhileSignal<>(this, f);
  }

  /**
   * @return a new signal that only returns unique items
   */
  @NonNull
  public Signal<T> distinct() {
    return new DistinctSignal<>(this);
  }

  /**
   * @return a new signal that ignores duplicate consecutive emissions.
   */
  @NonNull
  public Signal<T> distinctUntilChanged() {
    return new DistinctUntilChangedSignal<>(this);
  }

  /**
   * @return a signal that remembers its last value and emits it as the first value on subscribe
   */
  @NonNull
  public Signal<T> sticky() {
    return new StickySignal<>(this);
  }

  /**
   * retrieve the last item of this signal
   */
  @NonNull
  public Signal<T> last() {
    return new LastSignal<>(this);
  }

  /**
   * collate this signal into an accumulator
   */
  @NonNull
  public <U> Signal<U> scan(U initial, Fn2<? super U, ? super T, ? extends U> f) {
    return new ScanSignal<>(this, initial, f);
  }

  /**
   * share any previous operations across subsequent observers
   */
  @NonNull
  public Signal<T> shared() {
    return new SharedSignal<>(this);
  }

  /**
   * timeout this signal after timeoms milliseconds
   */
  @NonNull
  public Signal<T> timeout(long timeoms) {
    return new TimeoutSignal<>(this, timeoms, null);
  }

  /**
   * timeout this signal after timeoms milliseconds
   */
  @NonNull
  public Signal<T> timeout(long timeoms, @Nullable String loggingPayload) {
    return new TimeoutSignal<>(this, timeoms, loggingPayload);
  }

  /**
   * Recovers this Signal if it encounters an error by returning a new Signal.
   *
   * <p>The exception can be inspected to either return a new signal, or another error if it
   * should not be handled.
   */
  @NonNull
  public Signal<T> recoverWith(Fn<Throwable, Signal<T>> recoveryF) {
    return new RecoverWithSignal<>(this, recoveryF);
  }

  /**
   * delay all outputs from this signal by
   *
   * <p>delayms
   */
  @NonNull
  public Signal<T> delay(long delayms) {
    return new DelaySignal<>(this, delayms);
  }

  /**
   * Schedule a countdown until a time in the future, with regular notifications on intervals along
   * the way.
   *
   * @param millisInFuture Millis since epoch when alarm should stop.
   * @param countDownInterval The interval in millis that the user receives callbacks.
   */
  @NonNull
  public Signal<T> countDownTimer(long millisInFuture, long countDownInterval) {
    return new CountDownTimerSignal<>(this, millisInFuture, countDownInterval);
  }

  /**
   * Forwards errors to the designated callback without causing subscribe side-effects
   */
  @NonNull
  public Signal<T> tapError(Consumer<Throwable> consumer) {
    return new TapErrorSignal<>(this, consumer);
  }

  /**
   * Forwards all events from this Signal to the target signal
   */
  public final Subscription forward(Signal<T> signal) {
    return observe(new ForwarderObserver<T>(signal));
  }

  /**
   * Forwards all next values to the specified consumer for a side-effecting operation.
   */
  @NonNull
  public Signal<T> tap(Consumer<T> consumer) {
    return new TapSignal<>(this, consumer);
  }

  /**
   * Forwards completion to the specified consumer for a side-effecting operation.
   */
  @NonNull
  public Signal<T> tapCompletion(Runnable consumer) {
    return new TapCompleteSignal<>(this, consumer);
  }

  /**
   * Executes downstream operators on the provided {@link Executor}.
   */
  @NonNull
  public Signal<T> observeOn(@NonNull Executor executor) {
    return new ObserveOnSignal<>(this, executor);
  }

  public final Subscription onNext(@NonNull final Consumer<? super T> consumer) {
    return observe(new ObservesNext<T>() {
      @Override
      public void onNext(T next) {
        consumer.apply(next);
      }
    });
  }

  public final Subscription onComplete(@NonNull final Runnable completeHandler) {
    return observe(new ObservesComplete<T>() {
      @Override
      public void onComplete() {
        completeHandler.run();
      }
    });
  }

  public final Subscription onError(@NonNull final Consumer<? super Throwable> consumer) {
    return observe(new ObservesError() {
      @Override
      public void onError(Throwable error) {
        consumer.apply(error);
      }
    });
  }

  /**
   * Subscribes to onError and onComplete of this signal.
   *
   * <p><code>consumer</code> will be called with a <code>null</code> {@link Throwable} if this
   * has completed successfully, if there is an error, it will be non-<code>null</code>.
   */
  public final Subscription onTerminate(@NonNull final Consumer<? super Throwable> consumer) {
    return observe(new Observer<T>() {
      @Override
      public void onNext(T next) {
        // noop
      }

      @Override
      public void onComplete() {
        consumer.apply(null);
      }

      @Override
      public void onError(Throwable error) {
        consumer.apply(error);
      }
    });
  }

  /**
   * Observes this signal using the specified <code>onNext</code> and <code>onTerminate</code>.
   *
   * @param onNext will be called for every value on this signal
   * @param onTerminate will receive a <code>null</code> when this completes or a
   * non-<code>null</code> {@link Throwable} when an error is encountered
   */
  public final Subscription observe(
      @NonNull final Consumer<? super T> onNext,
      @NonNull Consumer<? super Throwable> onTerminate) {
    return observe(new Observer<T>() {
      @Override
      public void onNext(T next) {
        onNext.apply(next);
      }

      @Override
      public void onComplete() {
        onTerminate.apply(null);
      }

      @Override
      public void onError(Throwable error) {
        onTerminate.apply(error);
      }
    });
  }

  /**
   * observe events on this signal, unsubscribe the Subscription to stop observing
   */
  @NonNull
  public final Subscription observe(@NonNull final Observer<? super T> obs) {
    if (!completed && !errored) {
      observers.add(obs); // as a result of this operation order, hasObservers is always true within
      // onNewSubscription
      final Subscription wrapped = subscriptionFactory.onSubscribe(this);
      Subscription sub =
          new Subscription() {
            @Override
            public void onUnsubscribe() {
              observers.remove(obs);
              wrapped.unsubscribe();
            }
          };
      if (obs instanceof OnSubscribe) {
        ((OnSubscribe) obs).onSubscribe(sub);
      }
      return sub;
    } else if (completed) {
      obs.onComplete();
      return new Subscription();
    } else {
      obs.onError(ex);
      return new Subscription();
    }
  }

  /**
   * convenience method to observe all values on this Signal and ignore them, causes side-effects to
   * run.
   */
  @NonNull
  public final Subscription consume() {
    return observe(
        new ObservesComplete<T>() {
          @Override
          public void onComplete() {
          }
        });
  }

  public interface SubscriptionFactory<T> {

    @NonNull
    Subscription onSubscribe(@NonNull Signal<T> signal);
  }

  public interface Observer<T> {

    void onNext(@NonNull T t);

    void onError(@NonNull Throwable t);

    void onComplete();
  }

  public interface OnSubscribe {

    void onSubscribe(Subscription subscription);
  }

  static class SharedSignal<T> extends Signal<T> {

    SharedSignal(@NonNull Signal<T> source) {
      super(new SharedSubscriptionFactory<>(source));
    }

    static class SharedSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private int subCount = 0;
      private Subscription outerSub;

      SharedSubscriptionFactory(@NonNull Signal<T> source) {
        this.source = source;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(@NonNull final Signal<T> signal) {
        if (subCount++ == 0 && outerSub == null) {
          outerSub =
              source.observe(
                  new Observer<T>() {
                    @Override
                    public void onNext(@NonNull T t) {
                      signal.next(t);
                    }

                    @Override
                    public void onError(@NonNull Throwable t) {
                      signal.error(t);
                    }

                    @Override
                    public void onComplete() {
                      signal.complete();
                    }
                  });
        }
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            subCount = Math.max(0, subCount - 1);
            if (subCount == 0 && outerSub != null) {
              outerSub.unsubscribe();
              outerSub = null;
            }
          }
        };
      }
    }
  }

  static class MappedSignal<U, T> extends Signal<U> {

    MappedSignal(final @NonNull Signal<T> sig, final @NonNull Fn<? super T, ? extends U> f) {
      super(new MappedSubscriptionFactory<>(sig, f));
    }

    private static class MappedSubscriptionFactory<U, T> implements SubscriptionFactory<U> {

      private final Signal<T> sig;
      private final Fn<? super T, ? extends U> f;

      MappedSubscriptionFactory(Signal<T> sig, Fn<? super T, ? extends U> f) {
        this.sig = sig;
        this.f = f;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<U> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    try {
                      signal.next(f.apply(t));
                    } catch (Exception e) {
                      signal.error(e);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class ScanSignal<U, T> extends Signal<U> {

    ScanSignal(
        final @NonNull Signal<T> sig,
        U initial,
        final @NonNull Fn2<? super U, ? super T, ? extends U> f) {
      super(new ScanSubscriptionFactory<>(sig, initial, f));
    }

    private static class ScanSubscriptionFactory<U, T> implements SubscriptionFactory<U> {

      private final Signal<T> sig;
      private final Fn2<? super U, ? super T, ? extends U> f;
      private U accum;

      ScanSubscriptionFactory(Signal<T> sig, U initial, Fn2<? super U, ? super T, ? extends U> f) {
        this.sig = sig;
        this.f = f;
        accum = initial;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<U> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    try {
                      accum = f.apply(accum, t);
                      signal.next(accum);
                    } catch (Exception e) {
                      signal.error(e);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class FilteredSignal<T> extends Signal<T> {

    FilteredSignal(final @NonNull Signal<T> sig, final @NonNull Pred<? super T> f) {
      super(new FilteredSubscriptionFactory<>(sig, f));
    }

    private static class FilteredSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final Pred<? super T> f;

      FilteredSubscriptionFactory(Signal<T> sig, Pred<? super T> f) {
        this.sig = sig;
        this.f = f;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    try {
                      if (f.apply(t)) {
                        signal.next(t);
                      }
                    } catch (Exception e) {
                      signal.error(e);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class TakeSignal<T> extends Signal<T> {

    TakeSignal(final @NonNull Signal<T> sig, final int count) {
      super(new TakeSubscriptionFactory<>(sig, count));
    }

    private static class TakeSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final int count;

      TakeSubscriptionFactory(Signal<T> sig, int count) {
        this.sig = sig;
        this.count = count;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  int current = 0;

                  @Override
                  public void onNext(@NonNull T t) {
                    if (current++ < count) {
                      signal.next(t);
                    }
                    if (current == count) {
                      signal.complete();
                      getSubscription().unsubscribe();
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class TakeWhileSignal<T> extends Signal<T> {

    TakeWhileSignal(final @NonNull Signal<T> sig, final @NonNull Pred<? super T> f) {
      super(new TakeWhileSubscriptionFactory<>(sig, f));
    }

    private static class TakeWhileSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final Pred<? super T> f;
      private boolean noMore = false;

      TakeWhileSubscriptionFactory(Signal<T> sig, Pred<? super T> f) {
        this.sig = sig;
        this.f = f;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    if (!noMore && f.apply(t)) {
                      signal.next(t);
                    } else {
                      if (!noMore) {
                        signal.complete();
                        getSubscription().unsubscribe();
                      }
                      noMore = true;
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class DropWhileSignal<T> extends Signal<T> {

    DropWhileSignal(final @NonNull Signal<T> sig, final @NonNull Pred<? super T> f) {
      super(new DropWhileSubscriptionFactory<>(sig, f));
    }

    private static class DropWhileSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final Pred<? super T> f;
      private boolean noMore = false;

      DropWhileSubscriptionFactory(Signal<T> sig, Pred<? super T> f) {
        this.sig = sig;
        this.f = f;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    if (noMore || !f.apply(t)) {
                      noMore = true;
                      signal.next(t);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class FMappedSignal<U, T> extends Signal<U> {

    FMappedSignal(
        final @NonNull Signal<T> sig,
        final @NonNull Fn<? super T, ? extends Signal<? extends U>> f) {
      super(new FMappedSubscriptionFactory<>(sig, f));
    }

    private static class FMappedSubscriptionFactory<U, T> implements SubscriptionFactory<U> {

      private final Signal<T> sig;
      private final Fn<? super T, ? extends Signal<? extends U>> f;
      private boolean hasCompleted;

      FMappedSubscriptionFactory(Signal<T> sig, Fn<? super T, ? extends Signal<? extends U>> f) {
        this.sig = sig;
        this.f = f;
      }

      private synchronized void executeNext(
          Signal<U> signal,
          T next,
          ArrayDeque<T> queue,
          AtomicBoolean inFlight,
          Subscription outer) {
        if (inFlight.get()) {
          queue.add(next);
          return;
        }
        inFlight.set(true);
        f.apply(next)
            .observe(
                new Observer<U>() {
                  @Override
                  public void onNext(@NonNull U u) {
                    signal.next(u);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    outer.unsubscribe();
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    inFlight.set(false);
                    T after = queue.poll();

                    if (hasCompleted && after == null) {
                      signal.complete();
                      outer.unsubscribe();
                    }
                    if (after != null) {
                      executeNext(signal, after, queue, inFlight, outer);
                    }
                  }
                });
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<U> signal) {
        // queue of items to wait on processing while a child is inFlight
        final ArrayDeque<T> nextQueue = new ArrayDeque<>();
        // indicates whether a child is processing and should cause new items to be queued
        final AtomicBoolean inFlight = new AtomicBoolean(false);

        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    try {
                      executeNext(signal, t, nextQueue, inFlight, getSubscription());
                    } catch (Exception e) {
                      signal.error(e);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    if (!inFlight.get() && nextQueue.isEmpty()) {
                      signal.complete();
                      getSubscription().unsubscribe();
                    }
                    hasCompleted = true;
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class SwitchMappedSignal<U, T> extends Signal<U> {

    SwitchMappedSignal(
        final @NonNull Signal<T> sig,
        final @NonNull Fn<? super T, ? extends Signal<? extends U>> f) {
      super(new SwitchMapSubscriptionFactory<>(sig, f));
    }

    private static class SwitchMapSubscriptionFactory<U, T> implements SubscriptionFactory<U> {

      private final Signal<T> sig;
      private final Fn<? super T, ? extends Signal<? extends U>> f;
      private Subscription innerSubscription = new Subscription();

      public SwitchMapSubscriptionFactory(
          Signal<T> sig, Fn<? super T, ? extends Signal<? extends U>> f) {
        this.sig = sig;
        this.f = f;
      }

      private synchronized void executeNext(Signal<U> signal, T next) {
        innerSubscription.unsubscribe();
        innerSubscription =
            f.apply(next)
                .observe(
                    new Observer<U>() {
                      @Override
                      public void onNext(@NonNull U u) {
                        signal.next(u);
                      }

                      @Override
                      public void onError(@NonNull Throwable t) {
                        signal.error(t);
                      }

                      @Override
                      public void onComplete() {
                        signal.complete();
                      }
                    });
      }

      @NonNull
      @Override
      public Subscription onSubscribe(@NonNull Signal<U> signal) {
        return sig.observe(
            new Observer<T>() {
              @Override
              public void onNext(@NonNull T t) {
                executeNext(signal, t);
              }

              @Override
              public void onError(@NonNull Throwable t) {
                signal.error(t);
              }

              @Override
              public void onComplete() {
                signal.complete();
              }
            });
      }
    }
  }

    static class CannedSignal<T> extends Signal<T> {

    CannedSignal(Iterable<T> data) {
      super(new CannedSubscriptionFactory<>(data));
    }

    static class CannedSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Iterable<T> data;

      CannedSubscriptionFactory(final Iterable<T> data) {
        this.data = data;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(@NonNull Signal<T> signal) {
        for (T t : data) {
          signal.next(t);
        }
        signal.complete();
        return new Subscription();
      }
    }
  }

  static class MergedSignal<T> extends Signal<T> {

    MergedSignal(Signal<? extends T> sig1, Signal<? extends T> sig2) {
      super(new MergedSubscriptionFactory<>(sig1, sig2));
    }

    static class MergedSubscriptionFactory<T> implements SubscriptionFactory<T> {

      final Signal<? extends T> s1;
      final Signal<? extends T> s2;

      MergedSubscriptionFactory(Signal<? extends T> sig1, Signal<? extends T> sig2) {
        s1 = sig1;
        s2 = sig2;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final boolean[] completeOnce = {false};
        Observer<T> ob =
            new SubscribeObserver<T>() {
              @Override
              public void onNext(@NonNull T t) {
                signal.next(t);
              }

              @Override
              public void onError(@NonNull Throwable t) {
                signal.error(t);
              }

              @Override
              public void onComplete() {
                if (completeOnce[0]) {
                  signal.complete();
                  getSubscription().unsubscribe();
                }
                completeOnce[0] = true;
              }
            };
        final Subscription sub1 = s1.observe(ob);
        final Subscription sub2 = s2.observe(ob);
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            sub1.unsubscribe();
            sub2.unsubscribe();
          }
        };
      }
    }
  }

  static class TimeoutSignal<T> extends Signal<T> {

    TimeoutSignal(@NonNull Signal<T> source, long timeo, @Nullable String loggingPayload) {
      super(new TimeoutSubscriptionFactory<>(source, timeo, loggingPayload));
    }

    static class TimeoutSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final long timeo;
      private final TimeoutException ex;

      TimeoutSubscriptionFactory(Signal<T> source, long timeo, @Nullable String loggingPayload) {
        ex = new TimeoutException(String.format("Timeout after %dms%s", timeo,
            loggingPayload == null
                ? ""
                : (": " + loggingPayload)));
        this.source = source;
        this.timeo = timeo;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable timeout = () -> signal.error(ex);
        h.postDelayed(timeout, timeo);
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    h.removeCallbacks(timeout);
                    h.postDelayed(timeout, timeo);
                    signal.next(t);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    h.removeCallbacks(timeout);
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    h.removeCallbacks(timeout);
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            super.onUnsubscribe();
            h.removeCallbacks(timeout);
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class RecoverWithSignal<T> extends Signal<T> {

    RecoverWithSignal(@NonNull Signal<T> source, @NonNull Fn<Throwable, Signal<T>> recoveryF) {
      super(new RecoverWithSubscriptionFactory<>(source, recoveryF));
    }

    static class RecoverWithSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final Fn<Throwable, Signal<T>> recoveryF;

      RecoverWithSubscriptionFactory(Signal<T> source, Fn<Throwable, Signal<T>> recoveryF) {
        this.source = source;
        this.recoveryF = recoveryF;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    signal.next(t);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    subscription.set(recoveryF.apply(t).forward(signal));
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            super.onUnsubscribe();
            Subscription sub = subscription.get();
            if (sub != null) {
              sub.unsubscribe();
            }
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class DelaySignal<T> extends Signal<T> {

    DelaySignal(@NonNull Signal<T> source, long timeo) {
      super(new DelaySubscriptionFactory<>(source, timeo));
    }

    static class DelaySubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final long delay;

      DelaySubscriptionFactory(Signal<T> source, long delay) {
        this.source = source;
        this.delay = delay;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Handler h = new Handler(Looper.getMainLooper());
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    h.postDelayed(() -> signal.next(t), delay);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    h.postDelayed(() -> signal.error(t), delay);
                  }

                  @Override
                  public void onComplete() {
                    h.postDelayed(signal::complete, delay);
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            super.onUnsubscribe();
            h.removeCallbacksAndMessages(null);
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class CountDownTimerSignal<T> extends Signal<T> {

    CountDownTimerSignal(@NonNull Signal<T> source, long millisInFuture, long countDownInterval) {
      super(new CountDownTimerSubscriptionFactory<>(source, millisInFuture, countDownInterval));
    }

    static class CountDownTimerSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final long millisInFuture;
      private final long countDownInterval;

      CountDownTimerSubscriptionFactory(
          Signal<T> source, long millisInFuture, long countDownInterval) {
        this.source = source;
        this.millisInFuture = millisInFuture;
        this.countDownInterval = countDownInterval;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final CountDownTimer[] timer = new CountDownTimer[1];
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    timer[0] =
                        new CountDownTimer(millisInFuture, countDownInterval) {
                          @Override
                          public void onTick(long millisUntilFinished) {
                            @SuppressWarnings("unchecked") final T castT = (T) Long
                                .valueOf(millisUntilFinished);
                            signal.next(castT);
                          }

                          @Override
                          public void onFinish() {
                            signal.complete();
                          }
                        };
                    timer[0].start();
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            super.onUnsubscribe();
            CountDownTimer countDownTimer = timer[0];
            if (countDownTimer != null) {
              countDownTimer.cancel();
            }
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class TapSignal<T> extends Signal<T> {

    TapSignal(@NonNull Signal<T> source, Consumer<T> consumer) {
      super(new TapSubscriptionFactory<>(source, consumer));
    }

    static class TapSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final Consumer<T> consumer;

      TapSubscriptionFactory(Signal<T> source, Consumer<T> consumer) {
        this.source = source;
        this.consumer = consumer;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    consumer.apply(t);
                    signal.next(t);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class TapCompleteSignal<T> extends Signal<T> {

    TapCompleteSignal(@NonNull Signal<T> source, Runnable consumer) {
      super(new TapCompleteSubscriptionFactory<>(source, consumer));
    }

    static class TapCompleteSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final Runnable consumer;

      TapCompleteSubscriptionFactory(Signal<T> source, Runnable consumer) {
        this.source = source;
        this.consumer = consumer;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    signal.next(t);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    consumer.run();
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class TapErrorSignal<T> extends Signal<T> {

    TapErrorSignal(@NonNull Signal<T> source, Consumer<Throwable> consumer) {
      super(new TapErrorSubscriptionFactory<>(source, consumer));
    }

    static class TapErrorSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> source;
      private final Consumer<Throwable> consumer;

      TapErrorSubscriptionFactory(Signal<T> source, Consumer<Throwable> consumer) {
        this.source = source;
        this.consumer = consumer;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription s =
            source.observe(
                new Observer<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    signal.next(t);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    consumer.apply(t);
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                  }
                });
        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            s.unsubscribe();
          }
        };
      }
    }
  }

  static class DistinctSignal<T> extends Signal<T> {

    DistinctSignal(@NonNull Signal<T> source) {
      super(new DistinctSubscriptionFactory<>(source));
    }

    private static class DistinctSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;

      DistinctSubscriptionFactory(Signal<T> sig) {
        this.sig = sig;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Set<T> seen = new HashSet<>();
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    if (!seen.contains(t)) {
                      seen.add(t);
                      signal.next(t);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class DistinctUntilChangedSignal<T> extends Signal<T> {

    DistinctUntilChangedSignal(@NonNull Signal<T> source) {
      super(new DistinctUntilChangedSubscriptionFactory<>(source));
    }

    private static class DistinctUntilChangedSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;

      DistinctUntilChangedSubscriptionFactory(Signal<T> sig) {
        this.sig = sig;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final int[] hashcode = {0};
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    if(hashcode[0] != t.hashCode()) {
                      hashcode[0] = t.hashCode();
                      signal.next(t);
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    signal.error(t);
                  }

                  @Override
                  public void onComplete() {
                    signal.complete();
                    getSubscription().unsubscribe();
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  static class StickySignal<T> extends Signal<T> {

    private final Object[] lastData;

    StickySignal(@NonNull Signal<T> source) {
      super(new StickySubscriptionFactory<>());
      lastData = ((StickySubscriptionFactory<T>) this.subscriptionFactory).last;
      source.observe(
          new SubscribeObserver<T>() {
            @Override
            public void onNext(@NonNull T t) {
              lastData[0] = t;
              next(t);
            }

            @Override
            public void onError(@NonNull Throwable t) {
              error(t);
            }

            @Override
            public void onComplete() {
              // complete();
            }
          });
    }

    @Override
    public void next(@NonNull T t) {
      lastData[0] = t;
      super.next(t);
    }

    private static class StickySubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Object[] last = {null};

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        @SuppressWarnings("unchecked") final T castT = (T) last[0];
        if (castT != null) {
          signal.next(castT); // side-effect: this gets broadcast to all current subscribers as well
        }
        return new Subscription();
      }
    }
  }

  static class LastSignal<T> extends Signal<T> {

    LastSignal(@NonNull Signal<T> source) {
      super(new LastSubscriptionFactory<>(source));
    }

    private static class LastSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final Object[] last = {null};

      LastSubscriptionFactory(Signal<T> sig) {
        this.sig = sig;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        return sig.observe(
            new SubscribeObserver<T>() {
              @Override
              public void onNext(@NonNull T t) {
                last[0] = t;
              }

              @Override
              public void onError(@NonNull Throwable t) {
                signal.error(t);
              }

              @Override
              public void onComplete() {
                @SuppressWarnings("unchecked") final T castT = (T) last[0];
                if (castT != null) {
                  signal.next(castT);
                }
                signal.complete();
                getSubscription().unsubscribe();
              }
            });
      }
    }
  }

  static class ObserveOnSignal<T> extends Signal<T> {

    ObserveOnSignal(final @NonNull Signal<T> sig, final @NonNull Executor executor) {
      super(new ObserveOnSignalSubscriptionFactory<>(sig, executor));
    }

    private static class ObserveOnSignalSubscriptionFactory<T> implements SubscriptionFactory<T> {

      private final Signal<T> sig;
      private final Executor executor;

      ObserveOnSignalSubscriptionFactory(@NonNull Signal<T> sig, @NonNull Executor executor) {
        this.sig = sig;
        this.executor = executor;
      }

      @NonNull
      @Override
      public Subscription onSubscribe(final @NonNull Signal<T> signal) {
        final Subscription outer =
            sig.observe(
                new SubscribeObserver<T>() {
                  @Override
                  public void onNext(@NonNull T t) {
                    executor.execute(() -> signal.next(t));
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    executor.execute(() -> signal.error(t));
                  }

                  @Override
                  public void onComplete() {
                    executor.execute(signal::complete);
                  }
                });
        return new Subscription() {
          @Override
          public void onUnsubscribe() {
            outer.unsubscribe();
          }
        };
      }
    }
  }

  public static class Subscription {

    private boolean unsubscribed = false;

    /**
     * Subclasses should override for custom behavior
     */
    protected void onUnsubscribe() {
    }

    public final void unsubscribe() {
      if (!unsubscribed) {
        onUnsubscribe();
        unsubscribed = true;
      }
    }
  }

  abstract static class SubscribeObserver<T> implements Observer<T>, OnSubscribe {

    private Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    Subscription getSubscription() {
      return subscription == null ? new Subscription() : subscription;
    }
  }

  public abstract static class ObservesNext<T> implements Observer<T> {

    @Override
    public void onError(@NonNull Throwable t) {
    }

    @Override
    public void onComplete() {
    }
  }

  /**
   * Class to observe only complete block in subscription.
   */
  public abstract static class ObservesComplete<T> implements Observer<T> {

    @Override
    public void onNext(@NonNull T t) {
    }

    @Override
    public void onError(@NonNull Throwable t) {
    }
  }

  /**
   * Class to observe only errors in subscription.
   */
  public abstract static class ObservesError implements Observer<Object> {

    @Override
    public void onNext(@NonNull Object t) {
    }

    @Override
    public void onComplete() {
    }
  }

  public static class ForwarderObserver<T> implements Observer<T> {

    private final Signal<T> destination;

    public ForwarderObserver(Signal<T> destination) {
      this.destination = destination;
    }

    @Override
    public void onNext(@NonNull T t) {
      destination.next(t);
    }

    @Override
    public void onError(@NonNull Throwable t) {
      destination.error(t);
    }

    @Override
    public void onComplete() {
      destination.complete();
    }
  }
}
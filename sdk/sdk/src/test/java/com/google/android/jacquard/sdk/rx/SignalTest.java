/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.rx;

import static android.os.Looper.getMainLooper;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link Signal}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class SignalTest {

  @Test
  public void testFromList() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1,2,3,4));
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    sig.observe(c);
    // Assert
    assertEquals(4, c.xs.size());
    assertEquals(Arrays.asList(1, 2, 3, 4), c.xs);
  }

  @Test
  public void testMapList() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1,2,3,4));
    Signal<String> sigAsString = sig.map(new Fn<Integer,String>() {
      @Override
      public String apply(Integer integer) {
        return String.valueOf(integer);
      }
    });
    CollectAll<String> c = new CollectAll<>();
    // Act
    sigAsString.observe(c);
    // Assert
    assertEquals(4, c.xs.size());
    assertEquals(Arrays.asList("1", "2", "3", "4"), c.xs);
  }

  @Test
  public void testCombineMaps() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1,2,3,4));
    Signal<Integer> sigPlus2 = sig.map(new Fn<Integer, Integer>() {
      @Override
      public Integer apply(Integer integer) {
        return integer + 2;
      }
    });
    Signal<String> sigAsString = sigPlus2.map(new Fn<Integer, String>() {
      @Override
      public String apply(Integer integer) {
        return String.valueOf(integer);
      }
    });
    CollectAll<String> c = new CollectAll<>();
    // Act
    sigAsString.observe(c);
    // Assert
    assertEquals(4, c.xs.size());
    assertEquals(Arrays.asList("3", "4", "5", "6"), c.xs);
  }

  @Test
  public void testMappedUnsubscribe() {
    // Assign
    Signal<Integer> sig = Signal.create();
    Signal<String> sigString = sig.map(new Fn<Integer, String>() {
      @Override
      public String apply(Integer integer) {
        return integer.toString();
      }
    });
    // Act
    Signal.Subscription sub = sigString.observe(new Signal.ObservesNext<String>() {
      @Override
      public void onNext(@NonNull String s) {

      }
    });
    // Assert
    assertTrue("should have an observer", sig.hasObservers());
    sub.unsubscribe();
    assertFalse("should have no observers", sig.hasObservers());
  }

  @Test
  public void testTimeout() {
    // Assign
    Signal<?> s = Signal.create().timeout(5000);
    s.observe(new CollectAll<Object>());
    // Act
    Robolectric.getForegroundThreadScheduler().advanceBy(4999, MILLISECONDS);
    // Assert
    assertThrows(RuntimeException.class, () ->
        Robolectric.getForegroundThreadScheduler().advanceBy(1, MILLISECONDS)
    );
  }

  @Test
  public void testFlatMap() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1,2,3,4));
    Signal<Integer> sigPlus2 = sig.flatMap(new Fn<Integer, Signal<Integer>>() {
      @Override
      public Signal<Integer> apply(Integer integer) {
        return Signal.from(Arrays.asList(integer, integer + 2));
      }
    });
    Signal<String> sigAsString = sigPlus2.map(new Fn<Integer, String>() {
      @Override
      public String apply(Integer integer) {
        return String.valueOf(integer);
      }
    });
    CollectAll<String> c = new CollectAll<>();
    // Act
    sigAsString.observe(c);
    // Assert
    assertTrue("must be completed", c.completed);
    assertEquals(8, c.xs.size());
    assertEquals(Arrays.asList("1", "3", "2", "4", "3", "5", "4", "6"), c.xs);
  }

  @Test
  public void testSwitchMap() {
    // Assign
    CollectAll<Integer> c = new CollectAll<>();
    int[] expected = {0};
    Signal<Integer> sig = Signal.from(Arrays.asList(1,2,3,4));

    Signal<Integer> sig2 = Signal.create(signal -> {
      signal.next(1);
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          expected[0] = expected[0] + 1;
        }
      };
    });
    // Act
    sig.switchMap((Fn<Integer, Signal<Integer>>) integer -> sig2).observe(c);

    // Assert
    assertEquals(3, expected[0]);
    assertEquals(4, c.xs.size());
  }

  @Test
  public void testFilter() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Signal<Integer> odds = sig.filter(new Pred<Integer>() {
      @Override
      public boolean apply(Integer integer) {
        return integer % 2 == 1;
      }
    });
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    odds.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 3, 5, 7, 9), c.xs);
  }

  @Test
  public void testFilterWillCatchExceptions() {
    // Assign
    final boolean[] error = {false};
    // Act
    Signal.just(1)
        .filter(
            integer -> {
              throw new RuntimeException();
            })
        .onError(throwable -> error[0] = true);
    // Assert
    assertTrue("should emit an error", error[0]);
  }

  @Test
  public void testTakeWhile() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Signal<Integer> odds = sig.takeWhile(new Pred<Integer>() {
      @Override
      public boolean apply(Integer integer) {
        return integer < 6;
      }
    });
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    odds.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), c.xs);
  }

  @Test
  public void testTake() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Signal<Integer> odds = sig.take(5);
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    odds.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), c.xs);
    assertTrue("must be completed", c.completed);
  }

  @Test
  public void testDrop() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Signal<Integer> odds = sig.drop(5);
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    odds.observe(c);
    // Assert
    assertEquals(Arrays.asList(6, 7, 8, 9, 10), c.xs);
  }

  @Test
  public void testDropWhile() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Signal<Integer> odds = sig.dropWhile(new Pred<Integer>() {
      @Override
      public boolean apply(Integer integer) {
        return integer < 6;
      }
    });
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    odds.observe(c);
    // Assert
    assertEquals(Arrays.asList(6, 7, 8, 9, 10), c.xs);
  }

  @Test
  public void testSingletonSignal() {
    // Assign
    Signal<String> sig = Signal.from("Hi!");
    CollectAll<String> c = new CollectAll<>();
    // Act
    sig.observe(c);
    // Assert
    assertEquals("Hi!", c.xs.get(0));
    assertTrue("completed", c.completed);
    assertFalse("observers removed on complete", sig.hasObservers());
  }

  @Test
  public void testCannedSignal() {
    // Assign
    Signal<Integer> sig = Signal.from(Arrays.asList(1, 2, 3, 4));
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    sig.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 3, 4), c.xs);
    c.xs.clear();
    // cannot re-subscribed when already finished
    sig.observe(c);
    assertEquals(0, c.xs.size());
  }

  @Test
  public void testCannedMerge() {
    // Assign
    Signal<Integer> s1 = Signal.from(Arrays.asList(1, 2));
    Signal<Integer> s2 = Signal.from(Arrays.asList(3, 4));
    Signal<Integer> merged = Signal.merge(s1, s2);
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    merged.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 3, 4), c.xs);
    assertFalse("observers removed on complete", s1.hasObservers());
    assertFalse("merged observers removed on complete", merged.hasObservers());
  }

  @Test
  public void tapErrorHasSubscribeHasSideEffects() {
    // Assign
    final boolean[] sideEffect = {false};
    final boolean[] tappedError = {false};
    Signal<Integer> xs =
        Signal.<Integer>create(
            signal -> {
              sideEffect[0] = true;
              signal.complete();
              return new Signal.Subscription();
            })
            .tapError(
                t -> {
                  tappedError[0] = true;
                });
    // Act
    xs.consume();
    // Assert
    assertTrue("consumed has side-effect", sideEffect[0]);
    assertFalse("consumed has no error", tappedError[0]);
  }

  @Test
  public void tapErrorHasNoSubscribeNoError() {
    // Assign
    final boolean[] sideEffect = {false};
    final boolean[] tappedError = {false};
    Signal<Integer> xs =
        Signal.<Integer>create(
            signal -> {
              sideEffect[0] = true;
              signal.complete();
              return new Signal.Subscription();
            })
            .tapError(
                t -> {
                  tappedError[0] = true;
                });
    // Act
    xs.consume();
    // Assert
    assertFalse("unconsumed has no error", tappedError[0]);
  }

  @Test
  public void tapErrorHasNoSubscribeHasError() {
    // Assign
    final boolean[] sideEffect = {false};
    final boolean[] tappedError = {false};
    Signal<Integer> xs =
        Signal.<Integer>create(
            signal -> {
              sideEffect[0] = true;
              signal.complete();
              return new Signal.Subscription();
            })
            .tapError(
                t -> {
                  tappedError[0] = true;
                });
    xs.consume();
    Signal<Integer> ys =
        Signal.<Integer>create(
            signal -> {
              sideEffect[0] = true;
              signal.next(1);
              return new Signal.Subscription();
            })
            .map(
                x -> {
                  if (true) {
                    throw new IllegalStateException(); // intentional
                  } else {
                    return 1;
                  }
                })
            .tapError(t -> tappedError[0] = true);
    // Act
    ys.consume();
    // Assert
    assertTrue("consumed has error", tappedError[0]);
  }

  @Test
  public void testMergeMultiple() {
    // Assign
    Signal<Integer> merged = Signal.merge(Arrays.asList(
        Signal.from(Arrays.asList(1, 2, 3, 4)),
        Signal.from(Arrays.asList(5,6,7,8)),
        Signal.from(Arrays.asList(9,0,1,2))));
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    merged.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2), c.xs);
  }

  @Test
  public void testDistinct() {
    // Assign
    Signal<Integer> s1 = Signal.from(Arrays.asList(1, 2, 2, 1, 5, 9, 4, 5, 8)).distinct();
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    s1.observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 5, 9, 4, 8), c.xs);
    assertFalse("observers removed on complete", s1.hasObservers());
  }

  @Test
  public void testScanAndLast() {
    // Assign
    Signal<Integer> s1 = Signal.from(Arrays.asList(1, 2, 2, 1, 5, 9, 4, 5, 8));
    CollectAll<ArrayList<Integer>> c = new CollectAll<>();
    // Act
    s1.scan(new ArrayList<Integer>(), new Fn2<ArrayList<Integer>, Integer, ArrayList<Integer>>() {
      @Override
      public ArrayList<Integer> apply(ArrayList<Integer> integers, Integer integer) {
        integers.add(integer);
        return integers;
      }
    }).last().observe(c);
    // Assert
    assertEquals(Arrays.asList(1, 2, 2, 1, 5, 9, 4, 5, 8), c.xs.get(0));
    assertTrue("must complete", c.completed);
    assertEquals(1, c.count);
  }

  @Test
  public void testSticky() {
    // Assign
    Signal<Integer> s1 = Signal.create();
    Signal<Integer> sticky = s1.sticky();
    s1.next(1);
    s1.next(2);
    s1.next(3);
    s1.next(4);
    CollectAll<Integer> c = new CollectAll<>();
    // Act
    sticky.observe(c);
    // Assert
    assertEquals(4, (int)c.xs.get(0));
    assertEquals(1, c.count);
  }

  @Test
  public void sharedWillMultiCast() {
    // Assign
    final AtomicInteger effectCount = new AtomicInteger();
    Signal<Integer> s = Signal.create();
    Signal<Integer> withSideEffect = s.map(x -> {
      effectCount.incrementAndGet();
      return x + 0;
    });
    Signal<Integer> shared = withSideEffect.shared();
    CollectAll<Integer> ob1 = new CollectAll<>();
    CollectAll<Integer> ob2 = new CollectAll<>();
    shared.observe(ob1);
    shared.observe(ob2);
    // Act
    s.next(1);
    // Assert
    assertEquals(1, ob1.xs.size());
    assertEquals(1, ob2.xs.size());
    assertEquals(1, effectCount.get());
    // Act
    s.next(2);
    // Assert
    assertEquals(2, ob1.xs.size());
    assertEquals(2, ob2.xs.size());
    assertEquals(2, effectCount.get());
    assertEquals(Arrays.asList(1, 2), ob1.xs);
    assertEquals(Arrays.asList(1, 2), ob2.xs);
  }

  @Test
  public void delayWithNoCompletion() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    // Act
    Signal.from(1).delay(1000).observe(ob);
    // Assert
    assertFalse("not completed", ob.completed);
  }

  @Test
  public void delayWithCompletion() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    Signal.from(1).delay(1000).observe(ob);
    // Act
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // Assert
    assertTrue("must be completed", ob.completed);
  }

  @Test
  public void delayWithUnsubscribe() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    Signal.Subscription s = Signal.from(1).delay(1000).observe(ob);
    s.unsubscribe();
    // Act
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // Assert
    assertFalse("must not be completed", ob.completed);
  }

  @Test
  public void cannedIntoAsync() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    Signal.from(Arrays.asList(1, 2, 3)).flatMap(x -> Signal.from(x).delay(1000)).observe(ob);
    // Act
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // the sequential nature of async flatMap means there is a need to advance several times in
    // testing once for each item in the original list/async op
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // Assert
    assertTrue("must be completed", ob.completed);
    assertEquals(Arrays.asList(1, 2, 3), ob.xs);
  }

  @Test
  public void cannedIntoAsyncOrdering() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    Signal.from(Arrays.asList(3, 2, 1))
        .flatMap(x -> Signal.from(Arrays.asList(x, x + 10, x + 20)).delay(x * 1000)).observe(ob);
    // Act
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // async advances from sequential flatMap
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
    // Assert
    assertTrue("must be completed", ob.completed);
    assertEquals(Arrays.asList(3, 13, 23, 2, 12, 22, 1, 11, 21), ob.xs);
  }

  @Test
  public void observeOnMainExecutor() throws InterruptedException {
    // Assign
    CollectAll<Boolean> ob = new CollectAll<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Signal<Boolean> backgroundSignal =
        Signal.create(
            signal -> {
              new Thread(
                  () -> {
                    signal.next(true);
                    signal.complete();
                    countDownLatch.countDown();
                  })
                  .start();
              return new Subscription();
            });
    backgroundSignal
        .tap(value -> assertNotSame(getMainLooper().getThread(), Thread.currentThread()))
        .observeOn(Executors.mainThreadExecutor())
        .tap(value -> assertSame(getMainLooper().getThread(), Thread.currentThread()))
        .observe(ob);
    countDownLatch.await();
    // Act
    shadowOf(getMainLooper()).idle();
    // Assert
    assertTrue(ob.xs.get(0));
  }

  @Test
  public void observeOnMainExecutorUnsubscribed() throws InterruptedException {
    // Assign
    CountDownLatch countDownLatch = new CountDownLatch(1);
    CollectAll<Boolean> ob = new CollectAll<>();
    Signal<Boolean> backgroundSignal =
        Signal.create(
            signal -> {
              new Thread(
                  () -> {
                    signal.next(true);
                    signal.complete();
                    countDownLatch.countDown();
                  })
                  .start();
              return new Subscription();
            });
    Subscription subscription =
        backgroundSignal.observeOn(Executors.mainThreadExecutor()).observe(ob);
    subscription.unsubscribe();
    // Act
    countDownLatch.await();
    // Assert
    assertTrue(ob.xs.isEmpty());
  }

  static class CollectAll<T> implements Signal.Observer<T> {
    final List<T> xs = new ArrayList<>();
    private boolean completed;
    private int count = 0;
    @Override
    public void onNext(@NonNull T t) {
      xs.add(t);
      count++;
    }

    @Override
    public void onError(@NonNull Throwable t) {
      throw new RuntimeException(t);
    }

    @Override
    public void onComplete() {
      completed = true;
    }
  }

  static class Noop<T> implements Signal.Observer<T> {
    static <T> Noop<T> create() {
      return new Noop<>();
    }
    @Override
    public void onNext(@NonNull T t) {

    }

    @Override
    public void onError(@NonNull Throwable t) {

    }

    @Override
    public void onComplete() {

    }
  }

  @Test
  public void recoverWithNoOp() {
    // Assign
    CollectAll<Integer> is = new CollectAll<>();
    // Act
    Signal.from(1).recoverWith(x -> Signal.empty()).observe(is);
    // Assert
    assertEquals(1, is.xs.size());
    assertEquals(Collections.singletonList(1), is.xs);
  }

  @Test
  public void recoverWithFromEmpty() {
    // Assign
    CollectAll<Integer> is = new CollectAll<>();
    // Act
    Signal.<Integer>empty(new RuntimeException("should not be surfaced")).recoverWith(
        x -> Signal.from(2)).observe(is);
    // Assert
    assertEquals(1, is.xs.size());
    assertEquals(Collections.singletonList(2), is.xs);
  }

  @Test
  public void recursionStackSafety() throws StackOverflowError{
    // Assign
    Signal<Integer> signal = recurse(5000);
    // Act
    assertThrows(StackOverflowError.class, signal::consume);
  }

  @Test
  public void distinctUntilChanged() {
    // Assign
    CollectAll<Integer> ob = new CollectAll<>();
    List<Integer> expected = Arrays.asList(1, 2, 3, 2);
    Signal<Integer> signal =
        Signal.create(
            signal1 -> {
              signal1.next(1);
              signal1.next(1);
              signal1.next(2);
              signal1.next(2);
              signal1.next(3);
              signal1.next(2);
              return new Subscription();
            });

    // Act
    signal.distinctUntilChanged().observe(ob);
    // Assert
    assertEquals(expected, ob.xs);
  }

  private static Signal<Integer> recurse(int count) {
    return Signal.from(count).flatMap(c -> recurse(count - 1));
  }
}

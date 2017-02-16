/*
 * Copyright 2016 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.uniroma3.mat.extendedset.intset;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.Clock;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import it.uniroma3.mat.extendedset.test.Benchmark;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Category({Benchmark.class})
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
@BenchmarkOptions(clock = Clock.NANO_TIME, benchmarkRounds = 10)
public class ImmutableConciseSetBenchmark
{
  private static final long SEED = 34731478387914L;
  private static final int NUM_INTS = 17165;
  private static final int INT_LIMIT_MAX = ConciseSetUtils.MAX_ALLOWED_INTEGER;
  private static final Set<Integer> INTS = new HashSet<>(NUM_INTS);
  private static ImmutableConciseSet immutableConciseSet;

  @Rule
  public TestRule benchmarkRun = new BenchmarkRule();

  @BeforeClass
  public static void setUp() throws Exception
  {
    final Random random = new Random(SEED);

    final ConciseSet conciseSet = new ConciseSet();
    for (int i = 0; i < NUM_INTS; ++i) {

      int j;
      do {
        j = random.nextInt(INT_LIMIT_MAX);
      } while (!INTS.add(j));
      conciseSet.add(j);
    }
    immutableConciseSet = ImmutableConciseSet.newImmutableFromMutable(conciseSet);
  }

  @Test
  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 20)
  public void timeContains() throws Exception
  {
    for (int i : INTS) {
      Assert.assertEquals(INTS.contains(i), immutableConciseSet.contains(i));
    }
  }

  @Test
  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 20)
  public void timeUnion() throws Exception
  {
    for (int i : INTS) {
      final ConciseSet conciseSet = new ConciseSet();
      conciseSet.add(i);
      final ImmutableConciseSet otherSet = ImmutableConciseSet.newImmutableFromMutable(conciseSet);
      Assert.assertEquals(
          INTS.contains(i),
          ImmutableConciseSet.intersection(otherSet, immutableConciseSet).size() != 0
      );
    }
  }
}

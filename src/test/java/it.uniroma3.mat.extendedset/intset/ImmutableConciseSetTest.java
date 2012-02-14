package it.uniroma3.mat.extendedset.intset;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

/**
 */
public class ImmutableConciseSetTest
{
  public static final int NO_COMPLEMENT_LENGTH = -1;

  @Test
  public void testWordIteratorNext1()
  {
    final int[] ints = {1, 2, 3, 4, 5};
    ConciseSet set = new ConciseSet();
    for (int i : ints) {
      set.add(i);
    }
    ImmutableConciseSet iSet = ImmutableConciseSet.newImmutableFromMutable(set);

    ImmutableConciseSet.WordIterator itr = iSet.newWordIterator();
    Assert.assertEquals(new Integer(0x8000003E), itr.next());

    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testWordIteratorNext2()
  {
    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 100000; i++) {
      set.add(i);

    }
    ImmutableConciseSet iSet = ImmutableConciseSet.newImmutableFromMutable(set);

    ImmutableConciseSet.WordIterator itr = iSet.newWordIterator();
    Assert.assertEquals(new Integer(0x40000C98), itr.next());
    Assert.assertEquals(new Integer(0x81FFFFFF), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  /**
   * Advance to middle of a fill
   */
  @Test
  public void testWordIteratorAdvanceTo1()
  {
    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 100000; i++) {
      set.add(i);

    }
    ImmutableConciseSet iSet = ImmutableConciseSet.newImmutableFromMutable(set);

    ImmutableConciseSet.WordIterator itr = iSet.newWordIterator();
    itr.advanceTo(50);
    Assert.assertEquals(new Integer(1073744998), itr.next());
    Assert.assertEquals(new Integer(0x81FFFFFF), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  /**
   * Advance past a fill directly to a new literal
   */
  @Test
  public void testWordIteratorAdvanceTo2()
  {
    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 100000; i++) {
      set.add(i);

    }
    ImmutableConciseSet iSet = ImmutableConciseSet.newImmutableFromMutable(set);

    ImmutableConciseSet.WordIterator itr = iSet.newWordIterator();
    itr.advanceTo(3225);
    Assert.assertEquals(new Integer(0x81FFFFFF), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneLitOneLit()
  {
    int[] words = {-1, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));

    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x40000001), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneLitPureOneFill()
  {
    int[] words = {-1, 0x40000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x40000005), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneLitDirtyOneFill()
  {
    int[] words = {-1, 0x42000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(new Integer(0x42000004), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneFillOneLit()
  {
    int[] words = {0x40000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x40000005), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneFillPureOneFill()
  {
    int[] words = {0x40000004, 0x40000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x40000009), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactOneFillDirtyOneFill()
  {
    int[] words = {0x40000004, 0x42000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x40000004), itr.next());
    Assert.assertEquals(new Integer(0x42000004), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroLitZeroLit()
  {
    int[] words = {0x80000000, 0x80000000, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x00000001), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroLitPureZeroFill()
  {
    int[] words = {0x80000000, 0x00000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x00000005), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroLitDirtyZeroFill()
  {
    int[] words = {0x80000000, 0x02000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x80000000), itr.next());
    Assert.assertEquals(new Integer(0x02000004), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroFillZeroLit()
  {
    int[] words = {0x00000004, 0x80000000, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x00000005), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroFillPureZeroFill()
  {
    int[] words = {0x00000004, 0x00000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x00000009), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactZeroFillDirtyZeroFill()
  {
    int[] words = {0x00000004, 0x02000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x00000004), itr.next());
    Assert.assertEquals(new Integer(0x02000004), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleOneBitLitZeroLit()
  {
    int[] words = {0x80000001, 0x80000000, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x02000001), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactDoubleOneBitLitZeroLit()
  {
    int[] words = {0x80000003, 0x80000000, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x80000003), itr.next());
    Assert.assertEquals(new Integer(0x80000000), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleOneBitLitPureZeroFill()
  {
    int[] words = {0x80000001, 0x00000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x02000005), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactDoubleOneBitLitPureZeroFill()
  {
    int[] words = {0x80000003, 0x00000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x80000003), itr.next());
    Assert.assertEquals(new Integer(0x00000004), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleOneBitLitDirtyZeroFill()
  {
    int[] words = {0x80000001, 0x02000004, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x80000001), itr.next());
    Assert.assertEquals(new Integer(0x02000004), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleZeroBitLitOneLit()
  {
    int[] words = {0xFFFFFFFE, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x42000001), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactDoubleZeroBitLitOneLit()
  {
    int[] words = {0xFFFFFFEE, -1};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0xFFFFFFEE), itr.next());
    Assert.assertEquals(new Integer(-1), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleZeroBitLitPureOneFill()
  {
    int[] words = {0xFFFFFFFE, 0x40000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0x42000005), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactDoubleZeroBitLitPureOneFill()
  {
    int[] words = {0xFFFFFFFC, 0x40000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0xFFFFFFFC), itr.next());
    Assert.assertEquals(new Integer(0x40000004), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactSingleZeroBitLitDirtyOneFill()
  {
    int[] words = {0xFFFFFFFE, 0x42000004};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0xFFFFFFFE), itr.next());
    Assert.assertEquals(new Integer(0x42000004), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  @Test
  public void testCompactTwoLiterals()
  {
    int[] words = {0xFFFFFFFE, 0xFFEFFEFF};

    ImmutableConciseSet res = ImmutableConciseSet.compact(new ImmutableConciseSet(IntBuffer.wrap(words)));
    ImmutableConciseSet.WordIterator itr = res.newWordIterator();

    Assert.assertEquals(new Integer(0xFFFFFFFE), itr.next());
    Assert.assertEquals(new Integer(0xFFEFFEFF), itr.next());
    Assert.assertEquals(itr.hasNext(), false);
  }

  /**
   * Set 1: zero literal, zero fill with flipped bit 33, literal
   * Set 2: zero literal, zero fill with flipped bit 34, literal
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion1()
  {
    final int[] ints1 = {33, 100000};
    final int[] ints2 = {34, 100000};
    List<Integer> expected = Arrays.asList(33, 34, 100000);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: zero literal, zero fill with flipped bit 33, literal
   * Set 2: zero literal, zero fill with flipped bit 34, literal
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion2()
  {
    final int[] ints1 = {33, 100000};
    final int[] ints2 = {34, 200000};
    List<Integer> expected = Arrays.asList(33, 34, 100000, 200000);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: zero fill, one fill
   * Set 2: zero fill, one fill with flipped bit 62
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion3()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 62; i < 10001; i++) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 63; i < 10002; i++) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 62; i < 10002; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: zero literal, one fill with flipped bit 62
   * Set 2: zero literal, literal, one fill, literal
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion4()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 63; i < 1001; i++) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 64; i < 1002; i++) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 63; i < 1002; i++) {
      expected.add(i);
    }


    ConciseSet blah = new ConciseSet();
    for (int i : expected) {
      blah.add(i);
    }
    verifyUnion(expected, sets);
  }

  /**
   * Set 1: literal
   * Set 2: zero fill, zero literal, zero fill with flipped 33 bit, zero fill with flipped 1000000 bit, literal
   * Set3: literal, zero fill with flipped 34th bit, literal
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion5()
  {
    final int[] ints1 = {1, 2, 3, 4, 5};
    final int[] ints2 = {100000, 2405983, 33};
    final int[] ints3 = {0, 4, 5, 34, 333333};
    final List<Integer> expected = Arrays.asList(0, 1, 2, 3, 4, 5, 33, 34, 100000, 333333, 2405983);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    ConciseSet set3 = new ConciseSet();
    for (int i : ints3) {
      set3.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2),
        ImmutableConciseSet.newImmutableFromMutable(set3)
    );

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: literal
   * Set 2: literal
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion6()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 30; i++) {
      if (i != 28) {
        set1.add(i);
      }
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 0; i < 30; i++) {
      if (i != 27) {
        set2.add(i);
      }
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 30; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: zero literal, literal, one fill with flipped bit
   * Set 2: zero literal, one fill with flipped bit
   * <p/>
   * Testing merge
   */
  @Test
  public void testUnion7()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 64; i < 1005; i++) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 63; i < 99; i++) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 63; i < 1005; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: One fill with flipped 27th bit
   * Set 2: One fill with flipped 28th bit
   * <p/>
   * Testing creation of one fill with no flipped bits
   */
  @Test
  public void testUnion8()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      if (i != 27) {
        set1.add(i);
      }
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      if (i != 28) {
        set2.add(i);
      }
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1000; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Literal and one fill
   * Set 2: One fill with flipped 28th bit
   * <p/>
   * Testing creation of one fill with correct flipped bit
   */
  @Test
  public void testUnion9()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      if (!(i == 27 || i == 28)) {
        set1.add(i);
      }
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      if (i != 28) {
        set2.add(i);
      }
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1000; i++) {
      if (i != 28) {
        expected.add(i);
      }
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Multiple literals
   * Set 2: Multiple literals
   * <p/>
   * Testing merge of pure sequences of literals
   */
  @Test
  public void testUnion10()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 1000; i += 2) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 1; i < 1000; i += 2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1000; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Multiple literals
   * Set 2: Zero fill and literal
   * <p/>
   * Testing skipping of zero fills
   */
  @Test
  public void testUnion11()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 1000; i += 2) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    set2.add(10000);

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1000; i += 2) {
      expected.add(i);
    }
    expected.add(10000);

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Literal with 4 bits marked
   * Set 2: Zero fill with flipped bit 5
   * <p/>
   * Testing merge of literal and zero fill with flipped bit
   */
  @Test
  public void testUnion12()
  {
    final int[] ints1 = {1, 2, 3, 4};
    final int[] ints2 = {5, 1000};
    final List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 1000);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Literal with bit 0
   * Set 2: One fill with flipped bit 0
   * <p/>
   * Testing merge of literal and one fill with flipped bit
   */
  @Test
  public void testUnion13()
  {
    List<Integer> expected = Lists.newArrayList();
    final int[] ints1 = {0};

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 1; i < 100; i++) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 100; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Zero fill with flipped bit 0
   * Set 2: One fill with flipped bit 0
   * <p/>
   * Testing merge of flipped bits in zero and one fills
   */
  @Test
  public void testUnion14()
  {
    List<Integer> expected = Lists.newArrayList();
    final int[] ints1 = {0, 100};

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 1; i < 100; i++) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i <= 100; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: Zero fill with flipped bit 1
   * Set 2: Literal with 0th bit marked
   * Set 3: One Fill from 1 to 100 with flipped bit 0
   * <p/>
   * Testing merge of flipped bits in zero and one fills with a literal
   */
  @Test
  public void testUnion15()
  {
    List<Integer> expected = Lists.newArrayList();
    final int[] ints1 = {1, 100};
    final int[] ints2 = {0};

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    ConciseSet set3 = new ConciseSet();
    for (int i = 1; i < 100; i++) {
      set3.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2),
        ImmutableConciseSet.newImmutableFromMutable(set3)
    );

    for (int i = 0; i <= 100; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  /**
   * Testing merge of offset elements
   */
  @Test
  public void testUnion16()
  {
    final int[] ints1 = {1001, 1002, 1003};
    final int[] ints2 = {1034, 1035, 1036};
    List expected = Arrays.asList(1001, 1002, 1003, 1034, 1035, 1036);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyUnion(expected, sets);
  }

  /**
   * Testing merge of same elements
   */
  @Test
  public void testUnion17()
  {
    final int[] ints1 = {1, 2, 3, 4, 5};
    final int[] ints2 = {1, 2, 3, 4, 5};
    List expected = Arrays.asList(1, 2, 3, 4, 5);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyUnion(expected, sets);
  }

  @Test
  public void testUnion18()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    set2.add(1000);
    set2.add(10000);

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1001; i++) {
      expected.add(i);
    }
    expected.add(10000);

    verifyUnion(expected, sets);
  }

  /**
   * Set 1: one fill, all ones literal
   * Set 2: zero fill, one fill, literal
   */
  @Test
  public void testUnion19()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    for (int i = 0; i < 93; i++) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i = 62; i < 1000; i++) {
      set2.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    for (int i = 0; i < 1000; i++) {
      expected.add(i);
    }

    verifyUnion(expected, sets);
  }

  private void verifyUnion(List<Integer> expected, List<ImmutableConciseSet> sets)
  {
    List<Integer> actual = Lists.newArrayList();
    ImmutableConciseSet set = ImmutableConciseSet.union(sets);
    IntSet.IntIterator itr = set.iterator();
    while (itr.hasNext()) {
      actual.add(itr.next());
    }
    Assert.assertEquals(expected, actual);
  }

  /**
   * Testing basic intersection of similar sets
   */
  @Test
  public void testIntersection1()
  {
    final int[] ints1 = {33, 100000};
    final int[] ints2 = {33, 100000};
    List<Integer> expected = Arrays.asList(33, 100000);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyIntersection(expected, sets);
  }

  /**
   * Set1: literal, zero fill with flip bit, literal
   * Set2: literal, zero fill with different flip bit, literal
   */
  @Test
  public void testIntersection2()
  {
    final int[] ints1 = {33, 100000};
    final int[] ints2 = {34, 100000};
    List<Integer> expected = Arrays.asList(100000);

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyIntersection(expected, sets);
  }

  /**
   * Testing intersection of one fills
   */
  @Test
  public void testIntersection3()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    ConciseSet set2 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      set1.add(i);
      set2.add(i);
      expected.add(i);
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyIntersection(expected, sets);
  }

  /**
   * Similar to previous test with one bit in the sequence set to zero
   */
  @Test
  public void testIntersection4()
  {
    List<Integer> expected = Lists.newArrayList();
    ConciseSet set1 = new ConciseSet();
    ConciseSet set2 = new ConciseSet();
    for (int i = 0; i < 1000; i++) {
      set1.add(i);
      if (i != 500) {
        set2.add(i);
        expected.add(i);
      }
    }

    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyIntersection(expected, sets);
  }

  /**
   * Testing with disjoint sets
   */
  @Test
  public void testIntersection5()
  {
    final int[] ints1 = {33, 100000};
    final int[] ints2 = {34, 200000};
    List<Integer> expected = Lists.newArrayList();

    ConciseSet set1 = new ConciseSet();
    for (int i : ints1) {
      set1.add(i);
    }
    ConciseSet set2 = new ConciseSet();
    for (int i : ints2) {
      set2.add(i);
    }
    List<ImmutableConciseSet> sets = Arrays.asList(
        ImmutableConciseSet.newImmutableFromMutable(set1),
        ImmutableConciseSet.newImmutableFromMutable(set2)
    );

    verifyIntersection(expected, sets);
  }

  private void verifyIntersection(List<Integer> expected, List<ImmutableConciseSet> sets)
  {
    List<Integer> actual = Lists.newArrayList();
    ImmutableConciseSet set = ImmutableConciseSet.intersection(sets);
    IntSet.IntIterator itr = set.iterator();
    while (itr.hasNext()) {
      actual.add(itr.next());
    }
    Assert.assertEquals(expected, actual);
  }

  /**
   * Basic complement with no length
   */
  @Test
  public void testComplement1()
  {
    final int[] ints = {1, 100};
    List<Integer> expected = Lists.newArrayList();

    ConciseSet set = new ConciseSet();
    for (int i : ints) {
      set.add(i);
    }

    for (int i = 0; i <= 100; i++) {
      if (i != 1 && i != 100) {
        expected.add(i);
      }
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, NO_COMPLEMENT_LENGTH);
  }

  /**
   * Complement of a single partial word
   */
  @Test
  public void testComplement2()
  {
    List<Integer> expected = Lists.newArrayList();

    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 15; i++) {
      set.add(i);
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, NO_COMPLEMENT_LENGTH);
  }

  /**
   * Complement of a single partial word with a length set in the same word
   */
  @Test
  public void testComplement3()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 21;

    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 15; i++) {
      set.add(i);
    }
    for (int i = 15; i < length; i++) {
      expected.add(i);
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of a single partial word with a length set in a different word
   */
  @Test
  public void testComplement4()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 41;

    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 15; i++) {
      set.add(i);
    }
    for (int i = 15; i < length; i++) {
      expected.add(i);
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of a single partial word with a length set to create a one fill
   */
  @Test
  public void testComplement5()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 1001;

    ConciseSet set = new ConciseSet();
    for (int i = 0; i < 15; i++) {
      set.add(i);
    }
    for (int i = 15; i < length; i++) {
      expected.add(i);
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of words with a length set to create a one fill
   */
  @Test
  public void testComplement6()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 1001;

    ConciseSet set = new ConciseSet();
    for (int i = 65; i <= 100; i++) {
      set.add(i);
    }
    for (int i = 0; i < length; i++) {
      if (i < 65 || i > 100) {
        expected.add(i);
      }
    }

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of 2 words with a length in the second word
   */
  @Test
  public void testComplement7()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 37;

    ConciseSet set = new ConciseSet();
    for (int i = 0; i <= 35; i++) {
      set.add(i);
    }
    expected.add(36);

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of a one literal with a length set to complement the next bit in the next word
   */
  @Test
  public void testComplement8()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 32;

    ConciseSet set = new ConciseSet();
    for (int i = 0; i <= 30; i++) {
      set.add(i);
    }
    expected.add(31);

    ImmutableConciseSet testSet = ImmutableConciseSet.newImmutableFromMutable(set);

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of a null set with a length
   */
  @Test
  public void testComplement9()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 35;

    for (int i = 0; i < length; i++) {
      expected.add(i);
    }

    ImmutableConciseSet testSet = new ImmutableConciseSet();

    verifyComplement(expected, testSet, length);
  }

  /**
   * Complement of a null set to create a one fill
   */
  @Test
  public void testComplement10()
  {
    List<Integer> expected = Lists.newArrayList();
    final int length = 93;

    for (int i = 0; i < length; i++) {
      expected.add(i);
    }

    ImmutableConciseSet testSet = new ImmutableConciseSet();

    verifyComplement(expected, testSet, length);
  }

  private void verifyComplement(List<Integer> expected, ImmutableConciseSet set, int endIndex)
  {
    List<Integer> actual = Lists.newArrayList();

    ImmutableConciseSet res;
    if (endIndex == NO_COMPLEMENT_LENGTH) {
      res = ImmutableConciseSet.complement(set);
    } else {
      res = ImmutableConciseSet.complement(set, endIndex);
    }
    IntSet.IntIterator itr = res.iterator();
    while (itr.hasNext()) {
      actual.add(itr.next());
    }
    Assert.assertEquals(expected, actual);
  }
}

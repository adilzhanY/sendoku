package com.sendoku.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CandidatesTest {

    @Test
    fun `an empty set holds nothing`() {
        val empty = Candidates.EMPTY
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertFalse(empty.isSingle)
        assertEquals(Board.EMPTY, empty.single)
        assertEquals(Board.EMPTY, empty.lowest)
        assertEquals(emptyList(), empty.toList())
    }

    @Test
    fun `of builds the set the digits describe`() {
        val set = Candidates.of(7, 1, 4)
        assertEquals(3, set.size)
        assertEquals(listOf(1, 4, 7), set.toList())
        assertTrue(1 in set)
        assertTrue(4 in set)
        assertTrue(7 in set)
        assertFalse(2 in set)
        assertEquals(1, set.lowest)
    }

    @Test
    fun `all covers exactly the digits the grid shape allows`() {
        assertEquals(listOf(1, 2, 3, 4), Candidates.all(Dimensions.JUNIOR).toList())
        assertEquals(6, Candidates.all(Dimensions.SIX).size)
        assertEquals((1..9).toList(), Candidates.all(Dimensions.CLASSIC).toList())
        assertEquals(16, Candidates.all(Dimensions.HEXADOKU).size)
    }

    @Test
    fun `a set of one reports its digit`() {
        val single = Candidates.of(6)
        assertTrue(single.isSingle)
        assertEquals(6, single.single)
        assertEquals(6, single.lowest)
    }

    @Test
    fun `a set of two is not a single`() {
        val pair = Candidates.of(2, 6)
        assertFalse(pair.isSingle)
        assertEquals(Board.EMPTY, pair.single)
        assertEquals(2, pair.lowest)
    }

    @Test
    fun `plus and minus leave the original alone`() {
        val start = Candidates.of(1, 2)
        assertEquals(listOf(1, 2, 5), (start + 5).toList())
        assertEquals(listOf(2), (start - 1).toList())
        assertEquals(listOf(1, 2), start.toList())
    }

    @Test
    fun `adding a digit that is already there changes nothing`() {
        val set = Candidates.of(3, 8)
        assertEquals(set, set + 3)
    }

    @Test
    fun `removing a digit that is not there changes nothing`() {
        val set = Candidates.of(3, 8)
        assertEquals(set, set - 5)
    }

    @Test
    fun `set algebra behaves`() {
        val left = Candidates.of(1, 2, 3)
        val right = Candidates.of(3, 4)
        assertEquals(listOf(3), (left and right).toList())
        assertEquals(listOf(1, 2, 3, 4), (left or right).toList())
        assertEquals(listOf(1, 2), (left without right).toList())
        assertTrue(left overlaps right)
        assertFalse(left overlaps Candidates.of(4, 5))
    }

    @Test
    fun `containsAll is true for subsets and for the empty set`() {
        val set = Candidates.of(1, 2, 3)
        assertTrue(set.containsAll(Candidates.of(1, 3)))
        assertTrue(set.containsAll(set))
        assertTrue(set.containsAll(Candidates.EMPTY))
        assertFalse(set.containsAll(Candidates.of(1, 9)))
    }

    @Test
    fun `forEach walks the digits smallest first`() {
        val seen = mutableListOf<Int>()
        Candidates.of(9, 3, 5).forEach { seen.add(it) }
        assertEquals(listOf(3, 5, 9), seen)
    }

    @Test
    fun `forEach on an empty set never runs`() {
        var runs = 0
        Candidates.EMPTY.forEach { runs++ }
        assertEquals(0, runs)
    }

    @Test
    fun `toString shows the digits`() {
        assertEquals("{2,6}", Candidates.of(6, 2).toString())
        assertEquals("{}", Candidates.EMPTY.toString())
    }

    @Test
    fun `sets with the same digits are equal`() {
        assertEquals(Candidates.of(4, 1), Candidates.of(1, 4))
        assertEquals(Candidates.of(4, 1).hashCode(), Candidates.of(1, 4).hashCode())
    }

    @Test
    fun `a digit outside the supported range is rejected`() {
        assertFailsWith<IllegalArgumentException> { Candidates.of(0) }
        assertFailsWith<IllegalArgumentException> { Candidates.of(17) }
        assertFailsWith<IllegalArgumentException> { Candidates.EMPTY + -1 }
    }

    @Test
    fun `the top digit of a hexadoku still fits`() {
        val top = Candidates.of(16)
        assertTrue(top.isSingle)
        assertEquals(16, top.single)
    }
}

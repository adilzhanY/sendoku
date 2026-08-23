package com.sendoku.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HouseTest {

    private val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))

    @Test
    fun `a classic grid has nine of each kind of house`() {
        assertEquals(27, grid.houses.size)
        assertEquals(9, grid.houses.count { it.kind == HouseKind.ROW })
        assertEquals(9, grid.houses.count { it.kind == HouseKind.COLUMN })
        assertEquals(9, grid.houses.count { it.kind == HouseKind.BOX })
    }

    @Test
    fun `houses come rows first then columns then boxes`() {
        assertEquals(House(HouseKind.ROW, 0), grid.houses.first())
        assertEquals(House(HouseKind.COLUMN, 0), grid.houses[9])
        assertEquals(House(HouseKind.BOX, 8), grid.houses.last())
    }

    @Test
    fun `every house holds one cell per digit`() {
        for (house in grid.houses) {
            assertEquals(9, grid.cellsOf(house).size, "$house")
            assertEquals(9, grid.cellsOf(house).toSet().size, "$house")
        }
    }

    @Test
    fun `row and column cells are where they should be`() {
        assertEquals((0..8).toList(), grid.cellsOf(House(HouseKind.ROW, 0)).toList())
        assertEquals(listOf(2, 11, 20, 29, 38, 47, 56, 65, 74), grid.cellsOf(House(HouseKind.COLUMN, 2)).toList())
    }

    @Test
    fun `box cells are the nine that share a box`() {
        assertEquals(listOf(0, 1, 2, 9, 10, 11, 18, 19, 20), grid.cellsOf(House(HouseKind.BOX, 0)).toList())
        assertEquals(listOf(60, 61, 62, 69, 70, 71, 78, 79, 80), grid.cellsOf(House(HouseKind.BOX, 8)).toList())
    }

    @Test
    fun `a cell belongs to exactly its own row column and box`() {
        val cell = grid.indexOf(4, 7)
        assertEquals(
            listOf(House(HouseKind.ROW, 4), House(HouseKind.COLUMN, 7), House(HouseKind.BOX, 5)),
            grid.housesOf(cell),
        )
        assertEquals(4, grid.rowOf(cell))
        assertEquals(7, grid.colOf(cell))
        assertEquals(5, grid.boxOf(cell))
    }

    @Test
    fun `a cell sits inside every house it claims`() {
        for (cell in 0 until grid.cellCount) {
            for (house in grid.housesOf(cell)) {
                assertTrue(cell in grid.cellsOf(house).toSet(), "cell $cell is not in $house")
            }
        }
    }

    @Test
    fun `boxes of a six by six grid are three wide and two tall`() {
        val six = CandidateGrid.of(Board(Dimensions.SIX))
        assertEquals(18, six.houses.size)
        assertEquals(listOf(0, 1, 2, 6, 7, 8), six.cellsOf(House(HouseKind.BOX, 0)).toList())
        assertEquals(listOf(3, 4, 5, 9, 10, 11), six.cellsOf(House(HouseKind.BOX, 1)).toList())
    }

    @Test
    fun `isPlacedIn sees a digit already in the house`() {
        val working = CandidateGrid.of(Board(Dimensions.CLASSIC))
        working.place(working.indexOf(0, 4), 7)
        assertTrue(working.isPlacedIn(House(HouseKind.ROW, 0), 7))
        assertTrue(working.isPlacedIn(House(HouseKind.COLUMN, 4), 7))
        assertTrue(working.isPlacedIn(House(HouseKind.BOX, 1), 7))
        assertEquals(false, working.isPlacedIn(House(HouseKind.ROW, 1), 7))
        assertEquals(false, working.isPlacedIn(House(HouseKind.ROW, 0), 3))
    }

    @Test
    fun `a house prints the way a player counts`() {
        assertEquals("row 1", House(HouseKind.ROW, 0).toString())
        assertEquals("box 9", House(HouseKind.BOX, 8).toString())
    }

    @Test
    fun `the geometry is shared rather than rebuilt`() {
        assertSame(Geometry.of(Dimensions.CLASSIC), Geometry.of(Dimensions.CLASSIC))
        assertNotSame(Geometry.of(Dimensions.CLASSIC), Geometry.of(Dimensions.SIX))
    }

    @Test
    fun `the geometry answers the same questions as a grid does`() {
        val geometry = Geometry.of(Dimensions.CLASSIC)
        assertEquals(grid.houses, geometry.houses)
        for (cell in 0 until 81) {
            assertEquals(grid.housesOf(cell), geometry.housesOf(cell))
            assertEquals(grid.peersOf(cell).toList(), geometry.peersOf(cell).toList())
            assertEquals(grid.rowOf(cell), geometry.rowOf(cell))
            assertEquals(grid.colOf(cell), geometry.colOf(cell))
            assertEquals(grid.boxOf(cell), geometry.boxOf(cell))
        }
        for (house in geometry.houses) {
            assertEquals(grid.cellsOf(house).toList(), geometry.cellsOf(house).toList())
        }
    }

    @Test
    fun `seeing another cell means sharing a house with it`() {
        val geometry = Geometry.of(Dimensions.CLASSIC)
        for (a in 0 until 81) {
            assertTrue(!geometry.sees(a, a), "a cell should not see itself")
            val peers = geometry.peersOf(a).toSet()
            for (b in 0 until 81) {
                assertEquals(b in peers, geometry.sees(a, b), "cells $a and $b")
            }
        }
    }
}

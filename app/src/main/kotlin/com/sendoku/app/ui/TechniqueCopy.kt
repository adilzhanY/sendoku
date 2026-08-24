package com.sendoku.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId

/**
 * Every technique, explained in words a person would use.
 *
 * This is the difference between an app that helps and an app that finishes the puzzle for
 * you. "Hidden pair" means nothing to somebody who does not already know what it means, so
 * each one says what to look for, and then why it works.
 *
 * The words live in `strings.xml`; this only says which string belongs to which rule. That
 * split matters more here than anywhere else in the app: these are the hardest strings to
 * translate, and a translator needs to see them together, in a file, in order.
 */
public object TechniqueCopy {

    /** The name of a technique, as a hint says it. */
    @StringRes
    public fun nameOf(id: TechniqueId): Int = when (id) {
        TechniqueId.NAKED_SINGLE -> R.string.technique_naked_single
        TechniqueId.HIDDEN_SINGLE -> R.string.technique_hidden_single
        TechniqueId.LOCKED_CANDIDATES_POINTING -> R.string.technique_locked_candidates_pointing
        TechniqueId.LOCKED_CANDIDATES_CLAIMING -> R.string.technique_locked_candidates_claiming
        TechniqueId.NAKED_PAIR -> R.string.technique_naked_pair
        TechniqueId.HIDDEN_PAIR -> R.string.technique_hidden_pair
        TechniqueId.NAKED_TRIPLE -> R.string.technique_naked_triple
        TechniqueId.HIDDEN_TRIPLE -> R.string.technique_hidden_triple
        TechniqueId.NAKED_QUAD -> R.string.technique_naked_quad
        TechniqueId.HIDDEN_QUAD -> R.string.technique_hidden_quad
        TechniqueId.X_WING -> R.string.technique_x_wing
        TechniqueId.SIMPLE_COLOURING -> R.string.technique_simple_colouring
        TechniqueId.XY_WING -> R.string.technique_xy_wing
        TechniqueId.XYZ_WING -> R.string.technique_xyz_wing
        TechniqueId.W_WING -> R.string.technique_w_wing
        TechniqueId.SWORDFISH -> R.string.technique_swordfish
        TechniqueId.REMOTE_PAIRS -> R.string.technique_remote_pairs
        TechniqueId.UNIQUE_RECTANGLE -> R.string.technique_unique_rectangle
        TechniqueId.BUG_PLUS_ONE -> R.string.technique_bug_plus_one
        TechniqueId.JELLYFISH -> R.string.technique_jellyfish
        TechniqueId.MULTI_COLOURING -> R.string.technique_multi_colouring
        TechniqueId.X_CHAIN -> R.string.technique_x_chain
        TechniqueId.XY_CHAIN -> R.string.technique_xy_chain
        TechniqueId.ALS_XZ -> R.string.technique_als_xz
        TechniqueId.SUE_DE_COQ -> R.string.technique_sue_de_coq
    }

    /** One line: what kind of thing to go and look for. Shown at the first tap. */
    @StringRes
    public fun lookFor(id: TechniqueId): Int = when (id) {
        TechniqueId.NAKED_SINGLE -> R.string.look_naked_single
        TechniqueId.HIDDEN_SINGLE -> R.string.look_hidden_single
        TechniqueId.LOCKED_CANDIDATES_POINTING -> R.string.look_locked_candidates_pointing
        TechniqueId.LOCKED_CANDIDATES_CLAIMING -> R.string.look_locked_candidates_claiming
        TechniqueId.NAKED_PAIR -> R.string.look_naked_pair
        TechniqueId.HIDDEN_PAIR -> R.string.look_hidden_pair
        TechniqueId.NAKED_TRIPLE -> R.string.look_naked_triple
        TechniqueId.HIDDEN_TRIPLE -> R.string.look_hidden_triple
        TechniqueId.NAKED_QUAD -> R.string.look_naked_quad
        TechniqueId.HIDDEN_QUAD -> R.string.look_hidden_quad
        TechniqueId.X_WING -> R.string.look_x_wing
        TechniqueId.SIMPLE_COLOURING -> R.string.look_simple_colouring
        TechniqueId.XY_WING -> R.string.look_xy_wing
        TechniqueId.XYZ_WING -> R.string.look_xyz_wing
        TechniqueId.W_WING -> R.string.look_w_wing
        TechniqueId.SWORDFISH -> R.string.look_swordfish
        TechniqueId.REMOTE_PAIRS -> R.string.look_remote_pairs
        TechniqueId.UNIQUE_RECTANGLE -> R.string.look_unique_rectangle
        TechniqueId.BUG_PLUS_ONE -> R.string.look_bug_plus_one
        TechniqueId.JELLYFISH -> R.string.look_jellyfish
        TechniqueId.MULTI_COLOURING -> R.string.look_multi_colouring
        TechniqueId.X_CHAIN -> R.string.look_x_chain
        TechniqueId.XY_CHAIN -> R.string.look_xy_chain
        TechniqueId.ALS_XZ -> R.string.look_als_xz
        TechniqueId.SUE_DE_COQ -> R.string.look_sue_de_coq
    }

    /** Why it works. Shown at the last tap, once the player has seen the cells. */
    @StringRes
    public fun because(id: TechniqueId): Int = when (id) {
        TechniqueId.NAKED_SINGLE -> R.string.because_naked_single
        TechniqueId.HIDDEN_SINGLE -> R.string.because_hidden_single
        TechniqueId.LOCKED_CANDIDATES_POINTING -> R.string.because_locked_candidates_pointing
        TechniqueId.LOCKED_CANDIDATES_CLAIMING -> R.string.because_locked_candidates_claiming
        TechniqueId.NAKED_PAIR -> R.string.because_naked_pair
        TechniqueId.HIDDEN_PAIR -> R.string.because_hidden_pair
        TechniqueId.NAKED_TRIPLE -> R.string.because_naked_triple
        TechniqueId.HIDDEN_TRIPLE -> R.string.because_hidden_triple
        TechniqueId.NAKED_QUAD -> R.string.because_naked_quad
        TechniqueId.HIDDEN_QUAD -> R.string.because_hidden_quad
        TechniqueId.X_WING -> R.string.because_x_wing
        TechniqueId.SIMPLE_COLOURING -> R.string.because_simple_colouring
        TechniqueId.XY_WING -> R.string.because_xy_wing
        TechniqueId.XYZ_WING -> R.string.because_xyz_wing
        TechniqueId.W_WING -> R.string.because_w_wing
        TechniqueId.SWORDFISH -> R.string.because_swordfish
        TechniqueId.REMOTE_PAIRS -> R.string.because_remote_pairs
        TechniqueId.UNIQUE_RECTANGLE -> R.string.because_unique_rectangle
        TechniqueId.BUG_PLUS_ONE -> R.string.because_bug_plus_one
        TechniqueId.JELLYFISH -> R.string.because_jellyfish
        TechniqueId.MULTI_COLOURING -> R.string.because_multi_colouring
        TechniqueId.X_CHAIN -> R.string.because_x_chain
        TechniqueId.XY_CHAIN -> R.string.because_xy_chain
        TechniqueId.ALS_XZ -> R.string.because_als_xz
        TechniqueId.SUE_DE_COQ -> R.string.because_sue_de_coq
    }

    /** The region a hint is about, if it is about one. */
    @Composable
    @ReadOnlyComposable
    public fun where(deduction: Deduction): String? = when (deduction.houses.size) {
        0 -> null

        1 -> stringResource(R.string.hint_in_region, name(deduction.houses[0]))

        else -> stringResource(
            R.string.hint_in_regions,
            name(deduction.houses[0]),
            name(deduction.houses[1]),
        )
    }

    /** What the step actually does, in one line. */
    @Composable
    @ReadOnlyComposable
    public fun outcome(deduction: Deduction): String {
        val placement = deduction.placements.firstOrNull()
        if (placement != null) {
            return stringResource(
                R.string.hint_outcome_place,
                placement.cell / 9 + 1,
                placement.cell % 9 + 1,
                placement.digit,
            )
        }
        val digits = deduction.eliminations.map { it.digit }.distinct().sorted()
        val cells = deduction.eliminations.map { it.cell }.distinct().size
        val where = pluralStringResource(R.plurals.hint_cells, cells, cells)
        return if (digits.size == 1) {
            stringResource(R.string.hint_outcome_strike_one, digits[0], where)
        } else {
            stringResource(R.string.hint_outcome_strike_two, digits[0], digits[1], where)
        }
    }

    @Composable
    @ReadOnlyComposable
    private fun name(house: House): String = stringResource(
        when (house.kind) {
            HouseKind.ROW -> R.string.house_row
            HouseKind.COLUMN -> R.string.house_column
            HouseKind.BOX -> R.string.house_box
        },
        house.index + 1,
    )
}

---
title: Sendoku privacy policy
---

# Sendoku privacy policy

Last updated: 24 August 2026

Sendoku does not collect anything.

There is no account, no sign in, and no server. The app has no internet permission, so it
cannot send data anywhere even if it wanted to. You can check that yourself: the source is
public at [github.com/adilzhanY/sendoku](https://github.com/adilzhanY/sendoku), and the
permission list in the Play Store listing is empty.

## What the app stores on your phone

Your puzzles, your times, your progress through the lessons, and your settings. That is the
saved game, the record of which puzzles you have solved, how far through each lesson you got
and which techniques you have practised, and the choices you made on the settings screen. All
of it lives in the app's own private storage on your device, where no other app can read it.

Android's own backup service may copy that data to your Google account if you have backup
switched on, so that a new phone finds your history where you left it. That copy is between
you and Google. Sendoku never sees it. You can turn it off in your phone's backup settings.

## What the app does not do

- No analytics, no crash reporting, no telemetry.
- No advertising and no advertising identifier.
- No third party SDK that phones home.
- Nothing is shared with anyone, because there is nothing to share.

## Taking your data with you

The settings screen has an export. It writes everything above to a file you choose, through
the standard Android file picker, so a new phone can pick up where the old one left off. The
app needs no storage permission to do it: you pick the file, and the system hands the app that
one file and nothing else.

The file is json, and it is meant to be readable. Open it in any text editor and you can see
exactly what the app knows about you. Import reads a file back, merging it with whatever is
already on the phone and keeping whichever got further.

Nothing about this involves a network. There is nowhere for the file to go except where you
put it.

## Deleting your data

Uninstalling the app removes everything it stored, including the lessons you have finished
and every game you have played. There is no copy anywhere else, so there is nothing left
behind and nothing to ask us to delete.

Clearing the app's storage from Android settings does the same without uninstalling. Inside
the app, the statistics screen resets your solve history, and the settings screen has a
separate reset for the course that leaves your games alone.

## Children

Sendoku is a sudoku app. It is suitable for all ages and it is not directed at children in
the sense the Play policies use, because it collects no data from anybody, of any age.

## Changes

If this policy ever changes, the new version appears at this address with a new date at the
top. Since the app collects nothing, a change would mean the app started doing something it
does not do today, and that would be described here before it shipped.

## Contact

Questions or complaints: [adilzhan1112@gmail.com](mailto:adilzhan1112@gmail.com), or open an
issue on the [GitHub repository](https://github.com/adilzhanY/sendoku/issues).

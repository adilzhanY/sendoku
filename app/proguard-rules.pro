# Sendoku ships no reflection, no serialisation library and no dynamic class loading, so
# there is nothing R8 can shrink away that is looked up by name at runtime. The one thing
# worth keeping is the line numbers in a crash report, which are useless once R8 renames
# everything and the mapping file is the only way back.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

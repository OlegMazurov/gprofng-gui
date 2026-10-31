# JDBE

Java port of the gprofng analysis engine (C++ `gprofng/src` in binutils-gdb,
`gp-display-text -IPC`). Package `org.mazurov.jdbe`.

C++ baseline: binutils-gdb commit `a72b83ab379` (gprofng/src).

## Use

- Local analysis: the Analyzer runs the engine on a `JDBE-engine` thread of its
  own JVM over in-memory pipes (`EmbeddedProcess`), speaking the same wire protocol.
- Remote / standalone: `java -jar jdbe/build/libs/jdbe-*.jar -IPC`
  (or `er_print`-style CLI arguments).

# Notes

- One engine per JVM (static singletons); relative paths go through `Util.resolve`.

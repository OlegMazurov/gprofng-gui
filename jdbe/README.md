# JDBE

Java port of the gprofng analysis engine (C++ `gprofng/src` in binutils-gdb,
`gp-display-text -IPC`). Package `org.mazurov.jdbe`; comments tagged `CLAUDE`
cite the native file and line they were ported from.

C++ baseline: binutils-gdb commit `a72b83ab379` (gprofng/src).

## Use

- Local analysis: the Analyzer runs the engine on a `JDBE-engine` thread of its
  own JVM over in-memory pipes (`EmbeddedProcess`), speaking the same wire protocol.
- Remote / standalone: `java -jar jdbe/build/libs/jdbe-*.jar -IPC`
  (or `er_print`-style CLI arguments).

## Parity check

Record a GUI session with `SP_ANALYZER_LOG_IPC=5`, then:

    ./gradlew :jdbe:parityCheck -Plog=<ipc.log | experiment.er>

It replays the requests through a child process (twice, as a noise control) and
the embedded engine (twice) and fails on differences outside the noise set.
Rerun after engine changes. Avoid `HashMap`s keyed by identity-hashed objects
(iteration order depends on JVM history).

## Notes

- One engine per JVM (static singletons); relative paths go through `Util.resolve`.
- Experiments (`*.er`) are test data and not kept in the repo.

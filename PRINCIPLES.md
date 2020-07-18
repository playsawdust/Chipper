# Chipper Principles
Chipper is built on a few principles in an attempt to make the codebase
straightforward, extensible, discoverable, efficient, and easy to contribute to.

These principles are not set-in-stone and are generally not absolute rules;
they're just guidelines Una has followed while designing the initial code.

## No Reflection or Code Generation

### What This Means
Avoid use `java.lang.reflect.*`. Use ObjectWeb ASM and RuledClassLoader for
transparent optimizations and enforcing access rules, never to generate code
or redirect control flow. Use registries that accept functional interfaces
instead of `Class`es.

Purely internal usage of reflection that is never exposed and is straightforward
can be excused, such as that used by ProtoColor to rebuild its preset color
fields into a Map.

### Motivation
Code generation and reflection make code hard to follow and severely harms
"IDE-and-mouse" passive discovery, which is how Una learned a lot of what she
knows about programming, so it has special value to her. Additionally, Chipper
is being built for a `SecurityManager`-based sandbox, and while Chipper code
will have special rights to do anything, it's a bit scummy and reeks of "do as
I say, not as I do" to use reflection and then guard it behind a `DANGEROUS`
permission in addons that may want to do similar things.

### Corollary
Lacking reflection, we must make heavy use of functional interfaces, method
references, and lambdas, which may be strange or hard-to-grasp for those
unfamiliar with functional programming paradigms. Considering the high
inspectability and predictability of these primitives in comparison to
reflection, this is an acceptable tradeoff.

## Benchmark, Don't Guess

### What This Means
When presented with a potentially important performance concern, such as in
`FastMath`, always use microbenchmarks or preferably in-situ benchmarks to
decide what of a given set of approaches is the best.

### Motivation
There will always be points in a software project where you have to make a
decision that has bearing on performance, such as what implementation of
`sin`/`cos` to use, or what random generator algorithm. Often, these decisions
are made purely on intuition, while the actual best approach is surprising.
Modern computers have a tendency to produce surprising results if you don't
understand every single thing about how they work, due to the immense number of
layers of indirection and heuristics involved in modern development.

### Corollary
This principle has already resulted in a few surprising decisions in regards to
core math in Chipper; for example, it was found 64-bit `double` math is nearly
always faster than 32-bit `float` math, or is at least not much slower, on
modern computers (Note: with `strictfp` enabled. Without `strictfp`, `double`
math is actually 80-bit, and `float` is 64-bit.). Additionally, it was found
FastInvSqrt is not useful on modern processors. (More about this can be found
as comments in FastMath.) These decisions would not have been made this way if
it had been left up to intuition, resulting in universally reduced math
precision and a slow and poor-accuracy inv-square-root function in the engine.

Note this principle only deals with *important* performance concerns; widely
used functions that may be called frequently, such as those in FastMath.
Infrequently called or immature code can ignore performance concerns. "Premature
optimization is the root of all evil", etc.

# This document is unfinished and will be expanded upon in the future.

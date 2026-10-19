#!/usr/bin/env python3
"""Render a JMH run as a Markdown report, diffed against a base run when one is available.

Used by .github/workflows/benchmark.yml, which benchmarks the same benchmark sources
twice - once against the base branch's library sources and once against the PR's - so
the only difference between the two result files is the code being measured.
"""

import argparse
import json
import math
import os

# Below this, a difference is reported as noise even when the confidence intervals happen to miss
# each other. Two runs of identical code on one machine drift by a few percent - the second run is
# systematically slower - so anything tighter reports that drift as a regression.
NOISE_THRESHOLD = 5.0

FASTER, SLOWER, NOISE = "🟢", "🔴", "⚪"


def load(path):
    """JMH results keyed by benchmark name plus its parameters, or None if the run produced nothing
    usable - it never happened, it was killed mid-write, or it measured nothing."""
    if not path or not os.path.exists(path):
        return None

    try:
        with open(path, encoding="utf-8") as file:
            results = json.load(file)
    except (json.JSONDecodeError, UnicodeDecodeError):
        return None

    return {(entry["benchmark"], tuple(sorted(entry.get("params", {}).items()))): entry for entry in results} or None


def score(entry):
    metric = entry["primaryMetric"]
    # JMH writes an unavailable error as the *string* "NaN", so this can't skip the conversion.
    error = float(metric.get("scoreError", "NaN"))

    return metric["score"], (None if math.isnan(error) else error), metric["scoreUnit"]


def significant(value, digits):
    """Fixed-point with `digits` significant digits - JMH scores span microseconds to milliseconds,
    and %g would drop half of them into scientific notation."""
    if value == 0:
        return "0"

    precision = digits - 1 - int(math.floor(math.log10(abs(value))))
    return f"{round(value, precision):.{max(0, precision)}f}"


def render_score(entry):
    value, error, unit = score(entry)
    if error is None:
        return f"{significant(value, 4)} {unit}"

    return f"{significant(value, 4)} ± {significant(error, 2)} {unit}"


def render_change(base, pr):
    """The PR's score relative to the base, or a marker when the benchmark is new."""
    if base is None:
        return "🆕 new"

    base_value, base_error, _ = score(base)
    pr_value, pr_error, _ = score(pr)
    if base_value == 0:
        return f"{NOISE} n/a"

    # Every benchmark here is avgt, but a throughput mode would invert what "better" means.
    lower_is_better = pr["mode"] != "thrpt"
    delta = (pr_value - base_value) / base_value * 100
    faster = (delta < 0) == lower_is_better

    overlaps = base_error is not None and pr_error is not None and \
        abs(pr_value - base_value) <= base_error + pr_error
    if overlaps or abs(delta) < NOISE_THRESHOLD:
        return f"{NOISE} {delta:+.1f}%"

    return f"{FASTER if faster else SLOWER} {delta:+.1f}% {'faster' if faster else 'slower'}"


def render_setup(entry):
    """The run's JMH configuration, read back from the results so it can't drift from core/build.gradle."""
    return (f"JDK {entry['jdkVersion']} · {entry['forks']} fork × "
            f"({entry['warmupIterations']} × {entry['warmupTime']} warmup + "
            f"{entry['measurementIterations']} × {entry['measurementTime']} measurement)")


def render_params(params):
    if len(params) == 1:
        return f"`{params[0][1]}`"

    return ", ".join(f"`{key}={value}`" for key, value in params)


def params_header(params):
    """A single @Param names its own column, anything else gets a generic one."""
    if len(params) == 1:
        return params[0][0].replace("_", " ").capitalize()

    return "Params"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pr", required=True, help="JMH JSON produced from the pull request's sources")
    parser.add_argument("--base", help="JMH JSON produced from the base branch's sources; omitted or missing when that run failed")
    parser.add_argument("--refs", default="", help="The commits being compared, shown above the table")
    args = parser.parse_args()

    pr = load(args.pr)
    if not pr:
        print("<!-- jmh-benchmark -->\n## Benchmarks\n\nThe benchmark run produced no results.")
        return

    base = load(args.base)

    lines = ["<!-- jmh-benchmark -->", "## Benchmarks", ""]
    if base is None:
        lines += [
            "The base branch produced no results - it failed to build, or the benchmarks don't compile against "
            "it, which is expected on the pull request that introduces or reworks them. See the job log for which. "
            "Absolute numbers only.",
            "",
        ]
    lines += [" · ".join(filter(None, [args.refs, render_setup(next(iter(pr.values())))])), ""]

    columns = ["Benchmark", params_header(next(iter(pr))[1])]
    if base is not None:
        lines += ["| " + " | ".join(columns + ["Base", "This PR", "Change"]) + " |", "| --- | --- | ---: | ---: | :--- |"]
    else:
        lines += ["| " + " | ".join(columns + ["This PR"]) + " |", "| --- | --- | ---: |"]

    improved = regressed = added = 0
    # Insertion order: JMH groups by benchmark - alphabetically, not in declaration order - and within
    # a benchmark keeps the @Param values in the order they're declared.
    for key, entry in pr.items():
        name = key[0].rsplit(".", 2)
        cells = [f"`{name[-2]}.{name[-1]}`", render_params(key[1])]

        if base is None:
            cells.append(render_score(entry))
        else:
            baseline = base.get(key)
            change = render_change(baseline, entry)
            improved += change.startswith(FASTER)
            regressed += change.startswith(SLOWER)
            added += baseline is None
            cells += [render_score(baseline) if baseline else "—", render_score(entry), change]

        lines.append("| " + " | ".join(cells) + " |")

    if base is not None:
        lines += [
            "",
            f"**{improved} faster, {regressed} slower**, {len(pr) - improved - regressed - added} unchanged"
            + (f", {added} new." if added else "."),
            "",
            f"A change counts as real only when it exceeds {NOISE_THRESHOLD:g}% *and* the two confidence intervals "
            "don't overlap. Both runs share one hosted runner and the base run always goes second, so treat "
            "anything close to the threshold as noise rather than a result.",
        ]

    print("\n".join(lines))


if __name__ == "__main__":
    main()

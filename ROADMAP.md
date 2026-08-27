# PropStack Roadmap

## Release status

The source tree declares version `1.0.0`, but `v1.0.0` has not been tagged or
published as a GitHub release. Until the release checklist below is complete,
the source version is an **unpublished release candidate**. The latest published
release remains unaffected by this declaration.

This file is the project-level release policy. The background and detailed
backlog remain in [DD-009](docs/decisions/DD-009-1.0-remaining-tasks.md).

## v1.0.0 release checklist

- [x] GitHub Actions runs `mvn verify` successfully on pushes and pull requests.
- [x] `README.md` and `README.ja.md` show the CI badge.
- [ ] Every public API has complete, doclint-clean Javadoc.
- [x] `PropStack.defaultSources()` is documented in the getting-started guide
      and API cookbook.
- [ ] Immediately before release, confirm there are no open P0 or P1 bugs.
- [ ] Create the `v1.0.0` tag and GitHub release only after every preceding gate
      is complete.

The unchecked release gates are intentionally not part of issue #12. They must
be completed and verified before publishing; declaring `1.0.0` in `pom.xml`
alone does not publish the release.

## v1.0 API freeze

The v1.0 API is frozen at the repository's public Java boundary: public types
and their public constructors, methods, fields, record components, and inherited
public contracts in the `org.unlaxer.propstack` package. Before the v1.0.0 tag,
changes to this boundary must be backward-compatible and additive. Removing or
renaming API, narrowing accessibility, changing signatures, or intentionally
changing documented behavior is deferred to a future major version.

`protected`, package-private, and `private` implementation details are outside
this freeze unless they implement an inherited public contract. Tests, examples,
and documentation are not API, but they must continue to describe the frozen
public behavior accurately.

## After v1.0.0

The non-blocking evaluations listed in DD-009—such as all-source tracing,
anonymous key catalogs, factory naming, and Java 17 compatibility—remain
candidates for compatible 1.x additions or a later major release. Each change
requires its own issue and acceptance criteria; it does not delay v1.0.0 unless
it becomes necessary to satisfy the release checklist.

# Contributing

## Tests

All units of functionality should have tests. This is a goal, and will
eventually be a requirement. Code should be written to make testing easier,
using such techniques as loose coupling and interfaces (cf. protocols in
clojure, services and dependency injection in angular). Avoid depending on the
real world (db, http, io) where you don't absolutely need it.

Run the tests as follows:

```
lein test
```

## Organisation

Small is beautiful - small files containing a single unit of functionality is
best. Split things up when they get too big to understand easily, compose
functionality when something does too many things. No function should be more
than 50 lines.

### Clojure - server functionality

Separate code by its functionality. Persistence and retrieval goes in resources, routing
in routes, etc. None of these layers should depend on each other except through
protocols.

### Coffeescript - client functionality

Prefer skinny controllers, and controllers as classes where possible. Avoid
touching the scope where it can be avoided.

## Code Style

Developers and contributors should ensure that their code adheres to the
standard code styles for their chosen languages. We follow
[bbatsov][clojure-style-guide] for clojure and
[polarmobile][coffee-script-style-guide] for coffee-script, and
[johnpapa][angular-style-guide] for angular.

We are pretty far off some of these right now (especially the angular rules),
but that does not mean that new code should not adhere to them - always add code
that is better that what is already there.

Where possible these standards should be automatically enforced.

[clojure-style-guide]: https://github.com/bbatsov/clojure-style-guide
[coffee-script-style-guide]: https://github.com/polarmobile/coffeescript-style-guide
[angular-style-guide]: https://github.com/johnpapa/angular-styleguide

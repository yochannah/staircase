# staircase

[![Build Status][travis-badge]][ci]

The webapp that serves the step-based data-flow interface to the
[InterMine](http://www.intermine.org) system.

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

To run the tests, you will need a test database (by default named
`staircase-test`) and be able to access it. It is recommended you
add configuration values to `~/.lein/profiles.clj` for these
properties, but this can also be configured in environment
variables - see [Configuration][].

    lein test

## Configuration

The following configuration values are recognized, here presented
as you might configure them in a `profiles.clj` file:

```clojure
{
    :user {
        :env {
            :db-user "DB-USER"     ;; The db user (optional)
            :db-password "DB-PASS" ;; The db-password (optional)
            :db-subname "//host/dbname" ;; The db host and name
                                        ;; see profiles.clj for
                                        ;; default values.
        }
    }
}
```

These values can be configured through environment variables (see
[Environ][environ] for details), eg:

```bash
DB_USER=$PSQL_USER
```

## License

Copyright © 2014 Alex Kalderimis and InterMine

[travis-badge]: https://travis-ci.org/alexkalderimis/staircase.svg?branch=master
[ci]: https://travis-ci.org/alexkalderimis/staircase
[environ]: https://github.com/weavejester/environ

# staircase

[![Build Status][travis-badge]][ci]

The webapp that serves the step-based data-flow interface to the
[InterMine](http://www.intermine.org) system.

## Quick Start

Assuming all dependencies installed and the DB is configured (or you are using the default
user DB).

```sh
bower install
lein ring server
```

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed, as well as (at least for now
until I get `lein-bower` set up) [bower][2], and thus [node.js][3].

Before running you should must install the runtime javascript dependencies. This is
currently done with the following commands:

```sh
# Installs bower.
npm install
# Installs runtime dependencies.
./node_modules/bower/bin/bower install
```

## Configuration

You must configure the database and add a secrets file before you can run
the application.

The secrets file is needed to sign authentication tokens. You can use private/public
key peirs, or simply provide a key phrase for HMAC hashing. In the latter case, add
a `resources/secrets.edn` file which defines a key-phrase:

```clj
{
  :key-phrase "some utterly random and unguessable key phrase"
}
```

The database and the toolset are configured using [Environ][environ], which
has a flexible configuration system. Most settings are to be found in `profiles.clj`.

It is recommended that DB authentication settings do not live in source controlled files,
but should instead be configured through environment variables or in the user config
(`~./lein/profile.clj`).

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

## Running

To start a web server for the application, run:

    lein ring server

To run the tests, you will need a test database (by default named
`staircase-test`) and be able to access it. It is recommended you
add configuration values to `~/.lein/profiles.clj` for these
properties, but this can also be configured in environment
variables - see [Configuration][].

    lein test

## License

Copyright © 2014 Alex Kalderimis and InterMine

[travis-badge]: https://travis-ci.org/alexkalderimis/staircase.svg?branch=master
[ci]: https://travis-ci.org/alexkalderimis/staircase
[environ]: https://github.com/weavejester/environ
[1]: https://github.com/technomancy/leiningen
[2]: http://bower.io/
[3]: http://nodejs.org/

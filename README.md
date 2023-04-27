# keg-party

`tap>`, party style!

## Usage

### Launch the server

Start your party by launching a server with one of the following options:

- `clj -X keg-party.main/run` from the cloned project
- Build an uberjar with `clojure -X:uberjar` then run it with `java -jar keg-party.jar`

By default, the server will run at `http://localhost:3333`. You can change these defaults as described in the configuration section below.

### Connect your client

Then invite all your friends to the party by doing the following:

- Add `keg-party` as a dependency to your project. The suggested way is to add it to your `~/.clojure/deps.edn` file like so:

```clojure
 :deps {org.clojure/clojure {:mvn/version "1.10.3"}
        com.markbastian/keg-party
        {:git/url "https://github.com/markbastian/keg-party"
         :sha     "2b9af40112378f371dece94f063fa2cb7566ca2e"}}
```

- Connect the tap target. Note that your env vars need to be configured correctly if you aren't using the defaults.
  - The manual way is to invoke the following commands in sequence:
    - `(require '[keg-party.clients.rest-client :as kprc])`
    - `(kprc/tap-in!)` or `(kprc/tap-in! "username")`
  - For an automated experience, do the following:
    - Create a local dev or user profile (e.g. add `:dev {:extra-paths ["dev"]}` to your `~/.clojure/deps.edn` `:alias`es)
    - In that profile's extra paths, add a ns that looks something like this:

```clojure
(ns user
  (:require [keg-party.clients.rest-client :as kprc]))

(println "KEG PARTY TAPPING IN")
(kprc/tap-in!)
```

Ensure that this profile is active when you launch your REPL. When you do so, this code will be run and you are good to go.

- Test it out with by doing something like this:

```clojure
(tap> {:best-drink-ever     :diet-dew
       :the-next-best-thing :diet-dr-pepper})
```

- Head on over to your party server and see the data!

## Configuration

The following environment variables may be set:

- `KEG_PARTY_HOST`, defaults to http://localhost
- `KEG_PARTY_PORT`, defaults to 3333

Protip: Launch using Clojure deps prefixed with env vars like so:

```clojure
KEG_PARTY_PORT=3333 KEG_PARTY_INCLUDES_REGEX=my-project-ns.* clj -X keg-party.main/run
```

## Misc

Run the project's tests (they'll fail until you edit them):

    $ clojure -X:test:runner

Build a deployable jar of this library (Still needs work):

    $ clojure -X:jar

## TODOs
- [ ] Persistence
- [ ] Add args for deps so we can easily launch with -X args
- [ ] Add client profile so you can just add the profile and connect the tap at launch
- [ ] Documentation with screenshots

## License

Copyright © 2023 Mark Bastian

Distributed under the Eclipse Public License version 1.0.

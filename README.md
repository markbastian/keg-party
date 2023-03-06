# keg-party

`tap>`, party style!

## Usage

Start your party by launching a server with:

- `clj -X keg-party.main/run`
- Build an uberjar with `clojure -X:uberjar` then run it
  with `java -jar keg-party.jar`
- If you accept the defaults, it will be running at `http://localhost:3000/`

Then invite all your friends to the party by doing the following:

- Add this project to your `deps.edn` file as a dependency, like so:

```clojure
 :deps {org.clojure/clojure {:mvn/version "1.10.3"}
        com.markbastian/keg-party
        {:git/url "https://github.com/markbastian/keg-party"
         :sha     "88c57ab4f0db6b78dcf5d4d307fa24ef5b83d817"}}
```

- In your repl, do the following:
    - `(require '[keg-party.clients.rest-client :as kprc])`
    - `(kprc/tap-in!)` or `(kprc/tap-in! "username")` where username is whatever
      username or id you want to show up in the tap stream. When no username is
      provided the env user is used.
    - Test it out with by doing something like this:

```clojure
(tap> {:best-drink-ever     :diet-dew
       :the-next-best-thing :diet-dr-pepper})
```

- Head on over to your party server and see the data!

## Configuration

The following environment variables may be set:

- `KEG_PARTY_HOST`, defaults to http://localhost
- `KEG_PARTY_PORT`, defaults to 3000

## Misc

Run the project's tests (they'll fail until you edit them):

    $ clojure -X:test:runner

Build a deployable jar of this library (Still needs work):

    $ clojure -X:jar

## TODOs
- [ ] Persistence
- [ ] Add args for deps so we can easily launch with -X args
- [ ] Add client profile so you can just add the profile and connect the tap at launch

## License

Copyright © 2023 Mark Bastian

Distributed under the Eclipse Public License version 1.0.

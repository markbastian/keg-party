# keg-party

`tap>`, party style!

## Usage

### Launch the server

Start your party with this easy one-liner:

#### The super easy way

```shell
clojure -Sdeps '{:deps {com.github.markbastian/keg-party
                  {:git/url "https://github.com/markbastian/keg-party"
                  :sha     "61196367e42442ebe358752bec0eb258e18be08d"}}}' \
                   -X keg-party.main/run
```

By default, the server will run at `http://localhost:3333`. You can change these defaults as described in the configuration section below.

For several other options, including building an uberjar, go to the [Keg Party Server page](./doc/server.md).

### Connect your client

Invite all your friends to the party by adding `keg-party-client.jar` as a dependency to your project.

[![Clojars Project](https://img.shields.io/clojars/v/com.github.markbastian/keg-party-client.svg)](https://clojars.org/com.github.markbastian/keg-party-client)

Configure your environment with the following environment variables:
- `KEG_PARTY_HOST`, defaults to http://localhost
- `KEG_PARTY_PORT`, defaults to 3333
- `KEG_PARTY_USERNAME`, defaults to `(or (env :keg-party-username) (env :user))`
- `KEG_PARTY_PASSWORD`, no default. This is your password from the setup page.
  - This is the only env var that you _must_ set if you aren't using the defaults.

Once you've added the client jar and configured the environment, connect the tap target by invoking:

```clojure
(do
  (require '[keg-party.clients.rest-client :as kprc])
  (kprc/tap-in!))
```

For an automated experience, do the following:
- Create a local dev or user profile if you don't already have one
  - (e.g. add `:dev {:extra-paths ["dev"]}` to your `~/.clojure/deps.edn` `:aliases`)
- In that profile's extra paths, add a ns that looks something like this:

```clojure
(ns user
  (:require [keg-party.clients.rest-client :as kprc]))

(println "KEG PARTY TAPPING IN")
(kprc/tap-in!)
```

If you already have such a namespace, just add the above to it.

Ensure that this profile is active when you launch your REPL. When you do so, this code will be run and you are good to go.

Test your config you by doing something like this:

```clojure
(tap> {:best-drink-ever     :diet-dew
       :the-next-best-thing :diet-dr-pepper})
```

Head on over to your party server and see the data!

## The initial experience

When you connect to the server, you'll be directed to a login page:

![login.png](doc/login.png)

If you haven't created an account, follow the "Sign up" link to do so:

![signup.png](doc/signup.png)

Whether you are logging in to an existing account or creating a new one, you'll be immediately directed to the tap feed once you are signed in:

![feed.png](doc/feed.png)

Start playing around and have some fun!

## License

Copyright © 2023 Mark Bastian

Distributed under the Eclipse Public License version 1.0.

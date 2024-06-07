# Keg Party Server

## Launch it!

### Method 1: Command-line dependency with tools.deps

```shell
clojure -Sdeps '{:deps {com.github.markbastian/keg-party
                  {:git/url "https://github.com/markbastian/keg-party"
                  :sha     "61acf0a731df04fc55af564ce939cffbe544e255"}}}' \
                   -X keg-party.main/run
```

Yep, that's it!

### Method 2: deps.edn as an installed dep

Similar to the above, but with the dependency added to a `deps.edn` file (e.g. `~/.clojure/deps.edn`).

1. Create an alias (e.g. `:keg-party`) in your deps file.
2. `clj -X:keg-party keg-party.main/run`

Here's how you might add this alias to your `~/.clojure/deps.edn` file:

```clojure
{:aliases
 ;; Note: You may have many other aliases in this section already.
 ;; Jus add the `:keg-party` entry.
 {:keg-party
  {:extra-deps
   {'com.github.markbastian/keg-party
    {:git/url "https://github.com/markbastian/keg-party"
     :sha     "61acf0a731df04fc55af564ce939cffbe544e255"}}}}}
```

### Method 3: Run the -X target from the project

1. `git clone https://github.com/markbastian/keg-party.git`
2. `cd keg-party`
3. `clj -X keg-party.main/run`

### Method 4: Run a standalone uberjar

1. `git clone https://github.com/markbastian/keg-party.git`
2. `cd keg-party`
3. Build the jar with `clj -T:build server-uberjar`
4. Run the jar with `java -jar target/keg-party-${LATEST_VERSION}-standalone.jar`

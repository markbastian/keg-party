# TODOs

Features:
- [X] Persistence
- [ ] Add admin profile concept so that only admins can see client tabs, for example
- [X] Expose favorites
  - [X] Create favorites view (or do something else with it)
  - [X] Add "nuke all non favorites" option
- [ ] User spaces/channels
- [ ] Collaborative tap comments
- [X] Drill-down/explore individual tap data
- [ ] When you sign up, show a page with a hint (copyable code?) for how to setup your tap target
- [X] Push client code to clojars
  - Maybe make a separate repo
- [ ] Can I inject the client via a [java agent](https://dgopstein.github.io/articles/clojure-javaagent/)?

Architecture & Tech debt:
- [X] Create protocols for db ops to hide implementation (e.g. users and taps)
- [ ] Make events multimethods as in `dispatch-command`
  - `generate-event` or `process-event`
  - Consider - do we combine `htmx-notifications` into `events`?
    - Will there be other notification types?
    - Do we care at this point?
- [ ] Tests
- [ ] Rename `generic` to something like `ezcmd` or something
- [ ] Make command-event bridge async
  - Queue interface
    - Use core.async for now
    - Sets it up for something like SQS in the future
- [ ] Tied to the above, do some sort of event log maybe
- [ ] With an event log we could batch notifications so we don't hammer the UI with too many taps at once
- [X] Refactor `migrations` into something that isn't a terrible name
- [X] BUG - When you go to http://localhost:3333/ in a new incognito window you get
  - `HTTP ERROR 500 Cannot invoke "java.lang.CharSequence.length()" because "this.text" is null`
  - Why?
  - If you go to `/login` it gets fine. It should redirect and, in fact, does later on.
- [ ] Refactor repository signatures to always take a partial object map of tap, user, etc.
- [X] Put pages into a package with one page per actual page
- [ ] Clean up inline js fragments and maybe put them in an actual js file.

Bugs:
- [X] 3 starred taps, then add like 20 unstarred taps. Delete the unstarred taps.
  - The starred don't jump to the top, you have to refresh
  - How do I fix this?
  - Probably just a full swap since this is so destructive (Yep)
    - Instead of nuking the unstarred on the FE just reload with 10 starred
- [ ] Drilling down to non-parseable entities blows up
- [X] Figure out how to serve resources with clj -X execution

Client QoL:
- [ ] Add args for deps so we can easily launch with -X args
- [ ] Add client profile so you can just add the profile and connect the tap at launch

Docs:
- [X] Documentation with screenshots
- [ ] Tutorials

Questions:
- In SQLite can I delete an entry with a FK pointing to it?
  - It's a stupid pragma thing in SQLite

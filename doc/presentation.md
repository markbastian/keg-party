# Intro to keg-party

Crib sheet used for Clojure Visual Tools meetup 2024-06-07

## Preconfig
- Have keg-party up and running (IDE and site)
- Have keg-party-demo up and running
- Have my deps.edn open in VS Code

# Demo

- User creation
- Logout/login
- Note that back/forward work just fine
- `markbastian.keg-party-demo`
  - tap the different forms
  - Make a _lot_ of taps all at once
  - Refresh
  - Doom scroll
  - Copy
  - Drill
  - Delete
  - Favorite
  - Bulk delete
  - Sessions (more for debug)
  - Channels
    - NOTE: Ugliness is just me needing to give some TLC to that part of the UI
    - Has nothing to do with HTMX
    - See who is in what channel
    - Create/Change channels
    - Delete empty channels
    - Content is isolated to its channel as expected

# Party Demo

The original idea of keg-party was a hosted tap server like a chat server, but for chats. This hasn't really materialized only because I don't have a server and I'm getting plenty of use out of it as-is, but it _should_ work.

## Exposing the server

Please nobody use this subdomain, so we can keep it convenient 🙂:
- `lt --subdomain keg-party --port 3333`
- Will create: https://keg-party.loca.lt

Local tunnel can be laggy, so we'll see how performance goes

## Go to the sample project

# Architecture

- `keg-party` is a completely server-rendered tool
  - There is almost no js
- BE
  - The system follows a command - event pattern
    - Nearly every action is a command that is sent to the server via post or ws
    - `keg-party.web/post-message-handler` just dispatches the posted command
  - A simple architecture around this is in the `generic` package
    - `generic.client-api` has simple protocols for client management with a websocket implementation
    - `generic.commands` has a single `dispatch-command` multimethod taking a context and the command data
    - `generic.web` has some pre-baked handlers for websockets
    - `generic.ws-handlers` contains the actual code to handle client management and command dispatch
    - You could easily extend this to any application that sends commands over ws
  - The implementation is in the `keg-party` package
    - `keg-party.commands` does all of the command handling
      - Generally this means doing any sort of needed db operations then sending off an event
    - `keg-party.events` are actions that send responses back to clients
      - For `keg-party`, this almost always means generating hiccup and sending it back to the client
  - Storage
    - Sqlite - Easy and works
    - I use the repository pattern with a protocol for all db actions
- FE (It's actually BE)
  - This is where the HTMX fun is
  - All the handlers are in `keg-party.web`
  - Routes are easy to navigate
  - Each route is basically a hiccup fragment rendered as html
  - `keg-party.pages` contains top-level pages
  - You can generally preview a page with just:
    - `(->> (signup/signup-page {}) wrap-as-page (spit "signup.html"))`
  - Everything is simple html + styling (mostly bootstrap)
  - Very little code `cloc src/keg_party/pages`
- [HTMX](https://htmx.org/)
  - htmx is a very simple JS library (not a framework) that does the following:
    - Allows all http verbs from any element (vs. GET on <A> and POST on <form>) and event
    - Allows any element to be the target of a hypermedia result (rather than full page update)
  - It prefers locality of behavior (LOB) over abstraction
    - Typical SPA
      - index (contains react components)
      - model (contains data)
      - some sort of effects/co-effects state management system
      - state is frequently fully clone on FE
      - You need to know all the components and frameworks
      - testing is very hard - usually requires mocking E2E state with MSW or something
    - HTMX
      - Server renders HTML
      - User interacts with HTML
      - Server processes event
      - Repeat
      - You only need to know HTML and CSS
      - Testing is easy
        - Test all business logic on the BE
        - Test rendering by mocking request and parsing response with something like [hickory](https://github.com/clj-commons/hickory)
          - Since HATEOAS, the state is in the hypermedia, so it's just data
  - In general, htmx makes heavy use of `hx-swap` and `hx-target` to simply replace some element with the result of an action
    - This is the case for the non-feed pages in keg-party
    - Since `keg-party.pages.feed` was meant to be collaborative, it is a bit different
      - The `ws` extension is used
      - `ws-send` is used to send most messages to the backend rather than `post`
      - The events generally add `hx-swap-oob` to responses and broadcast these via ws
  - I could give a whole presentation on htmx, but:
    - It is **far** simpler than any SPA framework
    - I've found that you can create web apps much faster than framework-based alternatives
    - Easier to reason about and test

# TODOs
- The event system should really be dispatched off of the db (or even an event bus)
  - However, this is a simple tool, so I don't have much desire to overengineer it
  - I might do a core-async listener on an event table in the db at some point
- key-party works fine, so all of the TODOs are kind of "when I get to it" items

# Questions

- Why do you use multimethods in some places and protocols in others
  - If I'm dispatching on data (events), I use a multimethod
  - If I'm implementing a system component (wrapping some stateful thing), I use a protocol
  - Seems to work pretty well so far
- Why not
  - [portal](https://github.com/djblue/portal)
  - [REBL](https://docs.datomic.com/other-tools/REBL.html)
  - [reveal](https://github.com/vlaaad/reveal)
- They are all excellent and probably better
- It's fun to develop your own stuff
- I wanted an excuse to do some HTMX
- I like the idea of a standalone process for `tap>` output
- I like it

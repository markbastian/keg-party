# Architecture

- All mutations enter as commands
- Non-persistent changes (e.g. a local user view) are regular htmx localized changes
- Maybe
  - If feedback/ack is required, submit command via hx-post
  - Fire and forget will be through ws
- All stateful objects are wrapped in records and exposed as protocols - this is clean
  - stateful: I/o, disk, db, sockets
  - NOT serializable as data
- All data polymorphism uses multimethods

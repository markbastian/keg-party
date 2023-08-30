# Architecture

- All mutations enter as commands
- All stateful objects are wrapped in records and exposed as protocols - this is clean
  - stateful: I/o, disk, db, sockets
  - NOT serializable as data
- All data polymorphism uses multimethods

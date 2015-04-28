(ns staircase.protocols)

(defprotocol Resource
  (exists? [this id] "Does the thing exist?")
  (get-all [this] "Get all the things")
  (get-one [this id] "Get one of the things")
  (update [this id doc] "Update a thing")
  (delete [this id] "Destroy a thing")
  (create [this doc] "Create a new thing"))

(defprotocol SubindexedResource
  (get-child [this id child-id] "Get a child item.")
  (delete-child [this id child-id] "Destroy a child item, returning its id.")
  (add-child [this id child] "Add a child document, returning its id."))

(defprotocol Searchable
  (get-where [this constraint] "Find all the things matching the constraint"))


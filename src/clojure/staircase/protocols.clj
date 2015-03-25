(ns staircase.protocols)

(defprotocol Resource
  (exists? [this id] "Does the thing exist?")
  (get-all [this] "Get all the things")
  (get-one [this id] "Get one of the things")
  (update [this id doc] "Update a thing")
  (delete [this id] "Destroy a thing")
  (create [this doc] "Create a new thing"))

(defprotocol SubindexedResource
  (delete-child [this id child-id] "Destroy a child item")
  (add-child [this id child] "Add a child document"))

(defprotocol Searchable
  (get-where [this constraint] "Find all the things matching the constraint"))


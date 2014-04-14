(ns staircase.resources.schema)

;; Question: do we have any need of a user table? Currently using
;; persona means we don't need to record user entities on this end, do we?
;;
;; Currently we delegate user authentication to third party identity services
;; (persona), identifying users by email address alone.

(def histories
  {:histories
   [ [:id :uuid "primary key"]
     [:title "varchar(1024)"]
     [:description :text]
     [:owner "varchar(1024)"] ;; Should be long enough for emails...
    ]})

(def history-step
   {:history_step
    [ [:history_id :uuid]
      [:created_at :timestamp]
      [:step_id :uuid] ] })

;; Question: should steps also reference their owner? On one hand, since steps
;; are immutable, it should be reasonable to share them between histories, even
;; those owed by different users.
(def steps
  {:steps
   [ [:id :uuid :primary :key]
     [:title "varchar(1024)"]
     [:tool "varchar(1024)"]
     [:data :text] ]}) ;; ideally should use the json data type.

(def sessions
  {:sessions [ [:id :uuid :primary :key]
              [:data :text]
              [:valid_until "timestamp with time zone"] ]} )

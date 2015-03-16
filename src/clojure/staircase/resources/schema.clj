(ns staircase.resources.schema)

;; Question: do we have any need of a user table? Currently using
;; persona means we don't need to record user entities on this end, do we?
;;
;; Currently we delegate user authentication to third party identity services
;; (persona), identifying users by email address alone.

;; Fixed length string data type, as opposed to string data.
;; The type to use for things like email addresses, uris, names, etc.
(def string "varchar(1024)") 

;; Unbounded character data type. Ideally this should actually be something
;; like json - but that will be another day
(def data :text)

(def owner-column [:owner string "NOT NULL"])

(def histories
  {:histories
   [ [:id :uuid "primary key"]
     [:title string]
     [:created_at "timestamp with time zone"]
     [:description :text]
     owner-column
    ]})

;; Link table, allowing many-many relationships between steps and histories (histories
;; have many steps, steps can be part of more than one history).
(def history-step
   {:history_step
    [ [:history_id :uuid]
      [:created_at "timestamp with time zone"]
      [:step_id :uuid] ] })

;; Question: should steps also reference their owner? On one hand, since steps
;; are immutable, it should be reasonable to share them between histories, even
;; those owed by different users.
(def steps
  {:steps
   [ [:id :uuid :primary :key]
     [:title string]
     [:tool string "NOT NULL"] ;; No point in having steps with no associated tool.
     [:stamp string] ;; Optional stamp for identifying when this tool was run - context dependent.
     [:data data] ]}) ;; The input payload for this step.

(def sessions
  {:sessions [ [:id :uuid :primary :key]
              [:data data] ;; The session data (currently stored as edn)
              [:valid_until "timestamp with time zone"] ]} )

(def services
  {:services
   [[:id :uuid :primary :key ]
    [:name string "NOT NULL" ]
    [:root string "NOT NULL" ]
    [:token data ]                            ;; Use this token to access data.
    [:valid_until "timestamp with time zone"] ;; must refresh access token after this time
    [:refresh_token data]                     ;; Use this token to get a new access token.
    owner-column
    [:UNIQUE "(owner, root)"]
    [:UNIQUE "(owner, name)"]]})

(def toolsets
  {:toolsets
   [[:id :uuid :primary :key]
    [:active :boolean]
    owner-column]})

(def tool-config
   {:toolconfs
    [[:name string :primary :key]
     [:toolset :uuid :REFERENCES :toolsets "(id)"]
     [:index :integer]
     [:frontpage :boolean]
     [:data data]
     [:UNIQUE "(toolset, index)"]]})
      

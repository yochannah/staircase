(ns staircase.views
  (:require [hiccup.form :refer :all]
            [clojure.string :as string]
            [hiccup.def :refer (defelem)]
            [persona-kit.view :as pv])
  (:use [hiccup.core :only (html)]
        [hiccup.page :only (html5 include-css include-js)]
        [hiccup.element :only (link-to unordered-list javascript-tag mail-to)]))

(defn- with-utf8-charset [[tag attrs]]
  [tag (assoc attrs :charset "utf-8")])

(def ^:dynamic require-js "/vendor/requirejs/require.js")

;; TODO - read these from conf
(def repo "https://github.com/alexkalderimis/staircase")

(def contact "alex@intermine.org")

(defn- entry-point [ep]
  (->> (include-js require-js)
       (map (fn [[tag attrs]] [tag (assoc attrs :data-main ep)]))
       (first)))

(def vendor-scripts ["jquery/dist/jquery.min.js"])

(def button-style :blue) ;; :orange or :dark

(defn login []
  (-> (pv/sign-in-button {:ng-click "persona.request()"} button-style)
      (update-in [1 :class] str " navbar-btn")
      (update-in [2 1] (constantly "Sign in/Sign up"))))

(defn logout []
  (-> (pv/sign-in-button {:ng-click "persona.logout()"} button-style)
      (update-in [1 :class] str " navbar-btn")
      (update-in [2 1] (constantly "Sign out"))))

(declare search-form nav-list)

(def home-links [["home" "home" "/"]
                 ["about intermine" "info" "/about"]
                 ["options" "cog" {:ng-click "showOptions()"}]])

(defn header []
  [:headroom
    {:offset 205
     :tolerance 5
     :classes "{initial:'animated',
                pinned:'slideDown',
                unpinned:'slideUp',
                top:'headroom--top',
                notTop:'headroom--not-top'}"}
  [:div.navbar.navbar-inverse.navbar-custom.navbar-default.navbar-fixed-top {:role "navigation"}
   [:div.container-fluid

    [:div.navbar-header ;; The elements to always display.
     [:button.navbar-toggle {:data-toggle "collapse" :ng-click "showHeaderMenu = !showHeaderMenu"}
      [:span.sr-only "Toggle navigation"]
      (for [_ (range 3)]
        [:span.icon-bar])]
     [:div {:ng-controller "BrandCtrl" :dropdown "dropdown"}
      [:a.navbar-brand.dropdown-toggle
        [:span.app-name "Steps"]] ;; Make configurable?
      (unordered-list {:class "dropdown-menu"}
                      (for [[title icon path] home-links]
                        (let [icon [:i.fa.fa-fw {:class (str "fa-" icon)}]]
                          (if (string? path)
                            (link-to path icon " " title)
                            (link-to path "" icon " " title)))))]]

    [:div.collapse.navbar-collapse {:ng-class "{in: showHeaderMenu}"};; Only show if enough space.

     [:p.navbar-text.navbar-right {:ng-show "auth.loggedIn"}
      "Signed in as {{auth.identity}}"]

     (nav-list)

     (search-form)

     ]]]])

(def search-input
  [:div.input-group
   {:ng-controller "QuickSearchController"}
   [:input.form-control {:ng-model "searchTerm" :placeholder "enter a search term"}] ;; TODO: Definitely make placeholders injectable.
   [:span.input-group-btn
    [:button.btn.btn-primary
     {:type "submit" :ng-click "startQuickSearchHistory(searchTerm)"}
     "Search "
     [:i.fa.fa-search]]]])

(defn search-form []
  [:form.navbar-form.navbar-left {:role "search"} search-input])

(defn nav-list []
  [:ul.nav.navbar-nav.navbar-right {:ng-controller "AuthController"}
   [:li.dropdown
    [:a.dropdown-toggle
     "Start " [:b.caret]]
     [:ul.dropdown-menu
      [:li {:ng-repeat "tool in startingPoints"}
           [:a {:href "/starting-point/{{tool.ident}}/{{tool.args.service}}"}
               "{{tool.heading}}"]]]]
   [:li.dropdown
    [:a.dropdown-toggle
     "get in touch! " [:b.caret]]
    (unordered-list {:class "dropdown-menu"}
                    [(mail-to contact
                              [:span
                               [:i.fa.fa-envelope-o]
                               " Contact"])
                     (link-to repo
                              [:i.fa.fa-github]
                              " View on github")
                     (link-to (str repo "/issues")
                              [:i.fa.fa-warning]
                              " Report a problem")])]
   [:li {:ng-show "auth.loggedIn"} (logout)]
   [:li {:ng-hide "auth.loggedIn"} (login)]
   ])


(defn footer []
  [:section.footer {:ng-controller "FooterCtrl" :ng-show "showCookieMessage"}
   [:div.panel.panel-info
    [:div.panel-heading "Cookies"]
    [:div.panel-body
     [:button.btn.btn-warning.pull-right {:ng-click "showCookieMessage = false"} "understood"]
     [:p
      "This site uses cookies to provide essential functionality, such as remembering your
      identity. You can find out details of what information we store here "
      (link-to "/cookies" "here")
      ". By dismissing this message you agree to let this application store the data it needs
      to operate."]
     ]]])

(defn common
  ([title body] (common title body []))
  ([title body scripts]
   (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
      [:title title]
      (pv/include-persona)
      (include-css "/css/style.css")
      ]
     (concat [
              [:body {:class "staircase"}
               (header)
               [:section#content.main body]]
               (footer)
              ]
             scripts))))

(defn four-oh-four []
  (common "Page Not Found"
          [:div#four-oh-four "The page you requested could not be found"]))

(def about-header
     [:div.about-header
      [:div.container
       [:h1 "InterMine Steps"]
       [:p "The data-flow interface to InterMine data-warehouses,
           providing an extensible, programmable work-bench for
           scientists."]]])

(def welcome
   [:div.row.welcome {:ng-controller "WelcomeCtrl" :ng-show "showWelcome"}
    [:div.panel.panel-default
     [:div.panel-heading
      "Welcome to " [:strong "Steps"]]
     [:div.panel-body

      about-header 

      [:p
       "This is the data-flow interface for intermine data-warehouses.
       If this is your first time here, maybe you might like to read more about
       the intermine system " (link-to "/about" "here") "."]

      [:p
       "Below you will find a number of "
       [:strong "different tools"]
       " to get started. Each one offers
       a different specialised entry point into all the data available to you, and can 
       be linked in turn to a sequence of composable actions. The easiest way to get
       started is to enter a search term:"
       [:div.row
        [:form.search-form.col-sm-6.col-sm-offset-3 search-input]]]

      [:p
       [:strong "You do not need to be logged in"]
       " to use this site - all the site's functionality is
       available to you to use as a temporary anonymous user. But if you want to store your
       histories permanently you "
       [:strong "can sign in with any email address"]
       " - we don't store your
       personal information and you won't have to remember any new passwords."]
      [:div.btn-toolbar
        [:button.btn.btn-default.pull-right {:ng-click "showWelcome = false"}
        "Do not show again"]
        [:div.login.pull-left {:ng-hide "auth.loggedIn"} (login)]]
      ]]])

(defn initiator
  [attrs panel-attrs heading-buttons]
  [:starting-point.xs-col-12 (assoc attrs :ng-hide "tool.error")
   [:div.panel.panel-default.first-step
    panel-attrs
    (apply vector :div.panel-heading
     (concat heading-buttons
             [ [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "reset()"}]
               "{{tool.heading}}" ]))
    [:div.panel-body
     [:native-tool {:tool "tool" :actions "buttons" :state "state"}]]
    [:div.panel-footer {:ng-if "tool.action"}
     [:button.btn.btn-default.pull-right
      {:ng-disabled "state.disabled" :ng-click "act()"}
      "{{buttons.act || tool.action}}"]
     [:div.clearfix]]]])

(def starting-point
  [:div.container-fluid

   welcome

   [:div.row.starting-points
    [:div.alert.alert-warning {:ng-show "tool.error"}
     [:p
      [:strong "Error"]
      " {{tool.error.message}}"]]
    (initiator {}
               {:class "full-height" :ng-class "{'with-action': tool.action}"}
               [])]])

(def starting-points
  [:div.container-fluid

   welcome

   [:div.row.starting-points {:ng-controller "StartingPointsController"}

    (initiator {:ng-class "getWidthClass(tool)"
                :ng-repeat "tool in startingPoints | filter:{active:true}"
                :ng-show "tool.state != 'DOCKED'"}
               {:ng-class "(tool.action ? 'with-action ' : '') + getHeightClass(tool)"}
               [[:i.fa.fa-arrows-alt.pull-right {:ng-click "expandTool(tool)"}]])

    [:div.docked-tools
     [:ul.nav.nav-pills
      [:li.active {:ng-show "anyToolDocked()"}
       [:a {:ng-click "undockAll()"} [:i.fa.fa-th-large]]]
      [:li.active {:ng-repeat "tool in startingPoints"
                   :ng-show "tool.state == 'DOCKED'"}
       [:a {:ng-click "expandTool(tool)"} "{{tool.heading}}"]]]]]])

(def edit-button
       [:i.fa.fa-edit.pull-right {:ng-show "!editing" :ng-click "editing = true"}])

(def collapse-button
  [:i.fa.pull-left.collapser
   {:ng-click "collapsed = !collapsed"
    :ng-class "{'fa-caret-right': collapsed, 'fa-caret-down': !collapsed}"}])

(defn cancel-button [cb]
  [:i.fa.fa-undo.pull-right {:ng-show "editing" :ng-click cb}])

(defn save-button [cb]
       [:i.fa.fa-save.pull-right {:ng-show "editing" :ng-click cb}])

(defn label-input-pair [model]
  [[:span {:ng-show "!editing"}
    [:em {:ng-hide model} (clojure.string/replace model #"\w+\." "No ")]
    (str "{{" model "}}")]
   [:input.form-control {:ng-show "editing" :ng-model model}]])

(defelem tool-not-found []
  [:div.alert.alert-danger 
   [:h3 "Error"]
   [:p
    " The required tool for this step could not be found. Check with the site
    administrator to make sure that the "
    [:code "{{step.tool}}"]
    " tool is installed"]
   (update-in (mail-to contact
                       [:span
                        [:i.fa.fa-envelope-o]
                        " Send a grumpy email"])
              [1 :class] (constantly "btn btn-default"))])

(def facets ;; Facets panel displayed when there are facets to show.
  [:div.facets.panel.panel-default
   {:ng-controller "FacetCtrl"
    :ng-show "state.facets"}
   [:div.panel-heading "Facets"]
   [:div.panel-body
    [:div.row
     {:ng-repeat "(name, facetSet) in state.facets"}
     [:h4 
      {:ng-click "closed = !closed"}
      [:i.fa {:ng-class "{'fa-caret-right': closed, 'fa-caret-down': !closed}"}]
      [:ng-pluralize
       {:count "countFacets(facetSet)"
        :when "{'one': 'One {{name}}', 'other': '{} {{name}}s'}"}]]
     [:div.slide-up {:ng-class "{closed: closed}"}
      [:button.col-xs-12.facet.btn.btn-default.clearfix
       {:blur-on "click"
        :ng-class "{active: info.selected}"
        :ng-repeat "info in facetSet | mappingToArray | orderBy:'count':true"
        :ng-click "info.selected = !info.selected"}
       [:span.pull-right.badge "{{info.count}}"]
       [:span.pull-left        "{{info.$key}}"]]
      ]]]])

(def history
  [:div.container-fluid.history-view
   [:div.row

    [:div.sidebar.slide-left.col-xs-12.col-md-2
     {:ng-class "{minimised: state.expanded, collapsed: collapsed}"}
     [:div.panel.panel-default
      (apply vector
             :div.panel-heading
             collapse-button
             edit-button
             (cancel-button "updateHistory()")
             (save-button "saveHistory()")
             (label-input-pair "history.title"))
      (apply vector
             :div.panel-body
             [:p [:small [:em "{{history.created_at | roughDate }}"]]]
             (label-input-pair "history.description"))
      [:div.list-group
        [:a.list-group-item {:ng-repeat "s in steps"
                             :ng-controller "HistoryStepCtrl" 
                             :ng-class "{active: step.id == s.id}"
                             :href "/history/{{history.id}}/{{$index + 1}}"}

         [:i.pull-right.fa.fa-edit
            {:ng-click "openEditDialogue()"
             :ng-show "step.id == s.id"}]
         "{{s.title}}"]]]

     facets]

    [:div.next-steps.col-xs-12.col-md-2.slide-right.pull-right.offscreen
     {:ng-class "{onscreen: (!state.expanded && nextSteps.length)}"}
     [:div.panel.panel-default
      [:div.panel-heading "Next steps"]
      [:div.list-group {:ng-show "nextSteps.length"}
       [:next-step
        {:ng-repeat "ns in nextSteps"
         :previous-step "step"
         :append-step "nextStep(data)"
         :tool "ns.tool"
         :service "ns.service"
         :data "ns.data"}
        ]]
      [:div.panel-body {:ng-hide "nextSteps.length"}
       [:em "No steps available"]]]]

    [:div.col-xs-12.slide-left
     {:ng-class "{'col-md-8': (nextSteps.length && !state.expanded),
                  'col-md-10': (!state.expanded && !nextSteps.length),
                  'col-md-offset-2': !state.expanded}"}
     (tool-not-found {:ng-show "error.status === 404"})
     [:div.current-step
      {:ng-hide "error.status === 404"
       :tool "tool"
       :step "step"
       :state "state"
       :has-items "setItems(key, type, ids)"
       :has "hasSomething(what, data, key)"
       :wants "wantsSomething(what, data)"
       :next-step "nextStep(data)"
       :toggle "state.expanded = !state.expanded"} ]]
    ]])

(defn modal-dialogue ;; template for a dialogue whose controller provides close and ok methods.
  ([title body-content]
   (modal-dialogue title
                   (->> (string/split title #" ")
                        (map string/lower-case)
                        (string/join "-"))
                   body-content))
  ([title body-class body-content]
   (html
     [:div.modal-header
      [:h3.modal-title
       [:button.btn.btn-warning.pull-right {:ng-click "cancel()"} "close"]
       title]]
     [:div.modal-body {:class body-class} body-content]
     [:div.modal-footer
      [:button.btn.btn-warning {:ng-click "cancel()"} "cancel"]
      [:button.btn.btn-primary {:ng-click "ok()"} "OK"]
      ])))

(def edit-step
  (modal-dialogue "Edit step data"
                  [:form.form
                   [:editable-data {:data "data"}]]))

(def choose-tool-dialogue
  (modal-dialogue "Choose tool"
                  [:ul.list-group
                   [:li.list-group-item
                    {:ng-repeat "item in items"
                     :ng-class "{active: item == selected.item}"}
                    [:a {:ng-click "selected.item = item"} "{{item.heading}}"]]]))

(def service-control-buttons 
  [:div.btn-group.btn-group.sm
   [:button.btn.btn-warning.btn-sm
    {:ng-show "service.editing"
     :ng-click "cancelEditing(service)"} "cancel"]
   [:button.btn.btn-primary.btn-sm
    {:ng-show "service.editing"
     :ng-click "saveChanges()"}
    [:i.fa.fa-save]]
   [:button.btn.btn-default.btn-sm
    {:ng-click "resetService(service)"
     :ng-hide "service.editing"}
    [:i.fa.fa-undo]]
   [:button.btn.btn-default.btn-sm
    {:tooltip "link this service to {{service.user.username ? 'a different' : 'an existing'}} account"
     :tooltip-placement "right"
     :ng-hide "service.editing"
     :ng-click "linkService(service)"}
    [:i.fa
     {:ng-class "{'fa-link': !service.user.username, 'fa-unlink': service.user.username}"}
     ]]])

(defelem service-row []
  [:tr {:ng-controller "ServiceController"}
   [:td [:input {:type "checkbox" :ng-model "service.active"}]]
   [:td "{{service.ident}}"]
   [:td [:a {:href "{{service.root}}"} "{{service.name}}"]]
   [:td
    [:div.form-group {:ng-show "service.editing" :ng-class "{'has-error': service.user == null}"}
     [:input.form-control.token {:ng-model "service.token"}]]
    [:code {:ng-hide "service.editing" :tooltip "your api token"} "{{service.token}}"]]
   [:td "{{service.user.username || 'anonymous'}}"]
   [:td service-control-buttons]])

(def add-service-controls
  [:div.add-service-controls
   [:form.form
    {:name "addServiceForm" :ng-submit "addService()"}
    [:btn-group.pull-right
     [:button.btn.btn-default.pull-right
      {:ng-hide "adding.active" :ng-click "adding.active = true"}
      [:i.fa.fa-plus-square]
      " Add a data-source"]
     [:button.btn.pull-right
      {:type "submit"
       :ng-show "adding.active"
       :tooltip "{{adding.error.data.message}}"
       :ng-class "{'btn-primary': !adding.error, 'btn-danger': adding.error}"
       :ng-disabled "adding.urlError || adding.authError || adding.inProgress || addServiceForm.$invalid"}
      [:i.fa {:ng-class "{'fa-plus-square': !adding.inProgress, 'fa-spin fa-circle-o-notch': adding.inProgress}"}]
      " Add"]
     [:button.btn.btn-warning.pull-right
      {:ng-show "adding.active" :ng-click "adding.active = false"}
      [:i.fa.fa-minus-square]
      " Cancel"]]
    [:div {:ng-if "adding.active"}
     [:div.form-group
      {:ng-class "{'has-error': addServiceForm.$dirty && addServiceForm.name.$error.required}"}
      [:label "name"]
      [:input.form-control {:name "name" :required "true" :ng-model "adding.mine.name" :placeholder "a short, human-readable name"}]]
     [:div.form-group
      {:ng-class "{'has-error': adding.urlError || (addServiceForm.$dirty && addServiceForm.url.$error.required)}"}
      [:label "URL"]
      [:div.input-group
       [:div.input-group-btn {:dropdown true}
        [:button.btn.btn-default.dropdown-toggle
         "{{adding.mine.scheme}}://"
         [:span.caret]]
        [:ul.dropdown-menu
         [:li [:a {:ng-click "adding.mine.scheme = 'http'"} "http"]]
         [:li [:a {:ng-click "adding.mine.scheme = 'https'"} "https"]]]]
        [:input.form-control {:name "url" :required "true" :ng-model "adding.mine.root" :placeholder "location of the data source"}]]]
     [:div.form-group
      {:ng-class "{'has-error': adding.authError}"}
      [:label "API token"]
      [:input.form-control {:name "token" :ng-model "adding.mine.token" :placeholder "api token (optional)"}]]]]])

(def options-dialogue
  (modal-dialogue "Options"
                  [:tabset.options

                   [:tab
                    [:tab-heading "Active services"]
                    [:div.services
                     [:table.table
                      [:tbody (service-row {:ng-repeat "service in services"}) ]]
                     add-service-controls]]

                   [:tab
                    [:tab-heading "Starting Tools"]
                    [:div.starting-tools
                     [:table.table
                      [:tbody
                       [:tr {:ng-repeat "tool in startingPoints"}
                        [:td [:input {:type "checkbox" :ng-model "tool.active"}]]
                        [:td "{{tool.ident}}"]
                        [:td "{{tool.heading}}"]
                        [:td [:code "{{tool.args | json }}"]]
                        ]]]]]

                   ]))

(def about
  [:div.container-fluid
   [:div.row
    [:div.about.col-xs-10.col-xs-offset-1

     about-header

     [:section
      [:h2 "Using the home-page"]
      [:p.lead
       "The " (link-to "/" "home page") " allows quick access to the tools available "
       "for starting a new history"]
      [:p
       "Tools are components that let you interact with your data. They can do a wide
       range of things, from uploading a data-set, to running a complex query, to
       managing your stored resources. On the home page you can see the tools that can
       be used to start a sequence of steps in a data-flow. One of these is shown below:"]
      [:div.row
       [:starting-point.col-sm-6 ;; TODO: Move the template below into a directive. Currently not very DRY at all.
        [:div.panel.panel-default.first-step {:ng-class "tool.action ? 'with-action' : ''"}
         [:div.panel-heading
          [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "reset()"}]
          "{{tool.heading}}"]
         [:div.panel-body
          [:native-tool {:tool "tool"}]]
         [:div.panel-footer {:ng-if "tool.action"}
          [:button.btn.btn-default.pull-right {:ng-disabled "tool.disabled" :ng-click "act()"} "{{tool.action}}"]
          [:div.clearfix]]]]
       [:div.col-sm-6
        [:p {:ng-repeat "para in tool.description"} "{{para}}"]
        [:p
         "The following standard controls are available for interacting with one of these
         tools:"]
        [:table.table
          [:tr
           [:td
            [:button.btn.btn-default {:ng-disabled "tool.disabled" :ng-click "act()"} "{{tool.action}}"]]
           [:td
            "This button performs the main action for the component (if it has one)"]]
         [:tr
          [:td
            [:i.fa.fa-undo {:ng-click "reset()"}]]
          [:td
           "This button restores the component to its orginal state, removing any filters you may have applied."]]]]
        ]]]]])

(defn render-partial
  [fragment]
  (case fragment
    "frontpage" (html starting-points)
    "starting-point" (html starting-point)
    "edit-step-data" edit-step
    "choose-tool-dialogue" choose-tool-dialogue
    "options-dialogue" options-dialogue
    "history" (html history)
    "about" (html about)
    {:status 404})) 

(defn index []
  (common "Steps"
          [:div {:ng-view ""}]
          (map with-utf8-charset
               (conj (apply include-js (map (partial str "/vendor/") vendor-scripts))
                     (entry-point "/js/frontpage")))))

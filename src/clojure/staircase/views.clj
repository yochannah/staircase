(ns staircase.views
  (:require [hiccup.form :refer :all]
            [hiccup.def :as d]
            [persona-kit.view :as pv])
  (:use [hiccup.core :only (html)]
        [hiccup.page :only (html5 include-css include-js)]
        [hiccup.element :only (link-to unordered-list javascript-tag mail-to)]))

(defn- with-utf8-charset [[tag attrs]]
  [tag (assoc attrs :charset "utf-8")])

(def ^:dynamic require-js "/vendor/requirejs/require.js")

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

(def home-links [["home" "home" "/"] ["about intermine" "info" "/about"] ["options" "cog"]])

(defn header []
  [:div.navbar.navbar-inverse.navbar-custom.navbar-default.navbar-fixed-top {:role "navigation"}
   [:div.container-fluid

    [:div.navbar-header ;; The elements to always display.
     [:button.navbar-toggle {:data-toggle "collapse" :ng-click "showHeaderMenu = !showHeaderMenu"}
      [:span.sr-only "Toggle navigation"]
      (for [_ (range 3)]
        [:span.icon-bar])]
     [:a.navbar-brand.dropdown-toggle
      [:span.app-name "Steps"]] ;; Make configurable?
     (unordered-list {:class "dropdown-menu"}
                     (for [[title icon path] home-links]
                       (link-to (or path (str "/" title)) [:i.fa.fa-fw {:class (str "fa-" icon)}] " " title)))]

    [:div.collapse.navbar-collapse {:ng-class "{in: showHeaderMenu}"};; Only show if enough space.

     [:p.navbar-text.navbar-right {:ng-show "auth.loggedIn"}
      "Signed in as {{auth.identity}}"]

     (nav-list)

     (search-form)

     ]]])

(defn search-form []
  [:form.navbar-form.navbar-left
   {:ng-controller "QuickSearchController"
    :role "search"
    :ng-submit "startQuickSearchHistory(searchTerm)"}
   [:div.input-group
    [:input.form-control {:ng-model "searchTerm" :placeholder "eve"}] ;; TODO: Definitely make placeholders injectable.
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit"}
      "Search "
      [:i.fa.fa-search]]]]])

(defn nav-list []
  [:ul.nav.navbar-nav.navbar-right {:ng-controller "AuthController"}
   [:li.dropdown
    [:a.dropdown-toggle
     "Start " [:b.caret]]
     [:ul.dropdown-menu
      [:li {:ng-repeat "tool in startingPoints"}
           [:a {:href "/starting-point/{{tool.ident}}"}
               "{{tool.heading}}"]]]]
   [:li.dropdown
    [:a.dropdown-toggle
     "get in touch! " [:b.caret]]
    (unordered-list {:class "dropdown-menu"}
                    [(mail-to "alex.kalderimis@gmail.com" ;; TODO: Make configurable.
                              [:span
                               [:i.fa.fa-envelope-o]
                               " Contact"])
                     (link-to "https://github.com/alexkalderimis/staircase"
                              [:i.fa.fa-github]
                              " View on github")])]
   [:li {:ng-show "auth.loggedIn"} (logout)]
   [:li {:ng-hide "auth.loggedIn"} (login)]
   ])


(defn footer []
  [:section.footer {:ng-controller "FooterCtrl" :ng-show "showCookieMessage"}
   [:div.panel.panel-info
    [:div.panel-heading "Cookies"]
    [:div.panel-body
     [:p
      "This site uses cookies to provide essential functionality, such as remembering your
      identity. You can find out details of what information we store here "
      (link-to "/cookies" "here")
      "."]
     [:button.btn {:ng-click "showCookieMessage = false"} "Do not show again"]]]])

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

(def starting-points
  [:div.container-fluid
   [:div.row {:ng-controller "WelcomeCtrl" :ng-show "showWelcome"}
    [:div.panel.panel-default
     [:div.panel-heading
      "Welcome to " [:strong "Steps"]]
     [:div.panel-body
      [:p
       "This is the data-flow interface for intermine data-warehouses.
       If this is your first time here, maybe you might like to read more about
       the intermine system " (link-to "/about" "here") "."]
      [:button.btn.btn-default {:ng-click "showWelcome = false"}
       "Do not show again"]]]]

   [:div.row.starting-points {:ng-controller "StartingPointsController"}
    [:starting-point {:ng-class "getWidthClass(tool)"
                      :ng-repeat "tool in startingPoints"
                      :ng-hide "tool.state == 'DOCKED'"}
     [:div.panel.panel-default.first-step {:ng-class "(tool.action ? 'with-action ' : '') + getHeightClass(tool)"}
      [:div.panel-heading
       [:i.fa.fa-arrows-alt.pull-right {:ng-click "expandTool(tool)"}]
       [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "reset()"}]
       "{{tool.heading}}"]
      [:div.panel-body
       [:native-tool {:tool "tool"}]]
      [:div.panel-footer {:ng-if "tool.action"}
       [:button.btn.btn-default.pull-right {:ng-disabled "tool.disabled" :ng-click "act()"} "{{tool.action}}"]
       [:div.clearfix]]
      ]]

    [:div.docked-tools
     [:ul.nav.nav-pills
      [:li.active {:ng-show "anyToolDocked()"}
       [:a {:ng-click "undockAll()"} [:i.fa.fa-th-large]]]
      [:li.active {:ng-repeat "tool in startingPoints"
                   :ng-show "tool.state == 'DOCKED'"}
       [:a {:ng-click "expandTool(tool)"} "{{tool.heading}}"]]]]]

   ])

(def edit-button
       [:i.fa.fa-edit.pull-right {:ng-show "!editing" :ng-click "editing = true"}])

(defn cancel-button [cb]
  [:i.fa.fa-undo.pull-right {:ng-show "editing" :ng-click cb}])

(defn save-button [cb]
       [:i.fa.fa-save.pull-right {:ng-show "editing" :ng-click cb}])

(defn label-input-pair [model]
  [[:span {:ng-show "!editing"}
    [:em {:ng-hide model} (clojure.string/replace model #"\w+\." "No ")]
    (str "{{" model "}}")]
   [:input.form-control {:ng-show "editing" :ng-model model}]])

(def history
  [:div.container-fluid.history-view
   [:div.row

    [:div.sidebar.slide-left.col-xs-12.col-md-2
     {:ng-class "{minimised: expanded}"}
     [:div.panel.panel-default
      (apply vector
             :div.panel-heading
             edit-button
             (cancel-button "updateHistory()")
             (save-button "saveHistory()")
             (label-input-pair "history.title"))
      (apply vector
             :div.panel-body
             (label-input-pair "history.description"))
      [:div.list-group
        [:a.list-group-item {:ng-repeat "s in steps"
                             :ng-class "{active: step.id == s.id}"
                             :href "/history/{{history.id}}/{{$index + 1}}"}

         "{{s.title}}"]]]]

    [:div.next-steps.col-xs-12.col-md-2.slide-right.pull-right.offscreen
     {:ng-class "{onscreen: (!expanded && nextSteps.length)}"}
     [:div.panel.panel-default
      [:div.panel-heading "Next steps"]
      [:div.list-group {:ng-show "nextSteps.length"}
       [:next-step
        {:ng-repeat "ns in nextSteps"
         :previous-step "step"
         :append-step "nextStep(data)"
         :tool "ns.tool"
         :data "ns.data"}
        ]]
      [:div.panel-body {:ng-hide "nextSteps.length"}
       [:em "No steps available"]]]]

    [:div.col-xs-12.slide-left
     {:ng-class "{'col-md-8': (!expanded && nextSteps.length), 'col-md-10': (!expanded && !nextSteps.length), 'col-md-offset-2': !expanded}"}
     [:div.current-step
      {:tool "tool"
       :step "step"
       :full-size "expanded"
       :has-items "setItems(key, type, ids)"
       :has-list "hasList(data)"
       :next-step "nextStep(data)"
       :on-toggle "expanded = !expanded"} ]]

    ]])

(def about
  [:div.container-fluid
   [:div.row
    [:div.about.col-xs-10.col-xs-offset-1
     [:div.about-header
      [:div.container
       [:h1 "InterMine Steps"]
       [:p "The data-flow interface to InterMine data-warehouses,
           providing an extensible, programmable tool-box for
           scientists."]]]
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
    "history" (html history)
    "about" (html about)
    {:status 404})) 

(defn index []
  (common "Steps"
          [:div {:ng-view ""}]
          (map with-utf8-charset
               (conj (apply include-js (map (partial str "/vendor/") vendor-scripts))
                     (entry-point "/js/frontpage")))))

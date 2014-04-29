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
              [:body
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
       [:i.fa.fa-undo.pull-right {:ng-show "tool.resettable" :ng-click "resetTool(tool)"}]
       "{{tool.heading}}"]
      [:div.panel-body
       [:native-tool {:tool "tool"}]]
      [:div.panel-footer {:ng-if "tool.action"}
       [:button.btn.btn-default.pull-right "{{tool.action}}"]
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

(defn render-partial
  [fragment]
  (case fragment
    "frontpage" (html starting-points)
    {:status 404})) 

(defn index []
  (common "Steps"
          [:div {:ng-view ""}]
          (map with-utf8-charset
               (conj (apply include-js (map (partial str "/vendor/") vendor-scripts))
                     (entry-point "/js/frontpage")))))

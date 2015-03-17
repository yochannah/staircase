(ns staircase.views.header
  (:require [staircase.views.buttons :as btn]
            [staircase.views.forms :refer (search-form)]
            [hiccup.def :refer (defelem)]
            [hiccup.element :refer (link-to unordered-list mail-to)]))

(declare nav-list)

(def home-links [["home" "home" "/"]
                 ["about intermine" "info" "/about"]
                 ["options" "cog" {:ng-click "showOptions()"}]])

(defn snippet [config]
  [:header.fixed
    {:headroom true
     :offset 70
     :tolerance 5
     :classes "{initial: 'animated',
                pinned: 'slideDown',
                unpinned: 'slideUp',
                top: 'headroom-top',
                notTop: 'headroom-not-top'}"}
  [:div.navbar.navbar-custom.navbar-default
   {:role "navigation" :ng-controller "NavCtrl"}
   [:div.container-fluid

    [:div.navbar-header ;; The elements to always display.
     [:button.navbar-toggle {:data-toggle "collapse" :ng-click "showHeaderMenu = !showHeaderMenu"}
      [:span.sr-only "Toggle navigation"]
      (for [_ (range 3)]
        [:span.icon-bar])]
     [:div
      (link-to {:class "navbar-brand"} "/"
        [:span.app-name (:project-title config)])
      ]]

    [:div.collapse.navbar-collapse {:ng-class "{in: showHeaderMenu}"};; Only show if enough space.

     (nav-list config)

     (search-form config)

     ]]]])

(defelem tool-list []
  [:li {:ng-repeat "tool in startingPoints"}
   [:a {:href "/starting-point/{{tool.ident}}/{{tool.args.service}}"}
    "{{tool.heading}}"]])

(defn contacts [{contact :contact-email repo :gh-repository}]
  (let [links [(mail-to contact [:span
                                 [:i.fa.fa-envelope-o]
                                 " Contact"])]]
    (if repo
      (conj links
            (link-to {:target "blank"} repo
                     [:i.fa.fa-github] " View on github")
            (link-to {:target "blank"} (str repo "/issues")
                     [:i.fa.fa-warning]" Report a problem"))
      links)))

(defn nav-list [config]
  [:ul.nav.navbar-nav.navbar-right {:ng-controller "AuthController"}
   [:li {:ng-show "auth.loggedIn"} (link-to "/projects" "MyMine")]
   [:li.dropdown {:dropdown true}
    [:a.dropdown-toggle {:dropdown-toggle true}
     "Tools " [:b.caret]] [:ul.dropdown-menu (tool-list)]]
   [:li (link-to "/about" "Help")]
   [:li {:ng-click "showOptions()"} (link-to "" "Options")]
   [:li.dropdown {:dropdown true}
    [:a.dropdown-toggle {:dropdown-toggle true} "Contact " [:b.caret]]
     (unordered-list {:class "dropdown-menu"} (contacts config))]
   [:li {:ng-show "auth.loggedIn"} [:div (btn/logout)]]
   [:li {:ng-hide "auth.loggedIn"} [:div (btn/login)]]
   ])


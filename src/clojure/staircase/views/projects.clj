(ns staircase.views.projects
  (:use [hiccup.core :only (html)]
        [hiccup.element :only (link-to)]
        [hiccup.element :refer (link-to unordered-list)])
  )

(defn projects-header [config]
  ; [:div.about-header
   [:div.container
    [:div.row
     [:div.col-md-8.col-md-offset-2
      [:h1 (:project-title config)]]]
    [:div.row
     [:p "Welcome to the project page."]]
    ])


(defn create []
  [:div.panel.panel-primary
    [:div.panel-body
    [:form {:role "form"}
      [:div.form-group
        [:label "Project Name"]
        [:input {:ng-model "newtitle" :type "text", :placeholder "Name..." }]
      ]
      [:div.form-group
        [:label "Project Description"]
        [:input {:ng-model "newdescription" :type "text", :placeholder "Description..." }]
      ]
      [:div.form-group
      [:div.btn {:ng-click "createProject"} ]]]]])

(defn items []
  (html
      ; [:notifier]
      [:ul.list-group.cap
        [:li.list-group-item {:ng-repeat "list in lists" :droppable "list" :ng-click "setInspection(list)"}
          "{{list.title}} ({{list.short}})"
        ]]))

(defn project []
  (html [:div.panel.panel-primary.panel-project
      {:dropzone "project" :on-drop "dropped(dragE1, dropE1)"}
    [:div.panel-heading {:ng-click "setInspection(project)"}
      "{{project.id}}: {{project.title}}"]
    [:div.panel-body "{{project.description}}"]
    [:div "the length" "{{project.contents.length}}"]
    [:ul [:li {:ng-repeat "item in project.contents"} "{{item.item_id}}"]]
    [:div.panel-footer "{{project.last_modified | date:'d MMM, yyyy'}}"]]))

(defn project-card []
  (html
    ; [:div.card {:dropzone "project" :on-drop "dropped(dragE1, dropE1)"}
      [:div.header [:div.padme "{{project.title}}"]]
      [:div.contents [:div.padme "{{project.description}}"]]
      [:div.footer
        [:div.footer-item.padme "{{project.contents.length}} items"]
    ]))

(defn project []
  (html [:div.panel.panel-primary.panel-project
      {:dropzone "project" :on-drop "dropped(dragE1, dropE1)"}
    [:div.panel-heading {:ng-click "setInspection(project)"}
      "{{project.id}}: {{project.title}}"]
    [:div.panel-body "{{project.description}}"]
    [:div "the length" "{{project.contents.length}}"]
    [:ul [:li {:ng-repeat "item in project.contents"} "{{item.item_id}}"]]
    [:div.panel-footer "{{project.last_modified | date:'d MMM, yyyy'}}"]]))


(defn projects []
  (html [:div.container-fluid
   [:div.row.projects-grid-container
    [:div.col-sm-4.col-md-3.project-container
      {:ng-repeat "project in allProjects"}
     (project)]]]))


(defn projects-flex []
  (html [:div.card
      {:ng-repeat "project in allProjects"
      :dropzone "project"
      :on-drop "dropped(dragE1, dropE1)"
      :ng-click "setInspection(project)"}
     (project-card)]))

(defn project-details []
  (html [:div.panel.panel-primary 
        [:div.panel-heading "Project Details"]
        [:div.panel-body
          [:h2 "{{inspection.title}}"]
          [:p "{{inspection.description}}"]
          ; "{{inspection.contents}}"]]))
          [:table.table
            [:thead
              [:tr
                [:th "Type"]
                [:th "ID"]
                [:th "Source"]]]
            [:tbody
              [:tr {:ng-repeat "item in inspection.contents"}
                [:td "{{item.type}}"]
                [:td "{{item.item_id}}"]
                [:td "{{item.source}}"]]]]]]))


(defn breadcrumb []
  (html
  [:ol.breadcrumb.bc
    ; [:li [:a {:ng-click "setlevel(allProjects)"} "Home"]]
    [:li [:a {:ng-click "setlevelbc(allProjects)"} "Home"]]
    [:li {:ng-repeat "item in breadcrumbs" :ng-click "setlevelbc(item, $index)"} [:a "{{getname(item)}}"]]]))


(defn ctrlbutton []
  (html [:form {:editable-form "" :ng-show "rowform.$visible" :name "rowform" :onaftersave "updatefolder(project)"}
          ; [:div.drpdwn {:ng-show "hoverEdit && !rowform.$visible" :dropdown true}
          [:button.btn.btn-primary.btn-sm {:type "submit"} "Save"]
          [:button.btn.btn-sm {:ng-click "rowform.$cancel()" :type "button"} "Cancel"]]
          [:div.drpdwn {:ng-show "hoverEdit && !rowform.$visible" :dropdown true}
            [:button.btn.btn-default.btn-sm.dropdown-toggle "Options"
              [:span.caret]]
            [:ul.dropdown-menu
              [:li {:ng-click "rowform.$show()"} [:a "Edit Folder"]]
              [:li {:ng-click "deleteitem(item)"} [:a "Delete Item"]]
              [:li {:ng-click "deletefolder(project)"} [:a "Delete Folder"]]
              [:li {:ng-click "talk()"} [:a "Copy Project"] ]]]))

(defn navbar []
  (html [:div.navbar.navbar-custom.navbar-default
    (breadcrumb)
    [:div.collapse.navbar-collapse
      ; [:ul.nav.navbar-nav.navbar-right
      ;   [:li [:a {:href "#"} [:span.fa.fa-plus " New Folder"]]]]
      [:form.navbar-form.navbar-right
        [:div.form-group
          [:input.form-control {:ng-model "foldername" :type "text" :name "projectname" :placeholder "Folder name..."}]]
        [:button.btn.btn-default {:ng-click "createProject(foldername)"} "Create"]]]]))

(defn project-table []
  
  (html
    (navbar)
    [:table.table.table-hover.project-table.ng-hide
      {:ng-show "!details"
      :dropzone "level"
      :on-drop "dropped(dragE1, dropE1)"}
  [:thead
    [:tr
      [:th "Name"]
      [:th "Description"]
      [:th "Kind"]
      [:th "Modified"]
      [:th ""]]]
  [:tbody

    [:tr {:ng-repeat "project in level.child_nodes"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"
    :dropzone "project"
    :on-drop "dropped(dragE1, dropE1)"}


      [:td [:i {:ng-class "geticon(project)"}]
        [:a {:ng-click "setlevel(project)"
                :editable-text "project.title"
                :e-name "name"
                :e-form "rowform"
                :buttons "no"
                :onbeforesave "checkempty($data)"}
        "{{project.title}}"]]
      [:td [:span {:editable-text "project.description"
            :e-name "description"
            :e-form "rowform"
            :buttons "no"}
            "{{project.description}}"]]
      [:td.smallest [:span {:ng-show "item.type != 'Project'"} "{{item.type}}"]]
      [:td.smallest "{{project.last_modified | date:'dd/MM/yyyy hh:mm a'}}"]
      [:td.controls (ctrlbutton)]
      
      ]

    

    [:tr {:ng-repeat "item in level.contents"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"}
      [:td [:a {:ng-click "setInspection(item)"} [:i {:ng-class "geticon(item)"}] "{{item.item_id}}"]]
      [:td "{{project.item_id}}"]
      [:td.smallest [:span {:ng-show "item.type != 'Project'"} "{{item.type}}"]]
      [:td.smallest "{{project.last_modified | date:'dd/MM/yyyy hh:mm a'}}"]
      [:td.controls (ctrlbutton)]]

    [:tr.ng-hide {:ng-hide "level.contents.length > 0 || level.child_nodes.length > 0"}
      [:td {:colspan 5} "Empty folder."]]]]))

(defn explorer []
  (html
      ; [:notifier]
      [:ul.list-group.cap
        [:li.list-group-item {:ng-repeat "list in lists" :droppable "list" :ng-click "setInspection(list)"}
          "{{list.title}} ({{list.short}})"
        ]]))

(defn snippet [config]
  (html [:div.container-fluid
    ; [:div.row-fluid
    ;     [:div.col-md-12
    ;       [:div (create)]]]
    [:div.row-fluid
      [:div.col-md-8
        [:div (project-table)]]
      [:div.col-md-4
        [:div (explorer)]]]
    ; [:div.row-fluid
    ;   [:div.col-md-12 (items)]]
    [:div.row-fluid
      [:div.col-md-12 
      [:div.panel.panel-warning 
        [:div.panel-heading "Inspection"]
        [:div.panel-body
          [:pre.projects "{{inspection}}"]]]]]]))



; <span class="glyphicon glyphicon-folder-open"></span>
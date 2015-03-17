(ns staircase.views.projects
  (:use [hiccup.core :only (html)]
        [hiccup.element :only (link-to)]
        [hiccup.element :refer (link-to unordered-list)])
  )

(defn projects-header [config]
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

(def list-group
  [:ul.list-group.cap
   [:li.list-group-item {:ng-repeat "list in lists | orderBy:'title'" :droppable "list" :ng-click "setInspection(list)"}
    [:span.badge.origin
     {:title "{{list.short}}" :ng-class "list._mine.meta.color"}
     "{{ list.short | limitTo:1 }}"]
    [:span.badge.origin {:ng-show "list.authorized"} [:i.fa.fa-user]]
    [:h4 "{{list.title}}"
         [:i.fa.fa-ellipsis-h
          {:ng-show "list.description" :ng-click "showDescription = !showDescription"}]]

    [:p.text-muted.list-group-item-text {:ng-show "showDescription"} "{{list.description }}"]
    ]])

(def templates-group
  [:ul.list-group.cap
   [:li.list-group-item {:ng-repeat "template in templates" :droppable "template" :ng-click "setInspection(list)"}
    [:span.badge.origin {:title "{{template.short}}"} "{{ template.short | limitTo:1 }}"]
    [:h4 "{{template.title}}"]
    ]])

(defn items [] (html list-group))

(defn project []
  (html [:div.panel.panel-primary.panel-project
      {:dropzone "project" :on-drop "dropped(dragE1, dropE1)"}
    [:div.panel-heading {:ng-click "setInspection(project)"}
      "{{project.id}}: {{project.title}}"]
    [:div.panel-body "{{project.description}}"]
    [:div "the length" "{{project.contents.length}}"]
    [:ul [:li {:ng-repeat "item in project.contents"} "{{item.item_id}}"]]
    [:div.panel-footer "{{project.last_modified | date:'d MMM, yyyy'}}"]]))


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

(defn project-details []
  (html [:div.panel.panel-primary 
        [:div.panel-heading "Project Details"]
        [:div.panel-body
          [:h2 "{{inspection.title}}"]
          [:p "{{inspection.description}}"]
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
  [:ul.breadcrumb.list-inline.mymine
    [:li [:a {:ng-click "setlevelbc(allProjects)"} "Home"]]
    [:li {:ng-repeat "item in breadcrumbs" :ng-click "setlevelbc(item, $index)"} [:a "{{getname(item)}}"]]]))

(defn ctrlbuttonproject []
  (html [:form {:editable-form "" :ng-show "rowform.$visible" :name "rowform" :onaftersave "updatefolder(project)"}
          [:button.btn.btn-primary.btn-sm {:type "submit"} "Save"]
          [:button.btn.btn-sm {:ng-click "rowform.$cancel()" :type "button"} "Cancel"]]

          [:div.drpdwn {:ng-show "hoverEdit && !rowform.$visible" :dropdown true}
            [:button.btn.btn-default.btn-sm {:dropdown-toggle true} "Options"
              [:span.caret]]
            [:ul.dropdown-menu
              [:li {:ng-click "rowform.$show()"} [:a "Edit Folder"]]
              [:li {:ng-click "deletefolder(project)"} [:a "Remove Folder"]]]]))

(defn ctrlbuttonitem []
  (html [:form {:editable-form "" :ng-show "rowform.$visible" :name "rowform" :onaftersave "updatefolder(project)"}
          [:button.btn.btn-primary.btn-sm {:type "submit"} "Save"]
          [:button.btn.btn-sm {:ng-click "rowform.$cancel()" :type "button"} "Cancel"]]

          [:div.drpdwn {:ng-show "hoverEdit && !rowform.$visible" :dropdown true}
            [:button.btn.btn-default.btn-sm {:dropdown-toggle true} "Options"
              [:span.caret]]
            [:ul.dropdown-menu
              [:li {:ng-click "deleteitem(item)"} [:a "Remove Item"]]]]))

(defn navbar []
  (html [:div.navbar.navbar-custom.navbar-default
    [:div.collapse.navbar-collapse
      [:form.navbar-form.navbar-right
        [:div.form-group
          [:input.form-control {:ng-model "foldername" :type "text" :name "projectname" :placeholder "Folder name..."}]]
        [:button.btn.btn-default {:ng-click "createProject(foldername)"} [:i.fa.fa-folder " Create Folder"]]
        [:button.btn.btn-default.pushleft {:ng-click "showexplorer = !showexplorer" :ng-class "{'active': showexplorer}"} [:i.fa.fa-plus " Add Item"]]]]]))

(defn project-table []
  (html
    (navbar)
    (breadcrumb)
    [:table.table.table-hover.project-table.ng-hide
      {:ng-show "!details"
      :dropzone "level"
      :on-drop "dropped(dragE1, dropE1)"}
  [:thead
    [:tr
      [:th "Name"]
      [:th "Description"]
      [:th "Kind"]
      [:th "Source"]
      [:th "Modified"]
      [:th.foldercontrols ""]]]
  [:tbody

    [:tr {:ng-repeat "project in level.child_nodes"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"
    :dropzone "project"
    :on-drop "dropped(dragE1, dropE1)"}
      [:td [:i.testtest {:ng-class "geticon(project)"}
      [:span.badge.badge-notify "{{getContentCount(project)}}"]]
        [:a {:ng-click "setlevel(project)"
                :editable-text "project.title"
                :e-name "name"
                :e-form "rowform"
                :buttons "no"
                :onbeforesave "checkempty($data)"}
        "{{project.title}} "]]

      [:td [:span {:editable-text "project.description"
            :e-name "description"
            :e-form "rowform"
            :buttons "no"}
            "{{project.description}}"]]
      [:td.smallest [:span {:ng-show "item.type != 'Project'"} "{{item.type}}"]]
      [:td ""]
      [:td.smallest "{{project.last_modified | date:'dd/MM/yyyy hh:mm a'}}"]
      [:td.controls (ctrlbuttonproject)]]

    [:tr {:ng-repeat "item in level.contents"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"}
      [:td [:a {:href "{{gethref(item)}}" :ng-click ""} [:i {:ng-class "geticon(item)"}] "{{item.item_id}}"]]
      [:td "{{project.item_id}}"]
      [:td.smallest [:span {:ng-show "item.type != 'Project'"} "{{item.type}}"]]
      [:td.smallest "{{item.source}}"]
      [:td.smallest "{{project.last_modified | date:'dd/MM/yyyy hh:mm a'}}"]
      [:td.controls (ctrlbuttonitem)]]

    [:tr {:ng-model "level" :ng-show "emptymessage(level)"}
      [:td {:colspan 6}
        [:span {:ng-show "breadcrumbs.length > 0"} "Empty folder."]
        [:span {:ng-show "breadcrumbs.length < 1"} "Please begin by creating a folder."]]]]]))

(defn explorer []
  (html
      [:tabset
        [:tab {:heading "Lists"} list-group]
        [:tab {:heading "Templates"} templates-group]]))

(defn snippet [config]
  (html [:div.container-fluid
    [:div.row-fluid {:ng-hide "auth.loggedIn"}
      [:p "Please log in to access MyMine"]]
    [:div.flex-row.guttered {:ng-show "auth.loggedIn"}
      [:div.flex-box
        [:div (project-table)]]
      [:div.flex-box.flex-col-4 {:ng-show "showexplorer"}
        [:div (explorer)]]]
      
    ; [:div.row-fluid
    ;   [:div.col-md-12 (items)]]
    ; [:div.row-fluid
    ;   [:div.col-md-12 
    ;   [:div.panel.panel-warning 
    ;     [:div.panel-heading "Inspection"]
    ;     [:div.panel-body
    ;       [:pre.projects "{{inspection}}"]]]]]
          ]))

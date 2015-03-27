(ns staircase.views.projects
  (:use [hiccup.core :only (html)]))

(def list-group ^:private
  [:ul.list-group.cap
   [:li.list-group-item
    {:ng-repeat "item in appView.lists | orderBy:'title'"
     :ng-init "list = item.entity"
     :droppable "item"}
    [:span.badge.origin
     {:title "{{ item.source }}"
      :ng-class "item.meta.color"}
     "{{ item.source | limitTo:1 }}"]
    [:span.badge.origin {:ng-show "list.authorized"} [:i.fa.fa-user]]
    [:h4 "{{ list.title }}"
         [:i.fa.fa-ellipsis-h
          {:ng-show "list.description"
           :ng-click "showDescription = !showDescription"}]]

    [:p.text-muted.list-group-item-text
     {:ng-show "showDescription"}
     "{{list.entity.description }}"]]])

(def templates-group ^:private
  [:ul.list-group.cap
   [:li.list-group-item
    {:ng-repeat "item in appView.templates | orderBy:'name'"
     :ng-init "template = item.entity"
     :droppable "item"}
    [:span.badge.origin
     {:title "{{item.source}}"
      :ng-class "item.meta.color"}
     "{{ item.source | limitTo:1 }}"]
    [:h4 "{{ template.title }}"]]])

(def breadcrumbs ^:private
  [:ul.breadcrumb.list-inline.mymine
   [:li [:a {:ng-click "appView.goToRoot()"} "Home"]]
   [:li
    {:ng-repeat "segment in appView.pathToHere"
     :ng-click "appView.goToPathSegment($index)"}
    [:a "{{segment.name}}"]]])

(defn- row-controls
  [onSave & actions]
  [:div
   [:form ;; The form with the save/cancel controls.
    {:editable-form ""
     :ng-show "rowform.$visible"
     :name "rowform"
     :onaftersave onSave}
    [:button.btn.btn-primary.btn-sm {:type "submit"} "Save"]
    [:button.btn.btn-sm {:ng-click "rowform.$cancel()" :type "button"} "Cancel"]]

   [:div.drpdwn ;; Activate the actions.
    {:ng-show "hoverEdit && !rowform.$visible"
     :dropdown true}
    [:button.btn.btn-default.btn-sm {:dropdown-toggle true} "Options"
     [:span.caret]]
    [:ul.dropdown-menu
     (for [{click :click text :text} actions]
       [:li {:ng-click click} [:a text]])]]])

(def ctrl-button-project ^:private
  (row-controls "appView.updateProject(project)"
                {:click "rowform.$show()"        :text "Edit Folder"}
                {:click "deleteProject(project)" :text "Delete Folder"}))

(def ctrl-button-item ^:private
  (row-controls "appView.updateItem(item)"
                {:click "deleteItem(item)" :text "Remove {{item.item_type}} from Folder"}))

(defn- new-project-input
  [[prop placeholder]]
  [:div.form-group
   [:input.form-control
    {:ng-model (str "newProject" prop)
     :type "text"
     :placeholder placeholder}]])

(def navbar ^:private
  [:div.navbar.navbar-custom.navbar-default
   [:div.collapse.navbar-collapse
    [:form.navbar-form.navbar-right
     (for [opts [["Name" "Folder name..."] ["Desc" "optional description"]]]
       (new-project-input opts))
     [:button.btn.btn-default
      {:ng-class "{disabled: !newProjectName}"
       :ng-click "appView.createProject(newProjectName, newProjectDesc)"}
      [:i.fa.fa-folder]
      " Create Folder"]
     [:button.btn.btn-default.pushleft
      {:ng-class "{active: appView.showExplorer}"
       :ng-click "appView.showExplorer = !appView.showExplorer"}
      [:i.fa.fa-plus]
      " Add Item"]]]])

(def project-table-head ^:private
  [:thead
   [:tr
    (map (partial vector :th)
         ["Name" "Description" "Kind" "Source" "Modified" "" ])]])

(def subproject-rows ^:private
  [:tr ;; The branch nodes, ie. the sub-projects
   {:ng-repeat "project in appView.currentProject.child_nodes"
    :ng-mouseover "appView.hoverIn()"
    :ng-mouseleave "appView.hoverOut()"
    :dropzone "project"
    :on-drop "appView.dropped(dragE1, dropE1)"}

   ;; Editable and clickable folder title
   [:td [:i.fa.fa-folder
         [:span.badge.badge-notify "{{project.item_count}}"]]
    [:a {:ng-click "appView.setCurrentProject(project)"
         :editable-text "project.title"
         :e-name "name"
         :e-form "rowform"
         :buttons "no"
         :onbeforesave "appView.checkEmpty($data)"}
     "{{project.title}} "]]

   ;; Editable folder description.
   [:td [:span {:editable-text "project.description"
                :e-name "description"
                :e-form "rowform"
                :buttons "no"}
         "{{project.description}}"]]

   [:td {:colspan 2}] ;; no kind or source info for projects.
   [:td.smallest "{{project.last_modified | date:appView.dateFormat}}"]
   [:td.controls ctrl-button-project]])

(def content-rows ^:private
  [:tr ;; The leaf nodes, ie. the items.
   {:ng-repeat "item in appView.currentProject.contents"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"}
   [:td [:a
         {:href "{{ appView.getHref(item) }}"}
         [:i.fa {:ng-class "{'fa-list': item.item_type === 'List'
                             'fa-search': item.item_type === 'Template'}"}]
         "{{item.item_id}}"]]
   [:td          "{{ item.description }}"] ;; Currently always nil, but should be populated at some point.
   [:td.smallest "{{ item.item_type }}"]
   [:td.smallest "{{ item.source }}"]
   [:td.smallest "{{ item.last_modified | date:appView.dateFormat }}"]
   [:td.controls ctrl-button-item]])

(def project-table ^:private
  [:div
   navbar
   breadcrumbs
   [:table.table.table-hover.project-table.ng-hide
    {:dropzone "appView.currentProject"
     :on-drop "appView.dropped(dragE1, dropE1)"}

    project-table-head

    [:tbody

     subproject-rows
     content-rows
     [:tr {:ng-if "appView.currentProject._is_empty"}
      [:td {:colspan (dec (count (nth project-table-head 1)))} ;; Calculate the col-span by looking at the head.
       [:span {:ng-show "appView.pathToHere.length"} "Empty folder."]
       [:span {:ng-hide "appView.pathToHere.length"} "Please begin by creating a folder."]]]]]])

(def explorer ^:private
  [:tabset
   [:tab {:heading "Lists"} list-group]
   [:tab {:heading "Templates"} templates-group]])

(defn snippet [config]
  (html [:div.container-fluid.projects
    [:div.row-fluid {:ng-hide "auth.loggedIn"}
      [:p "Please log in to access MyMine"]]
    [:div.flex-row.guttered {:ng-show "auth.loggedIn"}
      [:div.flex-box project-table]
      [:div.flex-box.flex-col-4 {:ng-show "showExplorer"}
        [:div explorer]]]]))

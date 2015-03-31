(ns staircase.views.projects
  (:use [hiccup.core :only (html)]))

(defn- item-group
  [repeater init title is-owned?]
  [:ul.list-group.cap
   [:li.list-group-item
    {:ng-repeat repeater
     :ng-init init
     :droppable "item"}

    [:div.flex-row

     [:div.flex-column.stretched
      [:div
       [:input {:type "checkbox" :ng-model "item.selected"}]]]

     [:div.flex-box
      [:h4
       {:ng-click "item.selected = !item.selected"}
       title
       [:i.fa.fa-ellipsis-h
        {:ng-show "item.entity.description"
         :ng-click "showDescription = !showDescription"}]]
      [:p.text-muted.list-group-item-text
       {:ng-show "showDescription"}
       "{{ item.entity.description }}"]]

     [:div
      [:span.badge.origin {:ng-show is-owned?} [:i.fa.fa-user]]
      [:span.badge.origin
       {:title "{{ item.source }}"
        :ng-class "item.meta.color"}
       "{{ item.source | limitTo:1 }}"]]

     ]]])

(def list-group ^:private
  (item-group "item in appView.lists | orderBy:'entity.title'"
              "list = item.entity"
              "{{list.title}}"
              "list.authorized"))

(def templates-group ^:private
  (item-group "item in appView.templates | orderBy:'entity.name'"
              "template = item.entity"
              "{{ template.title }}"
              "false"))

(def breadcrumbs ^:private
  [:ul.breadcrumb.list-inline.mymine.clearfix
   [:button.btn.btn-default.pull-right
    {:ng-click "appView.editing = !appView.editing"
     :ng-class "{active: appView.editing}"}
    [:i.fa.fa-2x.fa-edit]]
   [:li [:a {:ng-click "appView.goToRoot()"} "Home"]]
   [:li
    {:ng-repeat "segment in appView.pathToHere"
     :ng-class "{active: $last}"
     :ng-click "appView.goToPathSegment($index)"}
    [:a {:ng-disabled "$last"} "{{segment.name}}"]]])

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
    {:ng-show "appView.editing && !rowform.$visible"
     :dropdown true}
    [:button.btn.btn-default.btn-sm {:dropdown-toggle true} "Options"
     [:span.caret]]
    [:ul.dropdown-menu
     (for [{click :click text :text} actions]
       [:li {:ng-click click} [:a text]])]]])

(def ctrl-button-project ^:private
  (row-controls "appView.updateProject(project)"
                {:click "rowform.$show()"        :text "Edit Folder"}
                {:click "appView.deleteProject(project)" :text "Delete Folder"}))

(def ctrl-button-item ^:private
  (row-controls "appView.updateItem(appView.currentProject, item)"
                {:click "appView.deleteItem(appView.currentProject, item)"
                 :text "Remove {{item.item_type}} from Folder"}))

(defn- new-project-input
  [[prop placeholder]]
  [:div.form-group
   [:input.form-control
    {:ng-model (str "newProject" prop)
     :type "text"
     :placeholder placeholder}]])

(defn- navbar [config]
  [:div.navbar.navbar-custom.navbar-default
   [:div.collapse.navbar-collapse
    [:form.navbar-form.navbar-left
     {:ng-controller "ProjectCreator as projectCreator"}
     (for [opts [["Name" "Folder name..."] ["Desc" "optional description"]]]
       (new-project-input opts))
     [:button.btn.btn-default
      {:ng-class "{disabled: !newProjectName}"
       :ng-click "projectCreator.create()"} ;;appView.createProject(newProjectName, newProjectDesc)"}
      [:i.fa.fa-folder]
      " "
      (get-in config [:strings :projects.add-project])]]
    [:form.navbar-form.navbar-right
     [:button.btn.btn-default.pushleft
      {:ng-class "{active: appView.showExplorer}"
       :ng-disabled "!appView.allProjects.length"
       :ng-click "appView.showExplorer = !appView.showExplorer"}
      [:i.fa.fa-plus]
      " "
      (get-in config [:strings :projects.add-item])]]]])

(def project-table-head ^:private
  (letfn [(th [[text sorting]] [:th
                                {:ng-click (str "appView.tableSort = '" sorting "'")}
                                (when sorting
                                  [:i.fa.fa-sort {:ng-class (str "{'fa-sort-asc': appView.tableSort === '" sorting "'}")}])
                                text])]
    [:thead
     [:tr
      (map th
        [["Name" "title"]
         ["Description" "description"]
         ["Kind" "item_type"]
         ["Source" "source"]
         ["Modified" "last_modified"]
         [""  nil]])]]))

(def subproject-rows ^:private
  [:tr ;; The branch nodes, ie. the sub-projects
   {:ng-repeat "project in appView.currentProject.child_nodes | orderBy:appView.tableSort"
    :dropzone "project"
    :on-drop "appView.dropped(dragged, project)"}

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
   {:ng-repeat "item in appView.currentProject.contents | orderBy:appView.tableSort"
    :ng-mouseover "hoverIn()"
    :ng-mouseleave "hoverOut()"}
   [:td [:a
         {:href "{{ appView.getHref(item) }}"}
         [:i.fa {:ng-class "{'fa-list': item.item_type === 'List',
                             'fa-search': item.item_type === 'Template'}"}]
         "{{item.item_id}}"]]
   [:td          "{{ item.description }}"] ;; Currently always nil, but should be populated at some point.
   [:td.smallest "{{ item.item_type }}"]
   [:td.smallest "{{ item.source }}"]
   [:td.smallest "{{ item.last_modified | date:appView.dateFormat }}"]
   [:td.controls ctrl-button-item]])

(defn- project-table [config]
  [:div
   (navbar config)
   breadcrumbs
   [:table.table.table-hover.project-table
    {:ng-hide "!auth.loggedIn"
     :dropzone "appView.currentProject"
     :on-drop "appView.dropped(dragged, appView.currentProject)"}

    project-table-head

    [:tbody

     subproject-rows
     content-rows
     [:tr {:ng-if "appView.currentProject._is_empty"}
      [:td {:colspan ;; Calculate the col-span by looking at the head.
            (count (get-in project-table-head [1 1]))}
       [:div.flex-row.stretched
        [:div
          [:span
            {:ng-show "appView.pathToHere.length"}
            (get-in config [:strings :projects.empty-folder])]
          [:span
            {:ng-hide "appView.pathToHere.length"}
            (get-in config [:strings :projects.create-folder-to-begin])]]
        [:div
          [:button.btn.btn-lg.btn-primary
            {:ng-click "appView.createProject()"}
            "Create a folder"]]]
       ]]]]])

(defn- project-list
  [config]
  [:div.panel.panel-default
   [:div.panel-heading (get-in config [:strings :projects.projects])]
   [:div.list-group
    [:li.list-group-item
      {:ng-repeat "project in appView.allProjects | orderBy:'title'"
       :ng-click "appView.setCurrentProject(project)"
       :ng-class "{active: appView.currentProject.id === project.id}"
       }
      [:span.badge "{{project.item_count}}"]
      "{{project.title}}"]]])

(def explorer ^:private
  [:div.item-explorer
   [:button.btn.btn-primary.pull-right
    {:ng-disabled "!appView.selectedItems().length"
     :ng-click "appView.addAllSelected()"}
    [:i.fa.fa-2x.fa-plus-circle]]
   [:tabset
    [:tab {:heading "Lists"} list-group]
    [:tab {:heading "Templates"} templates-group]]])

(defn snippet [config]
  (html [:div.container-fluid.projects
    [:div.row-fluid {:ng-hide "auth.loggedIn"}
      [:p "Please log in to access MyMine"]]
    [:div.flex-row.guttered {:ng-show "auth.loggedIn"}
      [:div.flex-box.flex-col-2 {:ng-show "appView.currentProject.id"} ;; Show when not at root
        (project-list config)]
      [:div.flex-box (project-table config)]
      [:div.flex-box.flex-col-4 {:ng-show "appView.showExplorer"}
        explorer]]]))

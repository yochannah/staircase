(ns staircase.views.services
  (:require [hiccup.def :refer (defelem)]))

(defelem service-control-buttons []
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
    {:title "Remove this service"
     :ng-click "deleteService(service)"
     :ng-hide "service.editing"}
    [:i.fa.fa-times]]
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
   [:td (service-control-buttons)]])

(defelem add-service-controls []
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
        [:input.form-control {:name "url"
                              :required "true"
                              :ng-model "adding.mine.root"
                              :placeholder "location of the data source"}]]]
     [:div.form-group
      {:ng-class "{'has-error': adding.authError}"}
      [:label "API token"]
      [:input.form-control {:name "token"
                            :ng-model "adding.mine.token"
                            :placeholder "api token (optional)"}]]]]])

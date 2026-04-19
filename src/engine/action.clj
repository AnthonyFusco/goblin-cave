(ns engine.action
  (:require [engine.action.command :as command]))

(defn make-show-effect
  [args]
  {:action/type :show
   :action/args args})

(defn make-mutation-effect
  [target args]
  {:action/type :mutation
   :action/target target
   :action/args args})

(defn make-effects
  ([]
   {:action/effects '()})
  ([effects]
   {:action/effects (flatten (remove nil? (if (sequential? effects) effects [effects])))}))

(defn get-effect-list
  [effects]
  (:action/effects effects))

(def action-rules
  {:action/default
   {:actions
    {:action/advance (fn [{:keys [instance]}]
                 (let [{:keys [id action/target]} instance]
                   (command/teleport id target)))}}})


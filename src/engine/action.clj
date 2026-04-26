(ns engine.action
  (:require [engine.action.command :as command]))

(defn make-show-effect
  [args]
  {:action/type :show
   :action/args args})

(defn make-mutation-effect
  [target args]
  {:action/type :action/mutation
   :action/target target
   :action/mutation args})

(defn make-effects
  ([]
   {:action/effects '()})
  ([effects]
   {:action/effects (flatten (remove nil? (if (sequential? effects) effects [effects])))}))

(defn make-commands
  ([]
   {:action/commands '()})
  ([commands]
   {:action/commands commands}))

(defn make-effects-and-commands
  ([]
   {:action/effects '()
    :action/commands '()})
  ([{:keys [effects commands]}]
   {:action/effects (flatten (remove nil? (if (sequential? effects) effects [effects])))
    :action/commands commands}))

(defn get-effects
  [effects]
  (or (:action/effects effects) []))

(defn get-commands
  [commands]
  (or (:action/commands commands) []))

(defn is-commands?
  [o]
  (contains? o :action/commands))

(defn merge-effects
  [effects1 effects2]
  (make-effects-and-commands
   {:effects (concat (get-effects effects1) (get-effects effects2))
    :commands (into (get-commands effects1) (get-commands effects2))}))

(defn make-action
  ([type]
   {:action/type type
    :action/args {}})
  ([type args]
   {:action/type type
    :action/args args}))

(def action-rules
  {:action/default
   {:actions
    {:action/advance (fn [{:keys [instance]}]
                       (let [{:keys [id action/target]} instance]
                         (command/teleport id target)))}}})
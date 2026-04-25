(ns engine.rule-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [engine.rule :as rule]
            [engine.object :as object]
            [engine.action :as action]
            [engine.core :as core]
            [engine.mocks :as mocks]))

(deftest default-failed-action-test
  (testing "returns default failed action message"
    (is (= {:describe "Nothing happens"} (rule/default-failed-action)))))

(deftest filter-rules-test
  (testing "filters rules by target set and action key"
    (let [rules {:semantic/exit {:action/open (fn [_] {})}
                 :semantic/activable {:action/activate-target (fn [_] {})}
                 :intrinsic/flammable {:action/burn (fn [_] {})}}
          target-set #{:semantic/exit :intrinsic/flammable}
          action-key :action/open
          result (rule/filter-rules rules target-set action-key)]
      (is (= 1 (count result)))
      (is (= :semantic/exit (first (keys result))))))

  (testing "returns empty when no rules match action"
    (let [rules {:semantic/exit {:action/close (fn [_] {})}}
          target-set #{:semantic/exit}
          action-key :action/open
          result (rule/filter-rules rules target-set action-key)]
      (is (empty? result))))

  (testing "returns empty when no rules match target set"
    (let [rules {:semantic/activable {:action/activate-target (fn [_] {})}}
          target-set #{:semantic/exit}
          action-key :action/activate-target
          result (rule/filter-rules rules target-set action-key)]
      (is (empty? result)))))

(deftest make-handler-args-test
  (testing "creates handler args with state name"
    (let [rule-key :object/lever
          rule {:name "lever"}
          instance {:entity/state {:name "custom lever"}}
          action-params {:param "value"}
          result (rule/make-handler-args mocks/env rule-key rule instance action-params)]
      (is (= rule-key (:rule-key result)))
      (is (= rule (:rule result)))
      (is (= "custom lever" (:name result)))
      (is (= instance (:instance result)))
      (is (= action-params (:action-params result)))))

  (testing "uses rule name when no state name"
    (let [rule-key :object/lever
          rule {:name "lever"}
          instance {:entity/state {}}
          action-params {}
          result (rule/make-handler-args mocks/env rule-key rule instance action-params)]
      (is (= "lever" (:name result)))))

  (testing "uses rule-key name when no rule name"
    (let [rule-key :object/lever
          rule {}
          instance {}
          action-params {}
          result (rule/make-handler-args mocks/env rule-key rule instance action-params)]
      (is (= "lever" (:name result))))))

(deftest choose-rule-test
  (testing "returns nil for empty list"
    (is (nil? (rule/choose-rule []))))

  (testing "returns single rule"
    (is (= :single-rule (rule/choose-rule [:single-rule]))))

  (testing "chooses most specific rule (longest sequence)"
    (let [rules [:short #{:longer :sequence}]]
      (is (= #{:longer :sequence} (rule/choose-rule rules)))))

  (testing "chooses most specific rule among multiple"
    (let [rules [:a #{:b :c} #{:d :e :f}]]
      (is (= #{:d :e :f} (rule/choose-rule rules))))))

(deftest compute-effects-test
  (testing "computes effects for wooden door open action"
    (let [env (core/with-entities mocks/env [object/wooden-door])
          result (rule/apply-action env object/object-rules (action/make-action :action/open) {:entity/id "wooden-door"})]
      (is (contains? result :action/effects))
      (is (some #(= (:describe %) "You push open the creaky wooden door")
                (map :action/args (:action/effects result))))))

  (testing "computes effects for lever activation"
    (let [test-lever {:entity/id "test-lever"
                      :entity/rule-type :object/lever
                      :entity/state {:switched? false
                                      :action/target 0}}
          env (core/with-entities mocks/env [test-lever])
          result (rule/apply-action env object/object-rules (action/make-action :action/activate) {:entity/id "test-lever"})]
      (is (contains? result :action/effects))
      (let [effects (:action/effects result)]
        (is (some #(= (:action/type %) :show) effects))
        (is (some #(= (:action/type %) :action/mutation) effects)))))

  (testing "returns nothing happens for unsupported action"
    (let [env (core/with-entities mocks/env [object/wooden-door])
          result (rule/apply-action env object/object-rules (action/make-action :action/unknown) {:entity/id "wooden-door"})]
      (is (contains? result :action/effects))
      (is (some #(= (:describe %) "Nothing happens when you unknown the wooden-door")
                (map :action/args (:action/effects result))))))

  (testing "handles semantic rules for poisoned food eating"
    (let [env (core/with-entities mocks/env [object/boulgiboulga])
          result (rule/apply-action env object/object-rules (action/make-action :action/eat) {:entity/id "boulgiboulga"})]
      (is (contains? result :action/effects))
      (is (some #(str/includes? (:describe %) "Ouch! The boulgiboulga is poisoned")
                (map :action/args (:action/effects result))))))

  (testing "handles semantic rules for non-poisoned food eating"
    (let [food-rule {:semantics #{:semantic/food}}
          apple-obj {:entity/id "apple"
                     :entity/rule-type ::apple
                     :entity/state {}}
          rules (assoc object/object-rules ::apple food-rule)
          env (core/with-entities mocks/env [apple-obj])
          result (rule/apply-action env rules (action/make-action :action/eat) {:entity/id "apple"})]
      (is (contains? result :action/effects))
      (is (some #(str/includes? (:describe %) "Nom! No more apple")
                (map :action/args (:action/effects result))))))

  (testing "handles flammable objects with burn action and color parameter"
    (let [env (core/with-entities mocks/env [object/wooden-door])
          result (rule/apply-action env object/object-rules (action/make-action :action/burn {:color "blue"}) {:entity/id "wooden-door"})]
      (is (contains? result :action/effects))
      (is (some #(str/includes? (:describe %) "bursts into blue flames")
                (map :action/args (:action/effects result)))))))

(deftest perform-on-object-test
  (testing "performs action on object using object key"
    (let [door {:entity/id "test-door"
                :entity/rule-type :object/wooden-door
                :entity/state {}}
          env (core/with-entities mocks/env [door])
          result (rule/apply-action env object/object-rules (action/make-action :action/open) {:entity/id "test-door"})]
      (is (contains? result :action/effects))
      (is (some #(= (:describe %) "You push open the creaky wooden door")
                (map :action/args (:action/effects result))))))

  (testing "performs action on lever object"
    (let [lever {:entity/id "test-lever"
                 :entity/rule-type :object/lever
                 :entity/state {:switched? false
                                 :action/target 0}}
          env (core/with-entities mocks/env [lever])
          result (rule/apply-action env object/object-rules (action/make-action :action/activate) {:entity/id "test-lever"})]
      (is (contains? result :action/effects))
      (let [effects (:action/effects result)]
        (is (some #(= (:action/type %) :show) effects))
        (is (some #(= (:action/type %) :action/mutation) effects)))))

  (testing "activates the switched-off healing lever from room data"
    (let [env (core/with-entities mocks/env [mocks/switched-off-lever-of-healing])
          result (rule/apply-action env object/object-rules (action/make-action :action/activate) {:entity/id 0})]
      (is (contains? result :action/effects))
      (let [effects (:action/effects result)]
        (is (some #(= (:action/type %) :action/activate) effects))
        (is (some #(= (:action/type %) :action/heal) effects))
        (is (some #(= (:action/target (:action/args %)) {:entity/id 1})
                  (filter #(= (:action/type %) :action/activate) effects)))))))


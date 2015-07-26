(ns clojure.tools.emitter.jvm.core-test
  (:require [clojure.tools.emitter.jvm :as e]
            [clojure.test :refer :all]
            [clojure.tools.analyzer.passes :refer [schedule]])
  (:import [clojure.tools.emitter.jvm VictimClass]))

(deftest eval-test
  (is (= 1 (e/eval 1)))
  (is (= :a (e/eval :a)))
  (is (= {:foo [#{"bar"}]} (e/eval {:foo [#{"bar"}]})))
  (is (= 1 (e/eval '((fn [] 1)))))
  (is (= :foo (e/eval '((fn [x] x) :foo))))
  (is (= 1225 (e/eval '(apply + (range 50)))))
  (is (e/eval (cons 'hash-map (range 50))))
  (is (= (range 10) (e/eval '(for [x (range 10)] x))))
  (is (= [1 2] (e/eval '(:foo {:foo [1 2]}))))
  (is (= 3 (e/eval '(do (def a 3) 3))))
  (is (= 6 (e/eval '(do (def g (fn [x] (* x 3))) (g 2)))))
  (is (= 3 (e/eval '(first (remove #(not= 3 %) (filter odd? (map inc (range 10))))))))
  (is (thrown? Exception (e/eval '(throw (Exception. "Foo!")))))
  (is (= 'foo (e/eval '(quote foo))))
  (is (= #'conj (e/eval '(var conj))))
  (is (= {:a :b}
         (e/eval '(let [x :b] {:a x}))
         (e/eval '{:a :b})))
  (is (= #{:a :b}
         (e/eval '#{:a :b})))
  (is (= [:a :b]
         (e/eval '[:a :b])))
  (is (= {:a :b}
         (meta (e/eval '(with-meta [:c :d] {:a :b})))))
  (is (= :catch
         (e/eval '(try (throw (Exception. "Foo!"))
                       (catch AssertionError e :assertion)
                       (catch Exception e :catch)))))
  (is (= :finally
         (e/eval '(let [x (atom 0)]
                    (try (throw (Exception. "Foo!"))
                         (catch Exception e :catch)
                         (finally (reset! x :finally)))
                    @x))))
  (is (= 12
         (e/eval '(letfn [(six-times [y]
                            (* (twice y) 3))
                          (twice [x]
                            (* x 2))]
                    (six-times 2)))))
  (let [f (e/eval
           '(fn [x]
              (case x
                (1) 1
                (:a) 2
                ([:x 1] :y) 3
                :default)))]
    (is (= 1 (f 1)))
    (is (= 2 (f :a)))
    (is (= :default (f 6)))
    (is (= 3 (f :y)))
    (is (= 3 (f [:x 1]))))
  (is (= 3
         (e/eval
          '(do (def ^:dynamic *foo*)
               (binding [*foo* 3]
                 *foo*)))))
  (is (= (apply + (range 3))
         (e/eval
          '(do (defmacro mapply [e & args]
                 `(~e ~@(butlast args) ~@(last args)))
               (mapply + 0 1 2 nil)))))
  (is (= [:a 5] (e/eval '(let [x 5] [:a x]))))
  (is (= #{:a :b} (e/eval '(let [x :b] #{:a x}))))
  (is (= {:a :b} (meta (e/eval '(let [x 3] ^{:a :b} {:a x})))))
  (is (= Double/MAX_VALUE (e/eval 'Double/MAX_VALUE)))
  (is (e/eval '(instance? java.lang.String "aaaaa")))
  (is (= (apply + (range 50))
         (e/eval '(loop [acc 0
                         col (range 50)]
                    (if-not (empty? col)
                      (recur (+ acc (first col)) (rest col))
                      acc)))))
  (is (= 3 (e/eval '(do (let [x (Object.)]
                          (locking x
                            (println "Got lock!")
                            (Thread/sleep 1)
                            (println "Done with lock!")))
                        3))))
  (is (= Double (e/eval 'Double)))
  (is (= 3 (e/eval
            '(do (defprotocol Foo
                   (blah
                     [this x]))
                 (let [r (reify Foo (blah [_ x] x))]
                   (blah r 3))))))
  (is (= 3 (e/eval
            '(do (deftype AType [x])
                 (.x (AType. 3))))))
  (is (e/eval
       '(do (set! *unchecked-math* true)
            *unchecked-math*)))
  (is (e/eval
       '(do (import '[clojure.tools.emitter.jvm VictimClass])
            (let [*v* VictimClass/foo]
              (set! VictimClass/foo (new Object))
              (not= *v* System/out))))))

;; (deftest load-core-test
;;   (is (= nil (e/load "/clojure.core"))))

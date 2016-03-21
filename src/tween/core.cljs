(ns tween.core
  (:require [reagent.core :as reagent :refer [create-class atom dom-node props]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

;; -------------------------
;; Constructors
(defonce Stage js/createjs.Stage)
(defonce Shape js/createjs.Shape)
(defonce Tween js/createjs.Tween)
(defonce Ticker js/createjs.Ticker)
(defonce Ease js/createjs.Ease)

;; Sample code from http://www.createjs.com/getting-started/tweenjs

;; var stage = new createjs.Stage("demoCanvas");
;; var circle = new createjs.Shape();
;; circle.graphics.beginFill("Crimson").drawCircle(0, 0, 50);
;; circle.x = 100;
;; circle.y = 100;
;; stage.addChild(circle);
;; createjs.Tween.get(circle, {loop: true})
;;  .to({x: 400}, 1000, createjs.Ease.getPowInOut(4))
;;  .to({alpha: 0, y: 75}, 500, createjs.Ease.getPowInOut(2))
;;  .to({alpha: 0, y: 125}, 100)
;;  .to({alpha: 1, y: 100}, 500, createjs.Ease.getPowInOut(2))
;;  .to({x: 100}, 800, createjs.Ease.getPowInOut(2));
;; createjs.Ticker.setFPS(60);
;; createjs.Ticker.addEventListener("tick", stage);

(defn create-circle
  [base {:keys [fill r x y] :as props}]
  (.. base
      -graphics
      (beginFill fill)
      (drawCircle 0 0 r))
  ;; set pos
  (when x
    (aset base "x" x))
  (when y
    (aset base "y" y))
  base)

(defn shape-maker
  [{:keys [type] :as props}]
  (let [base (Shape.)]
    (case type
      :circle (create-circle base props)
      ;; add others
      (js/console.error "no shape defined for " type))))

(defn generate-shape-movement
  [{:keys [ref loop movement]}]
  (let [act-on (.get Tween. ref #js {"loop" loop})]
    (doseq [{:keys [type item axis pos t ease alpha] :as m} movement]
      (let [shape-def (clj->js (cond-> {(name axis) pos}
                                       alpha (assoc "alpha" alpha)))
            move-fn (cond
                      ease (.getPowInOut Ease. ease)
                      :else nil)]
        (.. act-on
            (to shape-def t move-fn))))))

(defn TweenMovement
  "Prop types
  :id canvas id to use
  :item type of element to move (can expand to multiple elements and must map to movement maps)
  :fps-ticker ticker time
  :movement Vector of maps that define movement on an x-y plane for the item(s) defined"
  [init-props]
  (create-class
    {:component-will-unmount
     (fn [this]
       ;; add cleanup code for dom removal not doing this
       )
     :component-did-mount
     (fn [this]
       (let [node (dom-node this)
             {:keys [id item fps-ticker movement]} (props this)]
         (if-let [shape (shape-maker item)]
           (let [stage (Stage. id)]
             ;; add shape into canvas
             (.addChild stage shape)
             ;; add movement
             (generate-shape-movement {:ref shape
                                       :loop true
                                       :movement movement})
             ;; add ticker
             (.setFPS Ticker. fps-ticker)
             ;; add tick listener to make shape appear
             (.addEventListener Ticker. "tick" stage))
           (js/console.error "Not sure what kind of shape you made"))))
     :reagent-render
     (fn [{:keys [id] :as props}]
       [:canvas {:id id :height 500 :width 500}])}))

(defn home-page []
  [:div
   [:h2 "Welcome to Reagent"]
   [:p "Tween sample from main page integrated with reagent-frontend
    template not using externs though"]
   [TweenMovement {:id "demoCanvas"
                   :item {:id 1
                          :type :circle
                          :fill "black"
                          :r 50
                          :x 100
                          :y 100}
                   :fps-ticker 60
                   :movement [{:item 1 :axis :x :pos 400 :t 1000 :ease 4}
                              {:item 1 :axis :y :pos 75 :t 500 :ease 2 :alpha 0}
                              {:item 1 :axis :y :pos 125 :t 100 :alpha 0}
                              {:item 1 :axis :y :pos 100 :t 500 :ease 2 :alpha 1}
                              {:item 1 :axis :x :pos 100 :t 800 :ease 2}]}]
   [TweenMovement {:id "demoCanvas2"
                   :item {:id 1
                          :type :circle
                          :fill "red"
                          :r 50
                          :x 100
                          :y 100}
                   :fps-ticker 60
                   :movement [{:item 1 :axis :x :pos 100 :t 1000 :ease 4}
                              {:item 1 :axis :y :pos 100 :t 500 :ease 2 :alpha 0}
                              {:item 1 :axis :y :pos 125 :t 100 :alpha 0}
                              {:item 1 :axis :y :pos 75 :t 500 :ease 2 :alpha 1}
                              {:item 1 :axis :x :pos 400 :t 800 :ease 2}]}]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

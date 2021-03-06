package com.carterhudson.redux_kotlin_android

import com.carterhudson.redux_kotlin_android.util.ManagedSubscription
import com.carterhudson.redux_kotlin_android.util.Renderer
import com.carterhudson.redux_kotlin_android.util.State
import com.carterhudson.redux_kotlin_android.util.StateObservable
import com.carterhudson.redux_kotlin_android.util.StateObserver
import com.carterhudson.redux_kotlin_android.util.notifyAll
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify

class ManagedSubscriptionTest : BehaviorSpec() {
  val renderer = spyk(
      object : Renderer<State> {
        override fun render(state: State) {

        }
      }
  )

  val observers = mutableListOf<StateObserver<State, *>>()

  val state = object : State {}

  init {
    Given("a state observable") {
      val stateObservable = object : StateObservable<State> {
        override fun <OutStateT : State> subscribe(
          render: (OutStateT) -> Unit,
          distinct: Boolean,
          select: (State) -> OutStateT
        ): ManagedSubscription = object : ManagedSubscription() {}.also {
          observers.add(
              StateObserver(renderer::render, false, select, it).also { obs ->
                obs.notify(select(state))
              }
          )
        }
      }

      And("it is subscribed to") {
        val subscription: ManagedSubscription =
          stateObservable.subscribe(renderer::render, false) { it }

        Then("render should be called") {
          verify(exactly = 1) { renderer.render(any()) }
        }

        When("it is paused") {
          subscription.isPaused() shouldBe false
          subscription.pause()
          subscription.isPaused() shouldBe true

          And("observers are notified") {
            observers.notifyAll(state)

            Then("no state should be received") {
              verify(exactly = 1) { renderer.render(any()) }
            }
          }
        }

        When("it is resumed") {
          subscription.isPaused() shouldBe true
          subscription.resume()
          subscription.isPaused() shouldBe false

          And("observers are notified") {
            observers.notifyAll(state)

            Then("state should be received") {
              verify(exactly = 2) { renderer.render(any()) }
            }
          }
        }

        When("it is canceled") {
          subscription.isCanceled() shouldBe false
          subscription.cancel()
          subscription.isCanceled() shouldBe true

          And("observers are notified") {
            observers.notifyAll(state)

            Then("no state should be received") {
              verify(exactly = 2) { renderer.render(any()) }
            }
          }
        }
      }
    }
  }
}
package com.carterhudson.redux_kotlin_android.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.carterhudson.redux_kotlin_android.util.ManagedSubscription
import com.carterhudson.redux_kotlin_android.util.State
import com.carterhudson.redux_kotlin_android.util.addAll
import com.carterhudson.redux_kotlin_android.util.cancel
import com.carterhudson.redux_kotlin_android.util.lifecycle.LifecycleAction
import com.carterhudson.redux_kotlin_android.util.pause
import com.carterhudson.redux_kotlin_android.util.resume

abstract class ReduxFragment<StateT : State, ComponentStateT : State> : Fragment() {

  private lateinit var viewModel: ReduxViewModel<StateT>
  private lateinit var viewComponent: ViewComponent<ComponentStateT>
  private var subscriptions = mutableListOf<ManagedSubscription>()

  val dispatch by lazy { viewModel.dispatch }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onCreateViewModel().also { viewModel = it }
    onViewModelCreated(viewModel)
  }

  abstract fun onCreateViewModel(): ReduxViewModel<StateT>

  open fun onViewModelCreated(viewModel: ReduxViewModel<StateT>) {
    //optional
  }

  /**
   * Overridden from [Fragment.onCreateView].
   * Assigns [viewComponent] reference.
   * Dispatches [LifecycleAction.CreatingView].
   *
   * @return the root view of the created [viewComponent]
   */
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = onCreateViewComponent(inflater, container, savedInstanceState)
    .also { viewComponent = it }
    .also { viewModel.dispatch(LifecycleAction.CreatingView(this)) }
    .also { onViewComponentCreated(viewComponent) }
    .root()

  /**
   * Delegate method (Required).
   * Invoked in order to obtain a [ViewComponent] instance.
   * Called from [Fragment.onCreateView]
   *
   * @param inflater layout inflater provided by [Fragment.onCreateView]
   * @param container container provided by [Fragment.onCreateView]
   * @param savedInstanceState bundle provided by [Fragment.onCreateView]
   * @return the created [ViewComponent] instance.
   */
  abstract fun onCreateViewComponent(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): ViewComponent<ComponentStateT>

  open fun onViewComponentCreated(viewComponent: ViewComponent<ComponentStateT>) {
    //optional
  }

  /**
   * Overridden from [Fragment.onViewCreated]
   * Handles subscribing to state & side effects
   * Dispatches [LifecycleAction.ViewCreated]
   */
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    with(viewModel) {
      subscriptions.addAll(
        subscribe(viewComponent::render, distinct(), ::onSelectState),
        subscribe(::performSideEffect)
      )
    }

    dispatch(LifecycleAction.ViewCreated(this))
  }

  /**
   * Delegate method (Optional).
   * Override in order to change distinct vs indistinct render calls.
   *
   * @return boolean indicating distinct state rendering.
   */
  protected open fun distinct(): Boolean = true

  /**
   * Delegate method (Optional)
   * Override in order to perform side effects.
   *
   * @param state
   * @param action
   */
  protected open fun performSideEffect(state: StateT, action: Any) {
    //optional
  }

  abstract fun onSelectState(inState: StateT): ComponentStateT

  override fun onStart() {
    super.onStart()
    dispatch(LifecycleAction.Starting(this))
  }

  override fun onResume() {
    super.onResume()
    subscriptions.resume()
    dispatch(LifecycleAction.Resuming(this))
  }

  override fun onPause() {
    dispatch(LifecycleAction.Pausing(this))
    subscriptions.pause()
    super.onPause()
  }

  override fun onStop() {
    dispatch(LifecycleAction.Stopping(this))
    subscriptions.cancel()
    super.onStop()
  }

  override fun onDestroy() {
    dispatch(LifecycleAction.Destroying(this))
    super.onDestroy()
  }
}
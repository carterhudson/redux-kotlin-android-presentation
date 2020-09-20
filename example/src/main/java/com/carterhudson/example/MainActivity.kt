package com.carterhudson.example

import android.os.Bundle
import android.widget.Toast
import com.carterhudson.example.feature.counter.CounterState
import com.carterhudson.example.feature.counter.CounterViewComponent
import com.carterhudson.redux_kotlin_android.presentation.ReduxActivity
import com.carterhudson.redux_kotlin_android.presentation.ReduxViewModel
import com.carterhudson.redux_kotlin_android.presentation.ViewComponent

class MainActivity : ReduxActivity<AppState, CounterState>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExampleApp.injector.inject(this)
    }

    override fun onCreateViewModel(): ReduxViewModel<AppState> = ExampleApp.injector.appViewModel()

    override fun onCreateViewComponent(): ViewComponent<CounterState> =
        CounterViewComponent(inflater = layoutInflater, dispatch = reduxViewModel.dispatch)

    override fun onSelectState(state: AppState): CounterState = state.counterState

    override fun performSideEffect(state: AppState, action: Any) {
        Toast.makeText(this, "Side effect triggered for $action!", Toast.LENGTH_SHORT).show()
    }
}

package com.sample.app.presentation.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sample.app.domain.models.IPagingModel
import com.sample.app.domain.usecase.GetMemesFlowUseCase
import com.sample.app.domain.usecase.GetMemesUseCase
import com.sample.app.presentation.utils.SingleLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class MemesViewModel @Inject constructor(
  private val getMemesUseCase: GetMemesUseCase,
  private val getMemesFlowUseCase: GetMemesFlowUseCase
) : ViewModel() {

  private val _playersData = MutableLiveData<List<IPagingModel>>()
  val playersData = _playersData as LiveData<List<IPagingModel>>
  private val _showProgressbar = SingleLiveData<Boolean>()
  val showProgressbar = _showProgressbar as LiveData<Boolean>
  private val _showError = SingleLiveData<String>()
  val showError = _showError as LiveData<String>

  /**
   * Get Players data from flow usecase as paged content and updates livedata for the
   * players list on home
   */
  @ExperimentalCoroutinesApi
  fun getMemes() {
    _showProgressbar.value = true

    //Paging 3 implementation
    /*getMemesFlowUseCase.invoke(viewModelScope, Unit, onResult = { playersData ->
      viewModelScope.launch {
        playersData.collectLatest {

        }
      }

    })*/

    //Before applying paging
    getMemesUseCase.invoke(viewModelScope,
      params = Unit,
      onSuccess = { result ->
        Log.i(TAG, "result: $result")
        _playersData.postValue(result.data.memes)
        _showProgressbar.value = false
      },
      onFailure = { apiError ->
        _showError.value = apiError.message ?: "Error"
        _showProgressbar.value = false
      })
  }

  companion object {
    private val TAG = MemesViewModel::class.java.simpleName
  }

}
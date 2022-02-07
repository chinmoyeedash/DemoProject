package com.sample.app.domain.usecase

import androidx.paging.PagingData
import com.sample.app.data.repository.SampleRepository
import com.sample.app.domain.models.IPagingModel
import com.sample.app.domain.repository.ISampleRepository
import com.sample.app.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ExperimentalCoroutinesApi
class GetMemesFlowUseCase @Inject constructor(
  private val repository: ISampleRepository
) : FlowUseCase<Unit, PagingData<IPagingModel>>() {
  override suspend fun run(params: Unit): Flow<PagingData<IPagingModel>> {
    return repository.getMemesFlow()
  }
}

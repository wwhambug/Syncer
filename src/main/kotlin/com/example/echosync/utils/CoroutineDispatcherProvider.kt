package com.example.echosync.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 코루틴 디스패처 제공자
 * - 테스트 용이성 및 중앙 관리
 * - 필요시 메인, IO, 기본 디스패처 교체 가능
 */
interface DispatcherProvider {
    fun main(): CoroutineDispatcher = Dispatchers.Main
    fun io(): CoroutineDispatcher = Dispatchers.IO
    fun default(): CoroutineDispatcher = Dispatchers.Default
    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
}

/**
 * 기본 구현체 (실제 환경)
 */
class DefaultDispatcherProvider : DispatcherProvider

/**
 * 테스트용 구현체 (필요 시)
 */
class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) : DispatcherProvider {
    override fun main(): CoroutineDispatcher = testDispatcher
    override fun io(): CoroutineDispatcher = testDispatcher
    override fun default(): CoroutineDispatcher = testDispatcher
    override fun unconfined(): CoroutineDispatcher = testDispatcher
}
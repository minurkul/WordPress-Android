package org.wordpress.android.ui.bloggingprompts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.posts.BLOGGING_PROMPT_TAG
import org.wordpress.android.ui.posts.BloggingPromptsEditorBlockMapper
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel.EditorLoadedPrompt
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig
import java.util.Date

@ExperimentalCoroutinesApi
class EditorBloggingPromptsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var siteModel: SiteModel

    private lateinit var viewModel: EditorBloggingPromptsViewModel
    private var loadedPrompt: EditorLoadedPrompt? = null

    private val bloggingPrompt = BloggingPromptsResult(
        model = BloggingPromptModel(
            id = 123,
            text = "title",
            title = "",
            content = "content",
            date = Date(),
            isAnswered = false,
            attribution = "",
            respondentsCount = 5,
            respondentsAvatarUrls = listOf()
        )
    )

    private val bloggingPromptsStore: BloggingPromptsStore = mock {
        onBlocking { getPromptById(any(), any()) } doReturn flowOf(bloggingPrompt)
    }
    private val bloggingPromptsEditorBlockMapper: BloggingPromptsEditorBlockMapper = mock {
        on { it.map(any()) } doReturn (bloggingPrompt.model?.content ?: "")
    }
    private val bloggingPromptsEnhancementsFeatureConfig: BloggingPromptsEnhancementsFeatureConfig = mock()

    @Before
    fun setUp() {
        viewModel = EditorBloggingPromptsViewModel(
            bloggingPromptsStore,
            testDispatcher(),
            bloggingPromptsEditorBlockMapper,
            bloggingPromptsEnhancementsFeatureConfig,
        )

        viewModel.onBloggingPromptLoaded.observeForever {
            it.applyIfNotHandled {
                loadedPrompt = this
            }
        }
    }

    @Test
    fun `starting VM fetches a prompt and posts it to onBloggingPromptLoaded`() = test {
        viewModel.start(siteModel, 123)

        assertThat(loadedPrompt?.content).isEqualTo(bloggingPrompt.model?.content)
        assertThat(loadedPrompt?.promptId).isEqualTo(bloggingPrompt.model?.id)
        assertThat(loadedPrompt?.tag).isEqualTo(BLOGGING_PROMPT_TAG)

        verify(bloggingPromptsStore, times(1)).getPromptById(any(), any())
    }

    @Test
    fun `should NOT execute start method if prompt ID is less than 0`() = test {
        viewModel.start(siteModel, -1)
        verify(bloggingPromptsStore, times(0)).getPromptById(any(), any())
    }

    @Test
    fun `should NOT CALL block mapper if enhancements feature flag is DISABLED`() {
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(false)
        viewModel.start(siteModel, 123)
        verify(bloggingPromptsEditorBlockMapper, times(0)).map(any())
    }

    @Test
    fun `should CALL block mapper if enhancements feature flag is ENABLED`() {
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.start(siteModel, 123)
        verify(bloggingPromptsEditorBlockMapper, times(1)).map(any())
    }
}

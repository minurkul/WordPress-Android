package org.wordpress.android.ui.posts

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialContainerVisibility
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.social.JetpackSocialSharingTracker
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.config.JetpackSocialFeatureConfig

@OptIn(ExperimentalCoroutinesApi::class)
class EditorJetpackSocialViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var jetpackSocialFeatureConfig: JetpackSocialFeatureConfig

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase

    @Mock
    lateinit var getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase

    @Mock
    lateinit var getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase

    @Mock
    lateinit var jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper

    @Mock
    lateinit var postSocialSharingModelMapper: PostSocialSharingModelMapper

    @Mock
    lateinit var publicizeTableWrapper: PublicizeTableWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var editPostRepository: EditPostRepository

    @Mock
    lateinit var jetpackSocialSharingTracker: JetpackSocialSharingTracker

    private val showJetpackSocialContainerObserver: Observer<JetpackSocialContainerVisibility> = mock()
    private val jetpackSocialUiStateObserver: Observer<JetpackSocialUiState> = mock()
    private val actionEventsObserver: Observer<ActionEvent> = mock()

    private lateinit var classToTest: EditorJetpackSocialViewModel

    @Before
    fun setUp() {
        classToTest = EditorJetpackSocialViewModel(
            jetpackSocialFeatureConfig = jetpackSocialFeatureConfig,
            accountStore = accountStore,
            getPublicizeConnectionsForUserUseCase = getPublicizeConnectionsForUserUseCase,
            getJetpackSocialShareMessageUseCase = getJetpackSocialShareMessageUseCase,
            getJetpackSocialShareLimitStatusUseCase = getJetpackSocialShareLimitStatusUseCase,
            jetpackUiStateMapper = jetpackUiStateMapper,
            postSocialSharingModelMapper = postSocialSharingModelMapper,
            publicizeTableWrapper = publicizeTableWrapper,
            appPrefsWrapper = appPrefsWrapper,
            jetpackSocialSharingTracker = jetpackSocialSharingTracker,
            bgDispatcher = testDispatcher(),
        )

        whenever(postSocialSharingModelMapper.map(any(), any())).thenReturn(FAKE_SOCIAL_SHARING_MODEL)

        classToTest.jetpackSocialContainerVisibility.observeForever(showJetpackSocialContainerObserver)
        classToTest.jetpackSocialUiState.observeForever(jetpackSocialUiStateObserver)
        classToTest.actionEvents.observeForever(actionEventsObserver)
    }

    @Test
    fun `Should NOT load jetpack social if FF is disabled`() = test {
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(false)
        classToTest.start(FAKE_SITE_MODEL, editPostRepository)
        verify(getPublicizeConnectionsForUserUseCase, never()).execute(any(), any(), any())
    }

    @Test
    fun `Should hide jetpack social container if FF is disabled`() = test {
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(false)
        classToTest.start(FAKE_SITE_MODEL, editPostRepository)
        verify(showJetpackSocialContainerObserver).onChanged(JetpackSocialContainerVisibility.ALL_HIDDEN)
    }

    @Test
    fun `Should show jetpack social container if FF is enabled`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(showJetpackSocialContainerObserver).onChanged(JetpackSocialContainerVisibility.ALL_VISIBLE)
    }

    @Test
    fun `Should get publicize connections for user if jetpack social FF is enabled`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(fakeSiteModel(), editPostRepository)
        verify(getPublicizeConnectionsForUserUseCase).execute(FAKE_REMOTE_SITE_ID, FAKE_USER_ID)
    }

    @Test
    fun `Should get jetpack social share limit status if jetpack social FF is enabled`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(fakeSiteModel(), editPostRepository)
        verify(getJetpackSocialShareLimitStatusUseCase).execute(FAKE_SITE_MODEL)
    }

    @Test
    fun `Should map no connections UI if connections are empty, FF is enabled and never dismissed by user`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(PublicizeService().apply { status = PublicizeService.Status.OK }))
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(appPrefsWrapper.getShouldShowJetpackSocialNoConnections(any(), any()))
            .thenReturn(true)

        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(jetpackUiStateMapper).mapNoConnections(any(), any())
    }

    @Test
    fun `Should emit no connections UI if connections are empty, FF is enabled and never dismissed by user`() = test {
        val noConnections = JetpackSocialUiState.NoConnections(
            trainOfIconsModels = listOf(),
            message = "message",
            connectProfilesButtonLabel = "connect label",
            onConnectProfilesClick = {},
            notNowButtonLabel = "not now label",
            onNotNowClick = {},
        )
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(PublicizeService().apply { status = PublicizeService.Status.OK }))
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapNoConnections(any(), any()))
            .thenReturn(noConnections)
        whenever(appPrefsWrapper.getShouldShowJetpackSocialNoConnections(any(), any()))
            .thenReturn(true)
        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(jetpackSocialUiStateObserver).onChanged(noConnections)
    }

    @Test
    fun `Should hide social sharing container if connections are empty but no connections dismissed by user`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(PublicizeService().apply { status = PublicizeService.Status.OK }))
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(appPrefsWrapper.getShouldShowJetpackSocialNoConnections(any(), any()))
            .thenReturn(false)

        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(showJetpackSocialContainerObserver).onChanged(JetpackSocialContainerVisibility.ALL_HIDDEN)
    }

    @Test
    fun `Should map loaded UI state if connections list is NOT empty and jetpack social FF is enabled`() = test {
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(listOf(FAKE_PUBLICIZE_CONNECTION))
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(jetpackUiStateMapper).mapLoaded(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `Should emit loaded UI state if connections list is NOT empty and jetpack social FF is enabled`() = test {
        val loaded = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = FAKE_POST_SOCIAL_CONNECTION,
                    onConnectionClick = { _, _ -> },
                    enabled = false
                )
            ),
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            showShareLimitUi = true,
            isShareMessageEnabled = false,
            shareMessage = "message",
            onShareMessageClick = {},
            subscribeButtonLabel = "label"
        ) {}

        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(listOf(FAKE_PUBLICIZE_CONNECTION))
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapLoaded(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(loaded)
        classToTest.start(fakeSiteModel(true), editPostRepository)
        verify(jetpackSocialUiStateObserver).onChanged(loaded)
    }

    @Test
    fun `Should reload jetpack social on screen shown if last emitted action was OpenSocialConnectionsList`() = test {
        val loaded = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = FAKE_POST_SOCIAL_CONNECTION,
                    onConnectionClick = { _, _ -> },
                    enabled = false
                )
            ),
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            showShareLimitUi = true,
            isShareMessageEnabled = false,
            shareMessage = "message",
            onShareMessageClick = {},
            subscribeButtonLabel = "label"
        ) {}

        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(listOf(FAKE_PUBLICIZE_CONNECTION))
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapLoaded(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(loaded)

        classToTest.start(fakeSiteModel(true), editPostRepository)
        classToTest.onResume(JetpackSocialFlow.POST_SETTINGS)

        verify(jetpackSocialUiStateObserver).onChanged(loaded)
    }

    @Test
    fun `Should NOT reload jetpack social on screen shown if last emitted action was NOT OpenSocialConnectionsList`() {
        classToTest.onResume(JetpackSocialFlow.POST_SETTINGS)
        verify(jetpackSocialUiStateObserver, never()).onChanged(any())
    }

    @Test
    fun `Should emit OpenSocialConnectionList when onJetpackSocialConnectProfilesClick is called`() {
        classToTest.start(fakeSiteModel(), editPostRepository)
        classToTest.onJetpackSocialConnectProfilesClick(JetpackSocialFlow.POST_SETTINGS)
        verify(actionEventsObserver).onChanged(
            ActionEvent.OpenSocialConnectionsList(
                siteModel = FAKE_SITE_MODEL,
            )
        )
    }

    private fun fakeSiteModel(supportsPublicize: Boolean = false): SiteModel = FAKE_SITE_MODEL.apply {
        siteId = FAKE_REMOTE_SITE_ID
        if (supportsPublicize) {
            origin = SiteModel.ORIGIN_WPCOM_REST
            hasCapabilityPublishPosts = true
            setIsPublicizePermanentlyDisabled(false)
        }
    }

    private fun mockUserId() {
        val accountModel: AccountModel = mock {
            on { userId } doReturn FAKE_USER_ID
        }
        whenever(accountStore.account).thenReturn(accountModel)
    }

    companion object {
        private const val FAKE_REMOTE_SITE_ID = 123L
        private const val FAKE_USER_ID = 456L
        private val FAKE_SITE_MODEL = SiteModel().apply {
            siteId = 12345
        }
        private val FAKE_PUBLICIZE_CONNECTION = PublicizeConnection().apply {
            connectionId = 0
            service = "tumblr"
            label = "Tumblr"
            externalId = "myblog.tumblr.com"
            externalName = "My blog"
            externalProfilePictureUrl =
                "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png"
        }
        private val FAKE_POST_SOCIAL_CONNECTION = PostSocialConnection(
            connectionId = 1,
            service = "service",
            label = "label",
            externalId = "externalId",
            externalName = "externalName",
            iconUrl = "iconUrl",
            isSharingEnabled = true
        )
        private val FAKE_SOCIAL_SHARING_MODEL = PostSocialSharingModel(
            title = "Title",
            description = "Description",
            iconModels = emptyList(),
        )
    }
}

package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.stats.insights.SummaryStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightDetails
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.HUNDRED_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TotalSubscribersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val summaryStore: SummaryStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val totalStatsMapper: TotalStatsMapper,
    private val actionCardHandler: ActionCardHandler,
    private val statsUtils: StatsUtils
) : StatelessUseCase<Int>(StatsStore.SubscriberType.TOTAL_SUBSCRIBERS, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(TitleWithMore(R.string.stats_view_total_subscribers))

    override fun buildEmptyItem() = buildUiModel(0)

    override suspend fun loadCachedData() = summaryStore.getSummary(statsSiteProvider.siteModel)?.followers

    override suspend fun fetchRemoteData(forced: Boolean): State<Int> {
        val response = summaryStore.fetchSummary(statsSiteProvider.siteModel, forced)
        val model = response.model?.followers
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: Int): List<BlockListItem> {
        addActionCard(domainModel)
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        items.add(ValueWithChartItem(
            value = statsUtils.toFormattedString(domainModel, MILLION),
            extraBottomMargin = true
        ))
        if (totalStatsMapper.shouldShowFollowersGuideCard(domainModel)) {
            items.add(ListItemGuideCard(resourceProvider.getString(R.string.stats_insights_followers_guide_card)))
        }
        return items
    }

    private fun addActionCard(domainModel: Int) {
        if (domainModel <= 1) actionCardHandler.display(InsightType.ACTION_GROW)
    }

    private fun buildTitle() = TitleWithMore(
        R.string.stats_view_total_subscribers
    )


    class TotalSubscribersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val summaryStore: SummaryStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val actionCardHandler: ActionCardHandler,
        private val statsUtils: StatsUtils
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = TotalSubscribersUseCase(
            mainDispatcher,
            backgroundDispatcher,
            summaryStore,
            statsSiteProvider,
            resourceProvider,
            totalStatsMapper,
            actionCardHandler,
            statsUtils
        )
    }
}

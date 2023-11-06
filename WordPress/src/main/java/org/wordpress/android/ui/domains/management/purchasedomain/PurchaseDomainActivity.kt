package org.wordpress.android.ui.domains.management.purchasedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingSite
import org.wordpress.android.ui.domains.management.purchasedomain.composable.PurchaseDomainScreen
import javax.inject.Inject

private typealias NotImplemented = Unit

@AndroidEntryPoint
class PurchaseDomainActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: PurchaseDomainViewModel.Factory

    private val viewModel: PurchaseDomainViewModel by viewModels {
        PurchaseDomainViewModel.provideFactory(viewModelFactory, productArg, domainArg, privacyArg)
    }

    private val productArg: Int get() = intent.getIntExtra(PICKED_PRODUCT_ID, 0)

    private val domainArg: String get() = intent.getStringExtra(PICKED_DOMAIN_KEY) ?: error("Domain cannot be null.")

    private val privacyArg: Boolean get() = intent.getBooleanExtra(PICKED_DOMAIN_PRIVACY, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                PurchaseDomainScreen(
                    onNewDomainCardSelected = viewModel::onNewDomainSelected,
                    onExistingSiteCardSelected = viewModel::onExistingSiteSelected,
                    onBackPressed = viewModel::onBackPressed,
                )
            }
        }
        observeActions()
    }

    private fun observeActions() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: PurchaseDomainViewModel.ActionEvent) {
        when (actionEvent) {
            is GoToDomainPurchasing -> NotImplemented
            is GoToExistingSite -> NotImplemented
            GoBack -> onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val PICKED_PRODUCT_ID: String = "picked_product_id"
        const val PICKED_DOMAIN_KEY: String = "picked_domain_key"
        const val PICKED_DOMAIN_PRIVACY: String = "picked_domain_privacy"
    }
}

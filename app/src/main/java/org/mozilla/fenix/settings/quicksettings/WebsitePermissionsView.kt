/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.isVisible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.quicksettings_permissions.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.PhoneFeature.AUTOPLAY
import org.mozilla.fenix.settings.PhoneFeature.CAMERA
import org.mozilla.fenix.settings.PhoneFeature.MICROPHONE
import org.mozilla.fenix.settings.PhoneFeature.LOCATION
import org.mozilla.fenix.settings.PhoneFeature.NOTIFICATION
import org.mozilla.fenix.settings.PhoneFeature.PERSISTENT_STORAGE
import org.mozilla.fenix.settings.PhoneFeature.MEDIA_KEY_SYSTEM_ACCESS
import org.mozilla.fenix.settings.quicksettings.WebsitePermissionsView.PermissionViewHolder.SpinnerPermission
import org.mozilla.fenix.settings.quicksettings.WebsitePermissionsView.PermissionViewHolder.ToggleablePermission
import java.util.EnumMap

/**
 *  Contract declaring all possible user interactions with [WebsitePermissionsView]
 */
interface WebsitePermissionInteractor {
    /**
     * Indicates there are website permissions allowed / blocked for the current website.
     * which, status which is shown to the user.
     */
    fun onPermissionsShown()

    /**
     * Indicates the user changed the status of a certain website permission.
     *
     * @param permissionState current [WebsitePermission] that the user wants toggled.
     */
    fun onPermissionToggled(permissionState: WebsitePermission)

    /**
     * Indicates the user changed the status of a an autoplay permission.
     *
     * @param value current [AutoplayValue] that the user wants change.
     */
    fun onAutoplayChanged(value: AutoplayValue)
}

/**
 * MVI View that knows to display a list of specific website permissions (hardcoded):
 * - location
 * - notification
 * - microphone
 * - camera
 *
 * @param containerView [ViewGroup] in which this View will inflate itself.
 * @param interactor [WebsitePermissionInteractor] which will have delegated to all user interactions.
 */
class WebsitePermissionsView(
    override val containerView: ViewGroup,
    val interactor: WebsitePermissionInteractor
) : LayoutContainer {
    private val context = containerView.context

    val view: View = LayoutInflater.from(context)
        .inflate(R.layout.quicksettings_permissions, containerView, true)

    @VisibleForTesting
    internal var permissionViews: Map<PhoneFeature, PermissionViewHolder> = EnumMap(
        mapOf(
            CAMERA to ToggleablePermission(view.cameraLabel, view.cameraStatus),
            LOCATION to ToggleablePermission(view.locationLabel, view.locationStatus),
            MICROPHONE to ToggleablePermission(
                view.microphoneLabel,
                view.microphoneStatus
            ),
            NOTIFICATION to ToggleablePermission(
                view.notificationLabel,
                view.notificationStatus
            ),
            PERSISTENT_STORAGE to ToggleablePermission(
                view.persistentStorageLabel,
                view.persistentStorageStatus
            ),
            MEDIA_KEY_SYSTEM_ACCESS to ToggleablePermission(
                view.mediaKeySystemAccessLabel,
                view.mediaKeySystemAccessStatus
            ),
            AUTOPLAY to SpinnerPermission(
                view.autoplayLabel,
                view.autoplayStatus
            )
        )
    )

    /**
     * Allows changing what this View displays.
     *
     * @param state [WebsitePermissionsState] to be rendered.
     */
    fun update(state: WebsitePermissionsState) {
        val isVisible = permissionViews.keys
            .map { feature -> state.getValue(feature) }
            .any { it.isVisible }
        if (isVisible) {
            interactor.onPermissionsShown()
        }

        // If more permissions are added into this View we can display them into a list
        // and also use DiffUtil to only update one item in case of a permission change
        for ((feature, views) in permissionViews) {
            bindPermission(state.getValue(feature), views)
        }
    }

    /**
     * Helper method that can map a specific website permission to a dedicated permission row
     * which will display permission's [icon, label, status] and register user inputs.
     *
     * @param permissionState [WebsitePermission] specific permission that can be shown to the user.
     * @param viewHolder Views that will render [WebsitePermission]'s state.
     */
    @VisibleForTesting
    internal fun bindPermission(
        permissionState: WebsitePermission,
        viewHolder: PermissionViewHolder
    ) {
        viewHolder.label.isEnabled = permissionState.isEnabled
        viewHolder.label.isVisible = permissionState.isVisible
        viewHolder.status.isVisible = permissionState.isVisible

        when (viewHolder) {
            is ToggleablePermission -> {
                viewHolder.status.text = permissionState.status
                viewHolder.status.setOnClickListener {
                    interactor.onPermissionToggled(
                        permissionState
                    )
                }
            }
            is SpinnerPermission -> {
                if (permissionState !is WebsitePermission.Autoplay) {
                    throw IllegalArgumentException("${permissionState.phoneFeature} is not supported")
                }

                val selectedIndex = permissionState.options.indexOf(permissionState.autoplayValue)
                val adapter = ArrayAdapter(
                    context,
                    R.layout.quicksettings_permission_spinner_item,
                    permissionState.options
                )
                adapter.setDropDownViewResource(R.layout.quicksetting_permission_spinner_dropdown)
                viewHolder.status.adapter = adapter

                viewHolder.status.setSelection(selectedIndex, false)
                viewHolder.status.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val type = viewHolder.status.selectedItem as AutoplayValue
                            interactor.onAutoplayChanged(type)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
            }
        }
    }

    sealed class PermissionViewHolder(open val label: TextView, open val status: View) {
        data class ToggleablePermission(
            override val label: TextView,
            override val status: TextView
        ) :
            PermissionViewHolder(label, status)

        data class SpinnerPermission(
            override val label: TextView,
            override val status: AppCompatSpinner
        ) :
            PermissionViewHolder(label, status)
    }
}

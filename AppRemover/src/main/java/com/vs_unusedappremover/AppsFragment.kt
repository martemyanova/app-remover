package com.vs_unusedappremover

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.AsyncTask.Status
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import com.vs_unusedappremover.actions.OnSortSelectedListener

import com.vs_unusedappremover.actions.SortActionProvider
import com.vs_unusedappremover.common.GA
import com.vs_unusedappremover.data.ApplicationCollection
import com.vs_unusedappremover.data.Applications
import com.vs_unusedappremover.data.Applications.Filter
import com.vs_unusedappremover.data.OrderBy
import net.londatiga.android.ActionItem
import net.londatiga.android.QuickAction

import android.content.Intent.*
import android.provider.Settings


class AppsFragment : ListFragment() {

    private enum class Actions {
        REMOVE,
        LAUNCH,
        SEE_IN_PLAY_STORE,
        DONT_NOTIFY,
        APP_INFO
    }

    lateinit private var adapter: ApplicationsAdapter
    lateinit private var applicationCollection: ApplicationCollection
    private var order = OrderBy.TIME_UNUSED
    private var filter: Applications.Filter = Applications.Filter.DOWNLOADED
    private var isCustomView: Boolean = false
    private var updateTask: UpdateDataTask? = null

    private fun readParameters(args: Bundle) {
        if (args.containsKey(ARG_FILTER)) {
            val filterIndex = args.getInt(ARG_FILTER)
            filter = Applications.Filter.values()[filterIndex]
        }

        if (args.containsKey(ARG_ORDER)) {
            val orderIndex = args.getInt(ARG_ORDER)
            order = OrderBy.values()[orderIndex]
        }

        isCustomView = filter === Filter.UNUSED
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        applicationCollection = MyApplication.instance.applications
        readParameters(savedState ?: arguments)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            if (isCustomView) {
                inflater.inflate(R.layout.list_unused, container, false)
            } else {
                super.onCreateView(inflater, container, savedInstanceState)
            }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = ApplicationsAdapter(activity)
        listView.apply {
            divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
            adapter = this@AppsFragment.adapter
            onItemClickListener = onItemClick
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_FILTER, filter.ordinal)
        outState.putInt(ARG_ORDER, order.ordinal)
    }

    override fun onResume() {
        super.onResume()
        applicationCollection.addObserver(dataObserver)
        dataObserver.onChanged()
    }

    override fun onPause() {
        applicationCollection.removeObserver(dataObserver)

        super.onPause()
    }

    override fun onDestroy() {
        stopDataUpdateIfNeeded()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.activity_main, menu)

        val sort = menu.findItem(R.id.menu_sort)
        if (sort != null) {
            val p = MenuItemCompat.getActionProvider(sort) as SortActionProvider
            p.order = order
            p.setOnSortSelectedListener(onSortChanged)
        }

        val share = menu.findItem(R.id.menu_share)
        if (share != null) {
            val appName = getString(R.string.app_name)
            val url = "https://play.google.com/store/apps/details?id=${activity.packageName}"

            val p = MenuItemCompat.getActionProvider(share) as ShareActionProvider
            p.setShareIntent(Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(EXTRA_SUBJECT, appName)
                putExtra(EXTRA_TEXT, url)
            })
        }
    }

    private fun onListItemClick(item: AppEntry, view: View) {
        val quickAction = QuickAction(activity, QuickAction.VERTICAL)

        val pm = activity.packageManager

        quickAction.addActionItem(ActionItem(
                Actions.REMOVE.ordinal,
                getString(R.string.action_remove),
                scaleToActionSize(ContextCompat.getDrawable(activity, R.drawable.ic_delete_white_48dp))))

        quickAction.addActionItem(ActionItem(
                Actions.LAUNCH.ordinal,
                getString(R.string.action_launch),
                item.buildUrl()))

        val openInPlayStoreIntent = getOpenInPlayStoreIntent(pm, item.info.packageName)
        val infos = pm.queryIntentActivities(openInPlayStoreIntent, 0)

        if (infos.size > 0) {
            quickAction.addActionItem(ActionItem(
                    Actions.SEE_IN_PLAY_STORE.ordinal,
                    getString(R.string.action_open_in_play_store),
                    scaleToActionSize(infos[0].loadIcon(pm))))
        }

        quickAction.addActionItem(ActionItem(
                Actions.DONT_NOTIFY.ordinal,
                getString(R.string.action_dont_notify),
                ContextCompat.getDrawable(activity, R.drawable.ic_do_not_disturb_white_48dp)))

        quickAction.addActionItem(ActionItem(
                Actions.APP_INFO.ordinal,
                getString(R.string.action_open_app_info),
                ContextCompat.getDrawable(activity, R.drawable.ic_info_outline_white_48dp)))

        quickAction.setOnActionItemClickListener { _, _, actionId ->
            val action = Actions.values()[actionId]
            val packageName = item.info.packageName

            when (action) {
                AppsFragment.Actions.REMOVE -> {
                    uninstallPackage(packageName)
                }

                AppsFragment.Actions.LAUNCH -> {
                    launchApp(pm, packageName)
                }

                AppsFragment.Actions.SEE_IN_PLAY_STORE -> {
                    showInPlayStore(openInPlayStoreIntent)
                }

                AppsFragment.Actions.DONT_NOTIFY -> {
                    changeNotify(item, packageName)
                }

                AppsFragment.Actions.APP_INFO -> {
                    showInstalledAppDetails(context, packageName)
                }

                else -> {
                    Log.e(TAG, "TODO: Unknown action $action")
                }
            }
        }

        quickAction.show(ApplicationViewHolder.getFromView(view).appIcon)
        GA.event("Apps", "Show application popup")
    }

    private fun uninstallPackage(packageName: String) {
        val packageURI = Uri.parse("package:$packageName")
        startActivity(Intent(Intent.ACTION_DELETE, packageURI))
        GA.event("Apps", "Uninstall")
    }

    private fun launchApp(pm: PackageManager, packageName: String) {
        applicationCollection.notifyUsed(packageName, System.currentTimeMillis(), AppEntry.RanIn.FOREGROUND)
        val i = pm.getLaunchIntentForPackage(packageName)
        try {
            startActivity(i)
            GA.event("Apps", "Launch application")
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInPlayStore(openInPlayStoreIntent: Intent) {
        try {
            startActivity(Intent.createChooser(openInPlayStoreIntent, getString(R.string.action_launch)))
            GA.event("Apps", "Open in Play Market")
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeNotify(item: AppEntry, packageName: String) {
        val willNotify = !item.notifyAbout
        applicationCollection.setNotifyAbout(packageName, willNotify)
        GA.event("Apps", "Change notify/not notify")
        if (!willNotify) {
            Toast.makeText(activity, R.string.toast_dont_notify, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInstalledAppDetails(context: Context, packageName: String) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", packageName, null)
        context.startActivity(intent)
    }

    private fun getOpenInPlayStoreIntent(pm: PackageManager, packageName: String): Intent {

        var playUri = Uri.parse("market://details?id=$packageName")
        val intent = Intent(Intent.ACTION_VIEW, playUri)

        val infos = pm.queryIntentActivities(intent, 0)
        if (infos.size > 0) return intent

        playUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        return Intent(Intent.ACTION_VIEW, playUri)
    }

    private fun scaleToActionSize(d: Drawable): Drawable {
        val metrics = resources.displayMetrics
        val size = (QuickAction.ICON_SIZE_DP * metrics.density).toInt()
        val bmp = Bitmap.createScaledBitmap(drawableToBitmap(d), size, size, true)
        return BitmapDrawable(resources, bmp)
    }

    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable) {
            return d.bitmap
        }
        val b = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return b
    }

    private fun stopDataUpdateIfNeeded() {
        if (updateTask != null && updateTask?.status == Status.RUNNING) {
            updateTask?.cancel(true)
        }
    }

    private val onItemClick = OnItemClickListener { _, view, position, _ ->
        val index = position - listView.headerViewsCount
        val app = adapter.getItem(index)
        onListItemClick(app, view)
    }

    private val onSortChanged: OnSortSelectedListener = { _, order ->
        this@AppsFragment.order = order
        dataObserver.onChanged()
        GA.event("Apps", "Order by", order.toString())
    }

    private val dataObserver = object : DataSetObserver() {
        override fun onChanged() {
            val view = view
            view?.post(startUpdateData)
        }
    }

    private val startUpdateData = Runnable {
        Log.i(TAG, "Data changed")
        stopDataUpdateIfNeeded()
        updateTask = UpdateDataTask()
        updateTask?.execute()
    }

    private inner class UpdateDataTask : AsyncTask<Void, Int, Collection<AppEntry>>() {

        override fun doInBackground(vararg params: Void): Collection<AppEntry> {
            return applicationCollection.values(order.comparator, filter.create)
        }

        override fun onPostExecute(result: Collection<AppEntry>) {
            if (activity == null) return
            adapter.setApplications(result)
            if (!isCustomView) {
                setListShown(true)
            }
        }
    }

    companion object {

        private val TAG = AppsFragment::class.java.simpleName

        private val ARG_FILTER = "AppsFragment.filter"
        private val ARG_ORDER = "AppsFragment.sort"

        fun create(show: Applications.Filter, order: OrderBy): AppsFragment =
                AppsFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_FILTER, show.ordinal)
                        putInt(ARG_ORDER, order.ordinal)
                    }
                }

    }
}

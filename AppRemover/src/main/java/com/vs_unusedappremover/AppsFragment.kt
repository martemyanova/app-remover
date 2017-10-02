package com.vs_unusedappremover

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

class AppsFragment : ListFragment() {

    private enum class Actions {
        REMOVE,
        LAUNCH,
        SEE_IN_PLAY_STORE,
        DONT_NOTIFY
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
        val state = savedState ?: arguments
        readParameters(state)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return if (isCustomView) {
            inflater.inflate(R.layout.list_unused, container, false)
        } else super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ApplicationsAdapter(activity)
        val l = listView
        l.divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        l.adapter = adapter
        l.onItemClickListener = onItemClick
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        putParameters(outState, filter, order)
    }

    override fun onResume() {
        super.onResume()
        applicationCollection?.addObserver(dataObserver)
        dataObserver.onChanged()
    }

    override fun onPause() {
        applicationCollection?.removeObserver(dataObserver)

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
            val activity = activity
            val p = MenuItemCompat.getActionProvider(share) as ShareActionProvider

            val res = activity.resources
            val appName = res.getString(R.string.app_name)

            val url = "https://play.google.com/store/apps/details?id=" + activity.packageName

            val shareIntent = Intent(android.content.Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, appName)
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url)
            p.setShareIntent(shareIntent)
        }
    }

    private fun uninstallPackage(packageName: String) {
        val packageURI = Uri.parse("package:" + packageName)
        startActivity(Intent(Intent.ACTION_DELETE, packageURI))
    }

    private fun onListItemClick(item: AppEntry, view: View) {
        val activity = activity
        val quickAction = QuickAction(activity, QuickAction.HORIZONTAL)

        val pm = activity.packageManager

        quickAction.addActionItem(ActionItem(
                Actions.REMOVE.ordinal,
                getString(R.string.action_remove),
                scaleToActionSize(ContextCompat.getDrawable(activity, R.drawable.ic_delete_white_48dp))))

        quickAction.addActionItem(ActionItem(
                Actions.LAUNCH.ordinal,
                getString(R.string.action_launch),
                AppIcon.buildUrl(item.info.packageName)))

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

        quickAction.setOnActionItemClickListener { _, _, actionId ->
            val action = Actions.values()[actionId]
            val packageName = item.info.packageName

            when (action) {
                AppsFragment.Actions.REMOVE -> {
                    uninstallPackage(packageName)
                    GA.event("Apps", "Uninstall")
                }

                AppsFragment.Actions.LAUNCH -> {
                    applicationCollection?.notifyUsed(packageName, System.currentTimeMillis(), AppEntry.RanIn.FOREGROUND)
                    val i = pm.getLaunchIntentForPackage(packageName)
                    try {
                        startActivity(i)
                        GA.event("Apps", "Launch application")
                    } catch (e: Exception) {
                        Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show()
                    }

                }

                AppsFragment.Actions.SEE_IN_PLAY_STORE -> try {
                    startActivity(Intent.createChooser(openInPlayStoreIntent, getString(R.string.action_launch)))
                    GA.event("Apps", "Open in Play Market")
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show()
                }

                AppsFragment.Actions.DONT_NOTIFY -> {
                    val willNotify = !item.notifyAbout
                    applicationCollection?.setNotifyAbout(packageName, willNotify)
                    GA.event("Apps", "Change notify/not notify")
                    if (!willNotify) {
                        Toast.makeText(activity, R.string.toast_dont_notify, Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    Log.e(TAG, "TODO: Unknown action " + action)
                }
            }
        }

        quickAction.show(ApplicationViewHolder.getFromView(view).appIcon)
        GA.event("Apps", "Show application popup")
    }

    private fun getOpenInPlayStoreIntent(pm: PackageManager, packageName: String): Intent {

        var playUri = Uri.parse("market://details?id=" + packageName)
        val intent = Intent(Intent.ACTION_VIEW, playUri)

        val infos = pm.queryIntentActivities(intent, 0)
        if (infos.size > 0) return intent

        playUri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
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

    private val onSortChanged: OnSortSelectedListener = {_, order ->
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
            return applicationCollection.values(order, filter.create)
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

        fun create(show: Applications.Filter, order: OrderBy): AppsFragment {
            val args = Bundle()
            putParameters(args, show, order)
            val fragment = AppsFragment()
            fragment.arguments = args
            return fragment
        }

        private fun putParameters(toBundle: Bundle, show: Applications.Filter, order: OrderBy) {
            toBundle.putInt(ARG_FILTER, show.ordinal)
            toBundle.putInt(ARG_ORDER, order.ordinal)
        }
    }
}

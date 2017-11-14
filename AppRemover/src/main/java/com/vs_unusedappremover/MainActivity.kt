package com.vs_unusedappremover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity

import com.vs_unusedappremover.common.GA
import com.vs_unusedappremover.data.Applications
import com.vs_unusedappremover.data.OrderBy

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private enum class Pages(val title: Int, val create: () -> Fragment) {
        DOWNLOADED(
                title = R.string.title_downloaded_applications,
                create = { AppsFragment.create(Applications.Filter.DOWNLOADED, OrderBy.TIME_UNUSED) }),

        UNUSED(
                title = R.string.title_unused_applications,
                create = { AppsFragment.create(Applications.Filter.UNUSED, OrderBy.SIZE) });
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pager.adapter = SectionsPagerAdapter(supportFragmentManager)
        pager.currentItem = intent.getIntExtra(EXTRA_PAGE, Pages.DOWNLOADED.ordinal)

        if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            GA.event("MainActivity", "Start from notification")
        }
    }

    override fun onStart() {
        super.onStart()
        GA.onActivityStart(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val isRestored = savedInstanceState != null
        if (!isRestored) {
            val permissionRequestShown = RequestPermissionDialog.showIfNeeded(this)
            if (!permissionRequestShown) {
                RateThisAppDialog.showIfNeeded(this)
            }
        }
    }

    override fun onStop() {
        GA.onActivityStop(this)
        super.onStop()
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getPageTitle(position: Int): CharSequence =
                resources.getString(Pages.values()[position].title)

        override fun getItem(position: Int): Fragment = Pages.values()[position].create()

        override fun getCount(): Int = Pages.values().size
    }

    companion object {

        private val EXTRA_PAGE = "page"
        private val EXTRA_FROM_NOTIFICATION = "from notification"

        fun showUnusedFromNotificationIntent(context: Context): Intent =
                Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_PAGE, Pages.UNUSED.ordinal)
                    putExtra(EXTRA_FROM_NOTIFICATION, true)
                }
    }
}

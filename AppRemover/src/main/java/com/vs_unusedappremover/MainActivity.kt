package com.vs_unusedappremover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarActivity

import com.vs_unusedappremover.common.GA
import com.vs_unusedappremover.data.Applications
import com.vs_unusedappremover.data.OrderBy

class MainActivity : ActionBarActivity() {

    private enum class Pages private constructor(val titleResId: Int) {
        DOWNLOADED(R.string.title_downloaded_applications) {
            override fun createFragment(): Fragment {
                return AppsFragment.create(Applications.Filter.DOWNLOADED, OrderBy.TIME_UNUSED)
            }
        },

        UNUSED(R.string.title_unused_applications) {
            override fun createFragment(): Fragment {
                return AppsFragment.create(Applications.Filter.UNUSED, OrderBy.SIZE)
            }
        };

        abstract fun createFragment(): Fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        val viewPager = findViewById(R.id.pager) as ViewPager
        viewPager.adapter = sectionsPagerAdapter
        val item = intent.getIntExtra(EXTRA_PAGE, Pages.DOWNLOADED.ordinal)
        viewPager.currentItem = item

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

        override fun getPageTitle(position: Int): CharSequence {
            return resources.getString(Pages.values()[position].titleResId)
        }

        override fun getItem(position: Int): Fragment {
            return Pages.values()[position].createFragment()
        }

        override fun getCount(): Int {
            return Pages.values().size
        }
    }

    companion object {

        private val EXTRA_PAGE = "page"
        private val EXTRA_FROM_NOTIFICATION = "from notification"

        fun showUnusedFromNotificationIntent(context: Context): Intent {
            val i = Intent(context, MainActivity::class.java)
            i.putExtra(EXTRA_PAGE, Pages.UNUSED.ordinal)
            i.putExtra(EXTRA_FROM_NOTIFICATION, true)
            return i
        }
    }
}

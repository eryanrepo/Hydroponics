package com.eryan.calenderviewsample

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.eryan.calendarview.CalendarView
import com.eryan.calendarview.model.CalendarDay
import com.eryan.calendarview.model.CalendarMonth
import com.eryan.calendarview.model.DayOwner
import com.eryan.calendarview.ui.DayBinder
import com.eryan.calendarview.ui.ViewContainer
import com.eryan.calendarviewsample.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * These are UI behaviour tests.
 * The core functionality tests are in the library project.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CalenderViewTests {

    @get:Rule
    val homeScreenRule = ActivityTestRule(HomeActivity::class.java, true, false)

    private val currentMonth = YearMonth.now()

    @Before
    fun setup() {
        homeScreenRule.launchActivity(null)
    }

    @After
    fun teardown() {
    }

    @Test
    fun dayBinderIsCalledOnDayChanged() {
        onView(withId(R.id.examplesRv)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        class DayViewContainer(view: View) : ViewContainer(view)

        val calendarView = findFragment<CalendarFragment>().findViewById<CalendarView>(R.id.exOneCalendar)

        var boundDay: CalendarDay? = null

        val changedDate = currentMonth.atDay(4)

        homeScreenRule.runOnUiThread {
            calendarView.dayBinder = object : DayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, day: CalendarDay) {
                    boundDay = day
                }
            }
        }

        // Allow the calendar to be rebuilt due to dayBinder change.
        sleep(2000)

        homeScreenRule.runOnUiThread {
            calendarView.notifyDateChanged(changedDate)
        }

        // Allow time for date change event to be propagated.
        sleep(2000)

        assertTrue(boundDay?.date == changedDate)
        assertTrue(boundDay?.owner == DayOwner.THIS_MONTH)
    }

    @Test
    fun monthScrollListenerIsCalledWhenScrolled() {
        onView(withId(R.id.examplesRv)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        val calendarView = findFragment<CalendarFragment>().findViewById<CalendarView>(R.id.exOneCalendar)

        var targetCalMonth: CalendarMonth? = null
        calendarView.monthScrollListener = { month ->
            targetCalMonth = month
        }

        val twoMonthsAhead = currentMonth.plusMonths(2)
        homeScreenRule.runOnUiThread {
            calendarView.smoothScrollToMonth(twoMonthsAhead)
        }
        sleep(3000) // Enough time for smooth scrolling animation.
        assertEquals(targetCalMonth?.yearMonth, twoMonthsAhead)

        val fourMonthsAhead = currentMonth.plusMonths(4)
        homeScreenRule.runOnUiThread {
            calendarView.scrollToMonth(fourMonthsAhead)
        }
        sleep(3000)
        assertEquals(targetCalMonth?.yearMonth, fourMonthsAhead)

        val sixMonthsAhead = currentMonth.plusMonths(6)
        homeScreenRule.runOnUiThread {
            calendarView.smoothScrollToDate(sixMonthsAhead.atDay(1))
        }
        sleep(3000)
        assertEquals(targetCalMonth?.yearMonth, sixMonthsAhead)

        val eightMonthsAhead = currentMonth.plusMonths(8)
        homeScreenRule.runOnUiThread {
            calendarView.scrollToDate(eightMonthsAhead.atDay(1))
        }
        sleep(3000)
        assertEquals(targetCalMonth?.yearMonth, eightMonthsAhead)
    }

    @Test
    fun multipleSetupCallsRetainPositionIfCalendarHasBoundaries() {
        onView(withId(R.id.examplesRv)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        val calendarView = findFragment<CalendarFragment>().findViewById<CalendarView>(R.id.exOneCalendar)

        val targetVisibleMonth = currentMonth.plusMonths(2)

        var targetVisibleCalMonth: CalendarMonth? = null
        calendarView.monthScrollListener = { month ->
            targetVisibleCalMonth = month
        }

        homeScreenRule.runOnUiThread {
            calendarView.smoothScrollToMonth(targetVisibleMonth)
        }

        sleep(5000) // Enough time for smooth scrolling animation.

        homeScreenRule.runOnUiThread {
            calendarView.setup(
                targetVisibleMonth.minusMonths(10),
                targetVisibleMonth.plusMonths(10),
                daysOfWeekFromLocale().first()
            )
        }

        sleep(5000) // Enough time for setup to finish.

        assertTrue(calendarView.findFirstVisibleMonth() == targetVisibleCalMonth)
    }

    @Test
    fun completionBlocksAreCalledOnTheMainThread() {
        val calendarView = CalendarView(homeScreenRule.activity)
        homeScreenRule.runOnUiThread {
            val threadName = Thread.currentThread().name
            calendarView.setupAsync(YearMonth.now(), YearMonth.now().plusMonths(10), DayOfWeek.SUNDAY) {
                assertTrue(threadName == Thread.currentThread().name)
                calendarView.updateMonthConfigurationAsync {
                    assertTrue(threadName == Thread.currentThread().name)
                    calendarView.updateMonthRangeAsync {
                        assertTrue(threadName == Thread.currentThread().name)
                    }
                }
            }
        }
        sleep(3000)
    }

    private inline fun <reified T : Fragment> findFragment(): T {
        return homeScreenRule.activity.supportFragmentManager
            .findFragmentByTag(T::class.java.simpleName) as T
    }

    private fun <T : View> Fragment.findViewById(@IdRes id: Int): T = requireView().findViewById(id)
}
